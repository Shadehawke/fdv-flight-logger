package com.fdv.fdvflightlogger.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightLogRepository
import com.fdv.fdvflightlogger.data.prefs.AppSettings
import com.fdv.fdvflightlogger.data.prefs.PilotProfile
import com.fdv.fdvflightlogger.data.prefs.UserPrefsRepository
import com.fdv.fdvflightlogger.export.ExportCsv
import com.fdv.fdvflightlogger.export.ExportPdf
import com.fdv.fdvflightlogger.ui.screens.FlightDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.fdv.fdvflightlogger.data.airports.AirportRepository
import android.content.pm.PackageManager
import android.os.Build
import com.fdv.fdvflightlogger.data.backup.FlightBackup
import kotlinx.coroutines.flow.first
import com.fdv.fdvflightlogger.data.simbrief.SimBriefClient
import com.fdv.fdvflightlogger.data.simbrief.SimBriefException
import com.fdv.fdvflightlogger.data.simbrief.SimBriefParser
import com.fdv.fdvflightlogger.data.simbrief.SimBriefPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.fdv.fdvflightlogger.data.stats.FlightStats
import com.fdv.fdvflightlogger.data.stats.StatsCalculator
import kotlinx.coroutines.flow.flowOn
import com.fdv.fdvflightlogger.data.history.LogbookHistory
import kotlinx.coroutines.flow.map
import android.content.ActivityNotFoundException
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.fdv.fdvflightlogger.data.auth.AuthCallback
import com.fdv.fdvflightlogger.data.auth.FdvAuth
import com.fdv.fdvflightlogger.data.auth.Pkce
import com.fdv.fdvflightlogger.data.auth.PkceSession
import com.fdv.fdvflightlogger.data.auth.FdvAuthClient
import com.fdv.fdvflightlogger.data.auth.FdvAuthException
import com.fdv.fdvflightlogger.data.auth.FdvSession
import com.fdv.fdvflightlogger.data.auth.FdvSessionStore
import com.fdv.fdvflightlogger.data.fdv.FdvApi
import com.fdv.fdvflightlogger.data.fdv.FdvApiException
import com.fdv.fdvflightlogger.data.fdv.FdvPilot

