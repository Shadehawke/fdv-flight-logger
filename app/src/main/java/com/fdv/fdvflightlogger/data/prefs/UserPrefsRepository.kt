package com.fdv.fdvflightlogger.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fdv_prefs")

class UserPrefsRepository(private val context: Context) {

    private object Keys {
        val PILOT_ID = stringPreferencesKey("pilot_id")
        val PILOT_NAME = stringPreferencesKey("pilot_name")
        val PILOT_HUB = stringPreferencesKey("pilot_hub")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val FUEL_UNIT = stringPreferencesKey("fuel_unit")
        val QNH_UNIT = stringPreferencesKey("qnh_unit")
        val TEMP_UNIT = stringPreferencesKey("temp_unit")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SIMBRIEF_ALIAS = stringPreferencesKey("simbrief_alias")
        val LAST_LANDED = stringPreferencesKey("last_landed")

        val FDV_IMPORT_MODE = stringPreferencesKey("fdv_import_mode")
        val FDV_LAST_PIREP_ID = longPreferencesKey("fdv_last_pirep_id")

        val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
        val DISMISSED_UPDATE = stringPreferencesKey("dismissed_update")
    }

    val pilotProfileFlow: Flow<PilotProfile> =
        context.dataStore.data.map { prefs ->
            PilotProfile(
                pilotId = prefs[Keys.PILOT_ID].orEmpty(),
                name = prefs[Keys.PILOT_NAME].orEmpty(),
                hub = prefs[Keys.PILOT_HUB].orEmpty()
            )
        }

    val settingsFlow: Flow<AppSettings> =
        context.dataStore.data.map { prefs ->
            AppSettings(
                themeMode = prefs[Keys.THEME_MODE].asEnumOrDefault(ThemeMode.FDV),
                weightUnit = prefs[Keys.WEIGHT_UNIT].asEnumOrDefault(WeightUnit.LB),
                fuelUnit = prefs[Keys.FUEL_UNIT].asEnumOrDefault(WeightUnit.LB),
                qnhUnit = prefs[Keys.QNH_UNIT].asEnumOrDefault(QnhUnit.INHG),
                tempUnit = prefs[Keys.TEMP_UNIT].asEnumOrDefault(TempUnit.F),
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
                simbriefAlias = prefs[Keys.SIMBRIEF_ALIAS].orEmpty()
            )
        }

    val lastLandedFlow: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.LAST_LANDED].orEmpty() }

    val fdvSyncFlow: Flow<FdvSyncState> =
        context.dataStore.data.map { prefs ->
            FdvSyncState(
                // An unknown stored name (e.g. from a future version) reads as "never imported"
                mode = prefs[Keys.FDV_IMPORT_MODE]?.let { name ->
                    FdvImportMode.entries.firstOrNull { it.name == name }
                },
                lastPirepId = prefs[Keys.FDV_LAST_PIREP_ID]
            )
        }

    /** Saves the sync state; FdvSyncState() with no arguments clears it. */
    suspend fun saveFdvSync(state: FdvSyncState) {
        context.dataStore.edit { prefs ->
            if (state.mode != null) prefs[Keys.FDV_IMPORT_MODE] = state.mode.name
            else prefs.remove(Keys.FDV_IMPORT_MODE)

            if (state.lastPirepId != null) prefs[Keys.FDV_LAST_PIREP_ID] = state.lastPirepId
            else prefs.remove(Keys.FDV_LAST_PIREP_ID)
        }
    }


    suspend fun lastUpdateCheck(): Long =
        context.dataStore.data.first()[Keys.LAST_UPDATE_CHECK] ?: 0L

    suspend fun setLastUpdateCheck(epochMs: Long) {
        context.dataStore.edit { it[Keys.LAST_UPDATE_CHECK] = epochMs }
    }

    // "Later" hides the card for that one version; a newer release shows it again
    val dismissedUpdateFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.DISMISSED_UPDATE].orEmpty() }

    suspend fun dismissUpdate(version: String) {
        context.dataStore.edit { it[Keys.DISMISSED_UPDATE] = version }
    }

    suspend fun savePilotProfile(profile: PilotProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PILOT_ID] = profile.pilotId.trim()
            prefs[Keys.PILOT_NAME] = profile.name.trim()
            prefs[Keys.PILOT_HUB] = profile.hub.trim().uppercase()
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.WEIGHT_UNIT] = settings.weightUnit.name
            prefs[Keys.FUEL_UNIT] = settings.fuelUnit.name
            prefs[Keys.QNH_UNIT] = settings.qnhUnit.name
            prefs[Keys.TEMP_UNIT] = settings.tempUnit.name
            prefs[Keys.KEEP_SCREEN_ON] = settings.keepScreenOn
            prefs[Keys.SIMBRIEF_ALIAS] = settings.simbriefAlias.trim()
        }
    }

    suspend fun setLastLanded(airport: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_LANDED] = airport.trim().uppercase()
        }
    }
}

/**
 * Safe enum parsing from stored strings.
 * Returns [default] if null/blank/invalid.
 */
private inline fun <reified T : Enum<T>> String?.asEnumOrDefault(default: T): T {
    val raw = this?.trim().orEmpty()
    if (raw.isBlank()) return default
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
}