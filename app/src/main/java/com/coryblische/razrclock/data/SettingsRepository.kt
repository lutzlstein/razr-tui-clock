package com.coryblische.razrclock.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

private val Context.clockDataStore by preferencesDataStore(name = "clock")

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadedFlag = MutableStateFlow(false)

    val loaded: StateFlow<Boolean> = loadedFlag

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
        }
    }

    private fun Preferences.toClock(): ClockPreferences = ClockPreferences(
        use24Hour = this[Keys.USE_24] ?: true,
        showSeconds = this[Keys.SECONDS] ?: true,
        showDate = this[Keys.DATE] ?: true,
        showBattery = this[Keys.BATTERY] ?: true,
        standMode = this[Keys.STAND] ?: true,
        standOnlyWhileCharging = this[Keys.STAND_CHARGING] ?: false,
    )

    private object Keys {
        val USE_24 = booleanPreferencesKey("use24")
        val SECONDS = booleanPreferencesKey("seconds")
        val DATE = booleanPreferencesKey("date")
        val BATTERY = booleanPreferencesKey("battery")
        val STAND = booleanPreferencesKey("stand")
        val STAND_CHARGING = booleanPreferencesKey("stand_charging")
    }
}
