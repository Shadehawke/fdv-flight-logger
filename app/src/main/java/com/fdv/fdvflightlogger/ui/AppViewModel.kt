package com.fdv.fdvflightlogger.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.fdv.fdvflightlogger.data.airports.AirportRepository
import com.fdv.fdvflightlogger.data.auth.AuthCallback
import com.fdv.fdvflightlogger.data.auth.FdvAuth
import com.fdv.fdvflightlogger.data.auth.FdvAuthClient
import com.fdv.fdvflightlogger.data.auth.FdvAuthException
import com.fdv.fdvflightlogger.data.auth.FdvSession
import com.fdv.fdvflightlogger.data.auth.FdvSessionStore
import com.fdv.fdvflightlogger.data.auth.Pkce
import com.fdv.fdvflightlogger.data.auth.PkceSession
import com.fdv.fdvflightlogger.data.backup.FlightBackup
import com.fdv.fdvflightlogger.data.db.FlightLogEntity
import com.fdv.fdvflightlogger.data.db.FlightLogRepository
import com.fdv.fdvflightlogger.data.fdv.FdvApi
import com.fdv.fdvflightlogger.data.fdv.FdvApiException
import com.fdv.fdvflightlogger.data.fdv.FdvBid
import com.fdv.fdvflightlogger.data.fdv.FdvBidPlan
import com.fdv.fdvflightlogger.data.fdv.FdvPilot
import com.fdv.fdvflightlogger.data.fdv.FdvPirep
import com.fdv.fdvflightlogger.data.history.LogbookHistory
import com.fdv.fdvflightlogger.data.prefs.AppSettings
import com.fdv.fdvflightlogger.data.prefs.FdvImportMode
import com.fdv.fdvflightlogger.data.prefs.FdvSyncState
import com.fdv.fdvflightlogger.data.prefs.PilotProfile
import com.fdv.fdvflightlogger.data.prefs.UserPrefsRepository
import com.fdv.fdvflightlogger.data.profile.AvatarStore
import com.fdv.fdvflightlogger.data.simbrief.SimBriefClient
import com.fdv.fdvflightlogger.data.simbrief.SimBriefException
import com.fdv.fdvflightlogger.data.simbrief.SimBriefParser
import com.fdv.fdvflightlogger.data.simbrief.SimBriefPlan
import com.fdv.fdvflightlogger.data.stats.FlightStats
import com.fdv.fdvflightlogger.data.stats.StatsCalculator
import com.fdv.fdvflightlogger.data.sync.AirTimeFillWorker
import com.fdv.fdvflightlogger.data.sync.HistoryImport
import com.fdv.fdvflightlogger.data.sync.ImportPlan
import com.fdv.fdvflightlogger.data.sync.PirepMatcher
import com.fdv.fdvflightlogger.data.sync.toImportedFlight
import com.fdv.fdvflightlogger.data.sync.withActuals
import com.fdv.fdvflightlogger.data.update.AppRelease
import com.fdv.fdvflightlogger.data.update.AppVersion
import com.fdv.fdvflightlogger.data.update.ReleaseChecker
import com.fdv.fdvflightlogger.data.update.UpdateDownloader
import com.fdv.fdvflightlogger.data.update.UpdateException
import com.fdv.fdvflightlogger.data.update.UpdateInstaller
import com.fdv.fdvflightlogger.data.update.UpdateStatus
import com.fdv.fdvflightlogger.export.ExportCsv
import com.fdv.fdvflightlogger.export.ExportPdf
import com.fdv.fdvflightlogger.ui.screens.FlightDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import com.fdv.fdvflightlogger.data.fdv.FdvAward

data class AppState(
    val profile: PilotProfile = PilotProfile(),
    val settings: AppSettings = AppSettings(),
    val lastLanded: String = "",
    val isSetupComplete: Boolean = false
)

// Searching for a match stops after this many logbook pages (50 each)
private const val MAX_PIREP_SEARCH_PAGES = 10

// 5,000 flights; only a safety stop in case FDV's paging ever misbehaves
private const val MAX_HISTORY_PAGES = 100

