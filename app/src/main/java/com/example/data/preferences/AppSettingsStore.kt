package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "engineering_settings")

data class EngineeringAppSettings(
    val activeStandardEdition: String = "BS 7671:2018+A2:2022",
    val defaultVoltageDropPowerPct: Double = 5.0,
    val defaultVoltageDropLightingPct: Double = 3.0,
    val defaultEngineerName: String = "",
    val themeMode: String = "SYSTEM"
)

class AppSettingsStore(private val context: Context) {

    private object Keys {
        val STANDARD_EDITION = stringPreferencesKey("standard_edition")
        val VD_POWER_PCT = doublePreferencesKey("vd_power_pct")
        val VD_LIGHTING_PCT = doublePreferencesKey("vd_lighting_pct")
        val ENGINEER_NAME = stringPreferencesKey("engineer_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<EngineeringAppSettings> = context.settingsDataStore.data.map { prefs ->
        EngineeringAppSettings(
            activeStandardEdition = prefs[Keys.STANDARD_EDITION] ?: "BS 7671:2018+A2:2022",
            defaultVoltageDropPowerPct = prefs[Keys.VD_POWER_PCT] ?: 5.0,
            defaultVoltageDropLightingPct = prefs[Keys.VD_LIGHTING_PCT] ?: 3.0,
            defaultEngineerName = prefs[Keys.ENGINEER_NAME] ?: "",
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM"
        )
    }

    suspend fun updateSettings(transform: (EngineeringAppSettings) -> EngineeringAppSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = EngineeringAppSettings(
                activeStandardEdition = prefs[Keys.STANDARD_EDITION] ?: "BS 7671:2018+A2:2022",
                defaultVoltageDropPowerPct = prefs[Keys.VD_POWER_PCT] ?: 5.0,
                defaultVoltageDropLightingPct = prefs[Keys.VD_LIGHTING_PCT] ?: 3.0,
                defaultEngineerName = prefs[Keys.ENGINEER_NAME] ?: "",
                themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM"
            )
            val updated = transform(current)
            prefs[Keys.STANDARD_EDITION] = updated.activeStandardEdition
            prefs[Keys.VD_POWER_PCT] = updated.defaultVoltageDropPowerPct
            prefs[Keys.VD_LIGHTING_PCT] = updated.defaultVoltageDropLightingPct
            prefs[Keys.ENGINEER_NAME] = updated.defaultEngineerName
            prefs[Keys.THEME_MODE] = updated.themeMode
        }
    }
}
