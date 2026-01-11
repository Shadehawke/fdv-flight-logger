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

    fun saveFlight(draft: FlightDraft) {
        viewModelScope.launch {
            flightRepo.saveDraft(draft)
            // Update “Last landed” immediately for identity strip
            if (draft.arr.isNotBlank()) repo.setLastLanded(draft.arr)
        }
    }

    fun observeFlights() = flightRepo.observeAll()


    fun saveSetup(profile: PilotProfile, settings: AppSettings) {
        viewModelScope.launch {
            repo.savePilotProfile(profile)
            repo.saveSettings(settings)
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

    suspend fun getFlightById(id: Long): FlightLogEntity? {
        return flightRepo.getById(id)
    }
}