data class AppState(
    val profile: PilotProfile = PilotProfile(),
    val settings: AppSettings = AppSettings(),
    val lastLanded: String = "",
    val isSetupComplete: Boolean = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UserPrefsRepository(getApplication<Application>().applicationContext)
    private val flightRepo = FlightLogRepository(getApplication<Application>().applicationContext)

    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _simBriefLoading = MutableStateFlow(false)
    val simBriefLoading: StateFlow<Boolean> = _simBriefLoading.asStateFlow()
    // Flips to true once airports.json is parsed, so stats recompute with distances included
    private val airportsReady = MutableStateFlow(AirportRepository.isLoaded)
    // The secrets for a sign-in in progress; only needed until the browser comes back
    private var pendingSignIn: PkceSession? = null

    private val sessionStore = FdvSessionStore(getApplication<Application>().applicationContext)

    // Signed-in FDV pilot, read from encrypted storage; an expired session counts as signed out
    val fdvSession: StateFlow<FdvSession?> = sessionStore.session
        .map { stored -> stored?.takeUnless { it.isExpired(System.currentTimeMillis()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // The pilot's FDV profile from /me; fetched after sign-in and when Settings opens
    private val _fdvPilot = MutableStateFlow<FdvPilot?>(null)
    val fdvPilot: StateFlow<FdvPilot?> = _fdvPilot.asStateFlow()

    private val _fdvRefreshing = MutableStateFlow(false)
    val fdvRefreshing: StateFlow<Boolean> = _fdvRefreshing.asStateFlow()

    val state: StateFlow<AppState> =
        combine(
            repo.pilotProfileFlow,
            repo.settingsFlow,
            repo.lastLandedFlow
        ) { profile, settings, lastLanded ->
            AppState(
                profile = profile,
                settings = settings,
                lastLanded = lastLanded,
                isSetupComplete = profile.isComplete
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppState()
        )

    val stats: StateFlow<FlightStats> =
        combine(
            flightRepo.observeAll(),
            repo.pilotProfileFlow,
            airportsReady
        ) { flights, profile, _ ->
            StatsCalculator.compute(flights, profile.hub, AirportRepository::distanceNm)
        }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = FlightStats()
            )

    val logbookHistory: StateFlow<LogbookHistory> =
        flightRepo.observeAll()
            .map { flights -> LogbookHistory.from(flights) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LogbookHistory.EMPTY
            )

    init {
        viewModelScope.launch {
            AirportRepository.ensureLoaded(getApplication())
            airportsReady.value = true
        }
    }

    fun saveFlight(draft: FlightDraft) {
        viewModelScope.launch {
            try {
                val createdAtEpochMs = if (draft.id != null) {
                    flightRepo.getById(draft.id)?.createdAtEpochMs
                        ?: System.currentTimeMillis()
                } else {
                    System.currentTimeMillis()
                }

                flightRepo.saveDraft(draft = draft, createdAtEpochMs = createdAtEpochMs)

                if (draft.arr.isNotBlank()) {
                    repo.setLastLanded(draft.arr)
                }

                _events.tryEmit(UiEvent.Message("Flight saved"))  // ← Add success feedback
            } catch (e: Throwable) {
                _events.tryEmit(UiEvent.Message("Save failed: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun observeFlights() = flightRepo.observeAll()

    fun saveSetup(profile: PilotProfile, settings: AppSettings) {
        viewModelScope.launch {
            try {
                repo.savePilotProfile(profile)
                repo.saveSettings(settings)
                _events.tryEmit(UiEvent.Message("Setup complete"))
            } catch (e: Throwable) {
                _events.tryEmit(UiEvent.Message("Setup failed: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = state.value.settings
            repo.saveSettings(transform(current))
        }
    }

    fun exportAllFlightsToCsv(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val flights = flightRepo.getAll()
                val csv = ExportCsv.buildCsv(flights)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open output stream")
                }

                _events.tryEmit(UiEvent.ExportSuccess("fdv_flights.csv", "text/csv", uri))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ExportError("CSV export failed: ${t.message ?: "Unknown error"}"))
            }
        }
    }


    fun exportAllFlightsToPdf(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val flights = flightRepo.getAll()

                val header = ExportPdf.HeaderInfo(
                    pilotId = state.value.profile.pilotId,
                    pilotName = state.value.profile.name,
                    hub = state.value.profile.hub
                )

                val pdf = ExportPdf.buildPdf(flights, header)

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        pdf.writeTo(out)
                    } ?: error("Unable to open output stream")
                    pdf.close()
                }

                _events.tryEmit(UiEvent.ExportSuccess("fdv_flights.pdf", "application/pdf", uri))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ExportError("PDF export failed: ${t.message ?: "Unknown error"}"))
            }
        }
    }

    fun backupFlights(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            try {
                val flights = flightRepo.getAll()
                val json = FlightBackup.toJson(flights, appVersion(context))

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open output stream")
                }

                _events.tryEmit(UiEvent.ExportSuccess(fileName, "application/json", uri))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ExportError("Backup failed: ${t.message ?: "Unknown error"}"))
            }
        }
    }

    fun restoreFlights(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    } ?: error("Unable to open file")
                }

                val flights = withContext(Dispatchers.Default) { FlightBackup.fromJson(json) }
                val result = flightRepo.restore(flights)

                // Fresh install: seed "Last landed" from the most recent restored flight
                if (result.added > 0 && repo.lastLandedFlow.first().isBlank()) {
                    flights.maxByOrNull { it.createdAtEpochMs }?.let { repo.setLastLanded(it.arr) }
                }

                val message = when {
                    result.added == 0 -> "Nothing new to restore; all ${result.skipped} flights are already in your logbook"
                    result.skipped == 0 -> "Restored ${result.added} flights"
                    else -> "Restored ${result.added} flights (${result.skipped} already in your logbook)"
                }
                _events.tryEmit(UiEvent.Message(message))
            } catch (e: FlightBackup.InvalidBackupException) {
                _events.tryEmit(UiEvent.ExportError(e.message ?: "Invalid backup file"))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ExportError("Restore failed: ${t.message ?: "Unknown error"}"))
            }
        }
    }

    /**
     * Fetches the pilot's latest SimBrief plan and hands it to [onImported].
     * Only call this from a button tap: SimBrief bans apps that poll its endpoint.
     */
    fun importFromSimBrief(context: Context, onImported: (SimBriefPlan) -> Unit) {
        // A second tap while a request is in flight does nothing
        if (_simBriefLoading.value) return

        val alias = state.value.settings.simbriefAlias.trim()
        if (alias.isEmpty()) {
            _events.tryEmit(UiEvent.Message("Add your SimBrief alias in Settings first"))
            return
        }

        viewModelScope.launch {
            _simBriefLoading.value = true
            try {
                val json = SimBriefClient.fetchLatestOfp(alias, appVersion(context))
                // The OFP JSON is large (~800 KB); parse it off the main thread
                val plan = withContext(Dispatchers.Default) { SimBriefParser.parse(json) }
                onImported(plan)
                _events.tryEmit(UiEvent.Message("Imported ${plan.dep} → ${plan.arr} from SimBrief"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: SimBriefException) {
                _events.tryEmit(UiEvent.ExportError(e.message ?: "SimBrief import failed"))
            } catch (t: Throwable) {
                _events.tryEmit(UiEvent.ExportError("SimBrief import failed: ${t.message ?: "Unknown error"}"))
            } finally {
                _simBriefLoading.value = false
            }
        }
    }

    fun startFdvSignIn(context: Context, viaVatsim: Boolean = false) {
        val session = Pkce.newSession()
        pendingSignIn = session
        try {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(context, FdvAuth.authorizeUrl(session, viaVatsim).toUri())
        } catch (e: ActivityNotFoundException) {
            pendingSignIn = null
            _events.tryEmit(UiEvent.ExportError("No browser is installed to open FDV sign-in."))
        }
    }

    fun handleAuthRedirect(params: Map<String, String?>) {
        val session = pendingSignIn
        // Single use: a replayed or second callback can never match again
        pendingSignIn = null

        when (val result = AuthCallback.parse(params, session?.state)) {
            is AuthCallback.Code -> {
                // parse() only returns Code when a session existed, but the compiler can't know that
                val verifier = session?.verifier ?: return
                viewModelScope.launch {
                    try {
                        val signedIn = FdvAuthClient.exchangeCode(result.code, verifier)
                        sessionStore.save(signedIn)
                        _events.tryEmit(UiEvent.Message("Signed in to FDV as ${signedIn.callsign ?: "pilot"}"))
                        refreshFdvProfile(quiet = true)
                    } catch (e: FdvAuthException) {
                        _events.tryEmit(UiEvent.ExportError(e.message ?: "FDV sign-in failed"))
                    }
                }
            }
            is AuthCallback.Failure -> viewModelScope.launch {
                // On a cold start from the redirect, MainActivity's snackbar collector isn't running yet.
                // Wait until something is listening, so the message isn't silently dropped.
                _events.subscriptionCount.first { it > 0 }
                _events.emit(UiEvent.ExportError(result.message))
            }
        }
    }

    fun signOutOfFdv() {
        viewModelScope.launch {
            val session = sessionStore.session.first()
            // Clear locally first, so signing out works even with no connection
            sessionStore.clear()
            _fdvPilot.value = null
            session?.let { FdvAuthClient.logout(it.token) }
            _events.tryEmit(UiEvent.Message("Signed out of FDV"))
        }
    }

    /** Loads /me and copies callsign, name and hub into the pilot profile. */
    fun refreshFdvProfile(quiet: Boolean = false) {
        if (_fdvRefreshing.value) return
        viewModelScope.launch {
            // Set before the first suspension so a second tap can't start a parallel request
            _fdvRefreshing.value = true
            try {
                val session = sessionStore.session.first()
                    ?.takeUnless { it.isExpired(System.currentTimeMillis()) }
                    ?: return@launch

                val pilot = FdvApi.me(session.token)
                _fdvPilot.value = pilot

                // FDV is the source of truth while signed in; keep local values where FDV sent none
                val current = repo.pilotProfileFlow.first()
                repo.savePilotProfile(
                    current.copy(
                        pilotId = pilot.callsign,
                        name = pilot.name.ifBlank { current.name },
                        hub = pilot.hubIcao ?: current.hub
                    )
                )
                if (!quiet) _events.tryEmit(UiEvent.Message("FDV profile updated"))
            } catch (e: FdvApiException) {
                if (e.signedOut) {
                    sessionStore.clear()
                    _fdvPilot.value = null
                }
                _events.tryEmit(UiEvent.ExportError(e.message ?: "Couldn't load your FDV profile"))
            } finally {
                _fdvRefreshing.value = false
            }
        }
    }

    private fun appVersion(context: Context): String =
        runCatching {
            val pm = context.packageManager
            // The Int-flags overload is deprecated from API 33; minSdk is 31, so branch
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull() ?: "unknown"

    suspend fun getFlightById(id: Long): FlightLogEntity? {
        return flightRepo.getById(id)
    }

    suspend fun getLatestFlight(): FlightLogEntity? {
        return flightRepo.getLatestFlight()
    }

    fun deleteFlight(flight: FlightLogEntity) {
        viewModelScope.launch {
            try {
                flightRepo.delete(flight)
                _events.tryEmit(UiEvent.Message("Flight deleted"))
            } catch (e: Throwable) {  // ← Capture exception
                _events.tryEmit(UiEvent.Message("Delete failed: ${e.message ?: "Unknown error"}"))
            }
        }
    }
}
