package com.fdv.fdvflightlogger.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
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

        val LAST_LANDED = stringPreferencesKey("last_landed")
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
                tempUnit = prefs[Keys.TEMP_UNIT].asEnumOrDefault(TempUnit.F)
            )
        }

    val lastLandedFlow: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.LAST_LANDED].orEmpty() }

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
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
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
