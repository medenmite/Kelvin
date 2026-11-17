package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.RunMode
import com.example.model.SafetySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.safetyDataStore: DataStore<Preferences> by preferencesDataStore(name = "embercore_safety_prefs")

class SafetyPrefsRepository(private val context: Context) {

    private object Keys {
        val MAX_TEMP_C = floatPreferencesKey("max_temp_c")
        val MIN_BATTERY_PERCENT = intPreferencesKey("min_battery_percent")
        val AUTO_STEP_DOWN = booleanPreferencesKey("auto_step_down")
        val USE_FAHRENHEIT = booleanPreferencesKey("use_fahrenheit")
        val USE_OLED_BLACK_THEME = booleanPreferencesKey("use_oled_black_theme")
        val PREFERRED_RUN_MODE = stringPreferencesKey("preferred_run_mode")
        val DISCLAIMER_DISMISSED = booleanPreferencesKey("disclaimer_dismissed")
    }

    val settingsFlow: Flow<SafetySettings> = context.safetyDataStore.data.map { prefs ->
        val modeName = prefs[Keys.PREFERRED_RUN_MODE]
        val runMode = RunMode.entries.firstOrNull { it.name == modeName } ?: RunMode.SCREEN_ON
        SafetySettings(
            maxTempC = (prefs[Keys.MAX_TEMP_C] ?: 43.0f).coerceIn(37.0f, 48.0f),
            minBatteryPercent = (prefs[Keys.MIN_BATTERY_PERCENT] ?: 15).coerceIn(5, 50),
            autoStepDown = prefs[Keys.AUTO_STEP_DOWN] ?: true,
            useFahrenheit = prefs[Keys.USE_FAHRENHEIT] ?: false,
            useOledBlackTheme = prefs[Keys.USE_OLED_BLACK_THEME] ?: false,
            preferredRunMode = runMode,
            disclaimerDismissed = prefs[Keys.DISCLAIMER_DISMISSED] ?: false,
        )
    }

    suspend fun setMaxTempC(value: Float) {
        context.safetyDataStore.edit { it[Keys.MAX_TEMP_C] = value.coerceIn(37.0f, 48.0f) }
    }

    suspend fun setMinBatteryPercent(value: Int) {
        context.safetyDataStore.edit { it[Keys.MIN_BATTERY_PERCENT] = value.coerceIn(5, 50) }
    }

    suspend fun setAutoStepDown(enabled: Boolean) {
        context.safetyDataStore.edit { it[Keys.AUTO_STEP_DOWN] = enabled }
    }

    suspend fun setUseFahrenheit(useFahrenheit: Boolean) {
        context.safetyDataStore.edit { it[Keys.USE_FAHRENHEIT] = useFahrenheit }
    }

    suspend fun setUseOledBlackTheme(enabled: Boolean) {
        context.safetyDataStore.edit { it[Keys.USE_OLED_BLACK_THEME] = enabled }
    }

    suspend fun setPreferredRunMode(runMode: RunMode) {
        context.safetyDataStore.edit { it[Keys.PREFERRED_RUN_MODE] = runMode.name }
    }

    suspend fun setDisclaimerDismissed(dismissed: Boolean) {
        context.safetyDataStore.edit { it[Keys.DISCLAIMER_DISMISSED] = dismissed }
    }
}
