package com.coryblische.razrclock.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadedFlag = MutableStateFlow(false)

    val loaded: StateFlow<Boolean> = loadedFlag

    init {
        // v1.1 stored hand-tuned lens positions. Schema 2 replaces them
        // with the Motorola hole-map defaults.
        scope.launch {
            appContext.clockDataStore.edit { prefs ->
                if ((prefs[Keys.SCHEMA] ?: 1) < LENS_SCHEMA) {
                    val fresh = LensPair()
                    prefs[Keys.SCHEMA] = LENS_SCHEMA
                    prefs[Keys.LENS_AX] = fresh.ax
                    prefs[Keys.LENS_AY] = fresh.ay
                    prefs[Keys.LENS_AR] = fresh.ar
                    prefs[Keys.LENS_BX] = fresh.bx
                    prefs[Keys.LENS_BY] = fresh.by
                    prefs[Keys.LENS_BR] = fresh.br
                }
            }
        }
    }

    val preferences: StateFlow<ClockPreferences> = appContext.clockDataStore.data
        .map { it.toClock() }
        .onEach { loadedFlag.value = true }
        .stateIn(scope, SharingStarted.Eagerly, ClockPreferences())

    suspend fun update(block: (ClockPreferences) -> ClockPreferences) {
        appContext.clockDataStore.edit { prefs ->
            val next = block(prefs.toClock())
            prefs[Keys.USE_24] = next.use24Hour
            prefs[Keys.SECONDS] = next.showSeconds
            prefs[Keys.DATE] = next.showDate
            prefs[Keys.BATTERY] = next.showBattery
            prefs[Keys.STAND] = next.standMode
            prefs[Keys.STAND_CHARGING] = next.standOnlyWhileCharging
            prefs[Keys.FAHRENHEIT] = next.useFahrenheit
            prefs[Keys.DEVICE_LOCATION] = next.useDeviceLocation
            prefs[Keys.LATITUDE] = next.latitude
            prefs[Keys.LONGITUDE] = next.longitude
            prefs[Keys.LOCATION_LABEL] = next.locationLabel
            prefs[Keys.LENS_AX] = next.lenses.ax
            prefs[Keys.LENS_AY] = next.lenses.ay
            prefs[Keys.LENS_AR] = next.lenses.ar
            prefs[Keys.LENS_BX] = next.lenses.bx
            prefs[Keys.LENS_BY] = next.lenses.by
            prefs[Keys.LENS_BR] = next.lenses.br
            prefs[Keys.REQUEST_CALIBRATION] = next.requestCalibration
            prefs[Keys.LENS_ALIGN] = next.lensAlign
            prefs[Keys.SCHEMA] = LENS_SCHEMA
        }
    }

    private fun Preferences.toClock(): ClockPreferences = ClockPreferences(
        use24Hour = this[Keys.USE_24] ?: true,
        showSeconds = this[Keys.SECONDS] ?: true,
        showDate = this[Keys.DATE] ?: true,
        showBattery = this[Keys.BATTERY] ?: true,
        standMode = this[Keys.STAND] ?: true,
        standOnlyWhileCharging = this[Keys.STAND_CHARGING] ?: false,
        useFahrenheit = this[Keys.FAHRENHEIT] ?: true,
        useDeviceLocation = this[Keys.DEVICE_LOCATION] ?: false,
        latitude = this[Keys.LATITUDE] ?: ClockPreferences.NEW_YORK_LATITUDE,
        longitude = this[Keys.LONGITUDE] ?: ClockPreferences.NEW_YORK_LONGITUDE,
        locationLabel = this[Keys.LOCATION_LABEL] ?: "New York",
        lenses = if ((this[Keys.SCHEMA] ?: 1) >= LENS_SCHEMA) {
            LensPair(
                ax = this[Keys.LENS_AX] ?: LensPair().ax,
                ay = this[Keys.LENS_AY] ?: LensPair().ay,
                ar = this[Keys.LENS_AR] ?: LensPair().ar,
                bx = this[Keys.LENS_BX] ?: LensPair().bx,
                by = this[Keys.LENS_BY] ?: LensPair().by,
                br = this[Keys.LENS_BR] ?: LensPair().br,
            )
        } else {
            LensPair()
        },
        requestCalibration = this[Keys.REQUEST_CALIBRATION] ?: false,
        lensAlign = this[Keys.LENS_ALIGN] ?: true,
    )

    private object Keys {
        val USE_24 = booleanPreferencesKey("use24")
        val SECONDS = booleanPreferencesKey("seconds")
        val DATE = booleanPreferencesKey("date")
        val BATTERY = booleanPreferencesKey("battery")
        val STAND = booleanPreferencesKey("stand")
        val STAND_CHARGING = booleanPreferencesKey("stand_charging")
        val FAHRENHEIT = booleanPreferencesKey("fahrenheit")
        val DEVICE_LOCATION = booleanPreferencesKey("device_location")
        val LATITUDE = floatPreferencesKey("latitude")
        val LONGITUDE = floatPreferencesKey("longitude")
        val LOCATION_LABEL = stringPreferencesKey("location_label")
        val LENS_AX = floatPreferencesKey("lens_ax")
        val LENS_AY = floatPreferencesKey("lens_ay")
        val LENS_AR = floatPreferencesKey("lens_ar")
        val LENS_BX = floatPreferencesKey("lens_bx")
        val LENS_BY = floatPreferencesKey("lens_by")
        val LENS_BR = floatPreferencesKey("lens_br")
        val REQUEST_CALIBRATION = booleanPreferencesKey("request_calibration")
        val LENS_ALIGN = booleanPreferencesKey("lens_align")
        val SCHEMA = intPreferencesKey("lens_schema")
    }

    companion object {
        const val LENS_SCHEMA = 2
    }
}