// GitHub allows 60 unauthenticated API calls an hour; once a day is plenty for update checks
private const val UPDATE_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

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

    // The trophy case; null until first loaded
    private val _awards = MutableStateFlow<List<FdvAward>?>(null)
    val awards: StateFlow<List<FdvAward>?> = _awards.asStateFlow()

    private val _awardsLoading = MutableStateFlow(false)
    val awardsLoading: StateFlow<Boolean> = _awardsLoading.asStateFlow()

    // The pilot's photo, or null for initials
    private val _avatar = MutableStateFlow<Bitmap?>(null)
    val avatar: StateFlow<Bitmap?> = _avatar.asStateFlow()

    private val _fdvRefreshing = MutableStateFlow(false)
    val fdvRefreshing: StateFlow<Boolean> = _fdvRefreshing.asStateFlow()

    private val _bidLoading = MutableStateFlow(false)
    private val _pirepSyncing = MutableStateFlow(false)
    val fdvSync: StateFlow<FdvSyncState> = repo.fdvSyncFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FdvSyncState())

    // Non-null while an import or sync runs; the text is shown on the account card
    private val _historyProgress = MutableStateFlow<String?>(null)
    val historyProgress: StateFlow<String?> = _historyProgress.asStateFlow()
    // Non-null while the background air-time fill is queued or running; shown on the account card
    val airTimeFill: StateFlow<String?> = WorkManager.getInstance(getApplication())
        .getWorkInfosForUniqueWorkFlow(AirTimeFillWorker.NAME)
        .map { infos -> infos.firstOrNull()?.let(::describeAirTimeFill) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val pirepSyncing: StateFlow<Boolean> = _pirepSyncing.asStateFlow()
    val bidLoading: StateFlow<Boolean> = _bidLoading.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppRelease?>(null)
    val availableUpdate: StateFlow<AppRelease?> = _availableUpdate.asStateFlow()

    val dismissedUpdate: StateFlow<String> = repo.dismissedUpdateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    private val _checkingUpdate = MutableStateFlow(false)
    val checkingUpdate: StateFlow<Boolean> = _checkingUpdate.asStateFlow()

    // Whole percent while an update downloads; null when idle
    private val _updateDownload = MutableStateFlow<Int?>(null)
    val updateDownload: StateFlow<Int?> = _updateDownload.asStateFlow()

    // True when Android needs the pilot to allow installs from this app first
    private val _needsInstallPermission = MutableStateFlow(false)
    val needsInstallPermission: StateFlow<Boolean> = _needsInstallPermission.asStateFlow()

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
        // Signed in from an earlier session: load the FDV profile once, so the dashboard has it
        viewModelScope.launch {
            if (sessionStore.session.first() != null) refreshFdvProfile(quiet = true)
        }
        // Install failures arrive from InstallStatusReceiver, which can't reach the ViewModel directly
        viewModelScope.launch {
            UpdateStatus.failures.collect { message -> _events.tryEmit(UiEvent.ExportError(message)) }
        }
        viewModelScope.launch { _avatar.value = AvatarStore.load(getApplication()) }
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
                val json = FlightBackup.toJson(flights, appVersion(context), AvatarStore.jpegBytes(context))

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

                val contents = withContext(Dispatchers.Default) { FlightBackup.fromJson(json) }
                val flights = contents.flights
                val result = flightRepo.restore(flights)

                // Like flights, a restore only fills gaps: an existing photo is never replaced
                if (_avatar.value == null) {
                    contents.avatarJpeg?.let { jpeg ->
                        AvatarStore.restore(context, jpeg)?.let { _avatar.value = it }
                    }
                }

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
            _awards.value = null
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

    fun loadAwards() {
        if (_awardsLoading.value) return
        viewModelScope.launch {
            _awardsLoading.value = true
            try {
                fdvCall { token -> FdvApi.awards(token) }?.let { _awards.value = it }
            } finally {
                _awardsLoading.value = false
            }
        }
    }

    /**
     * Fetches the pilot's FDV bookings. One booking is loaded straight away and handed to [onLoaded];
     * several are handed to [onChoose] so the screen can show a picker.
     */
    fun startFromBid(onChoose: (List<FdvBid>) -> Unit, onLoaded: (FdvBidPlan) -> Unit) {
        if (_bidLoading.value) return
        viewModelScope.launch {
            _bidLoading.value = true
            try {
                val bids = fdvCall { token -> FdvApi.bids(token) } ?: return@launch
                when (bids.size) {
                    0 -> _events.tryEmit(UiEvent.Message("You have no booked flights on FDV"))
                    1 -> fetchBid(bids.single().id, onLoaded)
                    else -> onChoose(bids)
                }
            } finally {
                _bidLoading.value = false
            }
        }
    }

    /** Loads one booking the pilot picked from the list. */
    fun loadBid(id: Long, onLoaded: (FdvBidPlan) -> Unit) {
        if (_bidLoading.value) return
        viewModelScope.launch {
            _bidLoading.value = true
            try {
                fetchBid(id, onLoaded)
            } finally {
                _bidLoading.value = false
            }
        }
    }

    private suspend fun fetchBid(id: Long, onLoaded: (FdvBidPlan) -> Unit) {
        val bid = fdvCall { token -> FdvApi.bid(token, id) } ?: return
        onLoaded(bid)
        val callsign = listOfNotNull(bid.plan.airline, bid.plan.flightNumber).joinToString("")
        _events.tryEmit(UiEvent.Message("Loaded $callsign ${bid.plan.dep} → ${bid.plan.arr} from FDV"))
    }

    /** Finds this flight's PIREP and hands it to [onFound] for the pilot to confirm. Nothing is saved yet. */
    fun findPirepFor(flight: FlightLogEntity, onFound: (FdvPirep) -> Unit) {
        if (_pirepSyncing.value) return
        viewModelScope.launch {
            _pirepSyncing.value = true
            try {
                fdvCall { token -> findPirep(token, flight) }?.let(onFound)
            } finally {
                _pirepSyncing.value = false
            }
        }
    }

    /** Saves the confirmed PIREP's actuals onto the flight and hands back the updated row. */
    fun applyPirep(flight: FlightLogEntity, pirep: FdvPirep, onSaved: (FlightLogEntity) -> Unit) {
        viewModelScope.launch {
            try {
                val updated = flight.withActuals(pirep, state.value.settings.fuelUnit)
                flightRepo.upsert(updated)
                onSaved(updated)
                _events.tryEmit(UiEvent.Message("Synced with FDV PIREP ${pirep.callsign}"))
            } catch (e: Exception) {
                _events.tryEmit(UiEvent.ExportError("Sync failed: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    private suspend fun findPirep(token: String, flight: FlightLogEntity): FdvPirep {
        // Already linked (a re-sync): fetch that exact PIREP, no searching
        flight.fdvPirepId?.let { return FdvApi.pirep(token, it) }

        val taken = flightRepo.getAll().mapNotNullTo(HashSet()) { it.fdvPirepId }
        val oldestUseful = flight.createdAtEpochMs - PirepMatcher.WINDOW_MS

        for (pageNumber in 1..MAX_PIREP_SEARCH_PAGES) {
            val page = FdvApi.pireps(token, pageNumber)
            PirepMatcher.match(flight, page.pireps, taken)?.let { found ->
                // The list has no Black Box log; the detail call adds air time
                return FdvApi.pirep(token, found.id)
            }
            // Pages are newest first, so once they're older than the window, nothing further can match
            val oldestOnPage = page.pireps.mapNotNull { it.submittedAtMillis }.minOrNull()
            if (!page.hasMore || (oldestOnPage != null && oldestOnPage < oldestUseful)) break
        }
        throw FdvApiException("No filed PIREP on FDV matches this flight. Has it been filed and processed yet?")
    }

    /** Reads the whole FDV logbook and works out what an import would do. Nothing is written yet. */
    fun previewHistoryImport(onReady: (ImportPlan) -> Unit) {
        if (_historyProgress.value != null) return
        viewModelScope.launch {
            _historyProgress.value = "Reading your FDV logbook…"
            try {
                val pireps = fdvCall { token -> fetchAllPireps(token, afterId = null) } ?: return@launch
                val plan = withContext(Dispatchers.Default) { HistoryImport.plan(flightRepo.getAll(), pireps) }
                onReady(plan)
            } finally {
                _historyProgress.value = null
            }
        }
    }

    /** Writes a confirmed import and remembers the mode for ongoing sync. */
    fun applyHistoryImport(plan: ImportPlan, mode: FdvImportMode) {
        if (_historyProgress.value != null) return
        viewModelScope.launch {
            _historyProgress.value = "Updating your logbook…"
            try {
                val (updated, added) = writeImport(plan, mode)
                val message = when {
                    added > 0 -> "Added $added flights from FDV and filled in $updated of yours"
                    else -> "Filled in $updated of your flights from FDV"
                }
                _events.tryEmit(UiEvent.Message(message))
            } catch (e: Exception) {
                _events.tryEmit(UiEvent.ExportError("Import failed: ${e.message ?: "Unknown error"}"))
            } finally {
                _historyProgress.value = null
            }
        }
    }

    /** Fetches only PIREPs filed since the last import or sync, and applies them in the saved mode. */
    fun syncNewPireps() {
        if (_historyProgress.value != null) return
        viewModelScope.launch {
            val sync = fdvSync.value
            val mode = sync.mode ?: run {
                _events.tryEmit(UiEvent.Message("Import your FDV history first"))
                return@launch
            }
            _historyProgress.value = "Checking FDV for new flights…"
            try {
                val pireps = fdvCall { token -> fetchAllPireps(token, afterId = sync.lastPirepId) } ?: return@launch
                if (pireps.isEmpty()) {
                    _events.tryEmit(UiEvent.Message("No new flights on FDV"))
                    return@launch
                }
                val plan = withContext(Dispatchers.Default) { HistoryImport.plan(flightRepo.getAll(), pireps) }
                val (updated, added) = writeImport(plan, mode)
                _events.tryEmit(UiEvent.Message("Synced from FDV: $added added, $updated filled in"))
            } catch (e: Exception) {
                _events.tryEmit(UiEvent.ExportError("Sync failed: ${e.message ?: "Unknown error"}"))
            } finally {
                _historyProgress.value = null
            }
        }
    }

    /** Deletes the flights the import created and resets sync, so history could be imported again. */
    fun removeImportedFlights() {
        viewModelScope.launch {
            try {
                val removed = flightRepo.deleteImported()
                repo.saveFdvSync(FdvSyncState())
                _events.tryEmit(UiEvent.Message("Removed $removed imported flights"))
            } catch (e: Exception) {
                _events.tryEmit(UiEvent.ExportError("Couldn't remove imported flights: ${e.message ?: "Unknown error"}"))
            }
        }
    }

    fun startAirTimeFill() = AirTimeFillWorker.enqueue(getApplication())

    fun cancelAirTimeFill() = AirTimeFillWorker.cancel(getApplication())

    /**
     * Asks GitHub for the latest release. Automatic checks run at most daily and never in debug builds
     * (an update signed with the release key can't install over a debug build); [manual] always checks.
     */
    fun checkForUpdate(manual: Boolean) {
        if (_checkingUpdate.value) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            if (!manual) {
                val debuggable = (app.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (debuggable) return@launch
                if (System.currentTimeMillis() - repo.lastUpdateCheck() < UPDATE_CHECK_INTERVAL_MS) return@launch
            }

            _checkingUpdate.value = true
            try {
                val release = ReleaseChecker.latest()
                repo.setLastUpdateCheck(System.currentTimeMillis())

                val current = appVersion(app)
                val newer = release?.takeIf { AppVersion.isNewer(it.version, current) }
                _availableUpdate.value = newer

                if (manual) {
                    val message = newer?.let { "Version ${it.version} is available" }
                        ?: "You're on the latest version ($current)"
                    _events.tryEmit(UiEvent.Message(message))
                }
            } catch (e: UpdateException) {
                // Automatic checks fail quietly; only a check the pilot asked for reports a problem
                if (manual) _events.tryEmit(UiEvent.ExportError(e.message ?: "Update check failed"))
            } finally {
                _checkingUpdate.value = false
            }
        }
    }

    fun dismissUpdate(version: String) {
        viewModelScope.launch { repo.dismissUpdate(version) }
    }

    /** Downloads, verifies and installs [release]. Android then asks the pilot to confirm. */
    fun installUpdate(release: AppRelease) {
        if (_updateDownload.value != null) return
        val app = getApplication<Application>()

        // Android's one-time "allow installs from this app" switch
        if (!app.packageManager.canRequestPackageInstalls()) {
            _needsInstallPermission.value = true
            return
        }

        viewModelScope.launch {
            _updateDownload.value = 0
            try {
                val apk = UpdateDownloader.download(app, release) { percent -> _updateDownload.value = percent }
                withContext(Dispatchers.IO) { UpdateInstaller.install(app, apk) }
            } catch (e: UpdateException) {
                _events.tryEmit(UiEvent.ExportError(e.message ?: "Update failed"))
            } finally {
                _updateDownload.value = null
            }
        }
    }

    fun dismissInstallPermission() {
        _needsInstallPermission.value = false
    }

    fun setAvatar(uri: Uri) {
        viewModelScope.launch {
            try {
                _avatar.value = AvatarStore.saveFrom(getApplication(), uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                // Includes ImageDecoder.DecodeException: not an image, or a format the device can't read
                _events.tryEmit(UiEvent.ExportError("Couldn't use that photo. Try a different one."))
            } catch (e: SecurityException) {
                _events.tryEmit(UiEvent.ExportError("Couldn't open that photo."))
            }
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            AvatarStore.delete(getApplication())
            _avatar.value = null
        }
    }

    private fun describeAirTimeFill(info: WorkInfo): String? = when (info.state) {
        WorkInfo.State.ENQUEUED -> "Air times: waiting to continue…"
        WorkInfo.State.RUNNING -> {
            val done = info.progress.getInt(AirTimeFillWorker.KEY_DONE, 0)
            val total = info.progress.getInt(AirTimeFillWorker.KEY_TOTAL, 0)
            if (total > 0) "Filling in air times: $done of $total" else "Filling in air times…"
        }
        else -> null   // finished, failed or cancelled: nothing to show
    }

    // Returns (flights filled in, flights added)
    private suspend fun writeImport(plan: ImportPlan, mode: FdvImportMode): Pair<Int, Int> {
        val unit = state.value.settings.fuelUnit
        val now = System.currentTimeMillis()
        val updates = plan.matched.map { (flight, pirep) -> flight.withActuals(pirep, unit) }
        val inserts = if (mode == FdvImportMode.ALL) plan.unmatched.map { it.toImportedFlight(unit, now) } else emptyList()

        flightRepo.applyImport(updates, inserts)

        // The list has no Black Box logs; fetch air times in the background
        if (updates.isNotEmpty() || inserts.isNotEmpty()) AirTimeFillWorker.enqueue(getApplication())

        // Never move the sync point backwards
        val lastId = listOfNotNull(plan.highestPirepId, fdvSync.value.lastPirepId).maxOrNull()
        repo.saveFdvSync(FdvSyncState(mode = mode, lastPirepId = lastId))
        return updates.size to inserts.size
    }

    private suspend fun fetchAllPireps(token: String, afterId: Long?): List<FdvPirep> {
        val all = mutableListOf<FdvPirep>()
        for (pageNumber in 1..MAX_HISTORY_PAGES) {
            val page = withRateLimitRetry { FdvApi.pireps(token, pageNumber, afterId) }
            all += page.pireps
            _historyProgress.value = "Reading your FDV logbook… page $pageNumber of ${page.lastPage}"
            if (!page.hasMore) break
        }
        return all
    }

    // On HTTP 429, wait as long as FDV asks and try again (twice at most), then give up with its message
    private suspend fun <T> withRateLimitRetry(block: suspend () -> T): T {
        repeat(2) {
            try {
                return block()
            } catch (e: FdvApiException) {
                val wait = e.retryAfterSeconds ?: throw e
                _historyProgress.value = "FDV asked the app to slow down; waiting $wait seconds…"
                delay(wait * 1000L)
            }
        }
        return block()
    }

    /**
     * Runs an FDV API call with the stored session. Errors become snackbars, and a 401 signs out.
     * Returns null when the call couldn't be made or failed, so callers just stop.
     */
    private suspend fun <T> fdvCall(block: suspend (token: String) -> T): T? {
        val session = sessionStore.session.first()
            ?.takeUnless { it.isExpired(System.currentTimeMillis()) }
        if (session == null) {
            _events.tryEmit(UiEvent.Message("Sign in to FDV in Settings first"))
            return null
        }
        return try {
            block(session.token)
        } catch (e: FdvApiException) {
            if (e.signedOut) {
                sessionStore.clear()
                _fdvPilot.value = null
            }
            _events.tryEmit(UiEvent.ExportError(e.message ?: "FlyDeltaVirtual request failed"))
            null
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
