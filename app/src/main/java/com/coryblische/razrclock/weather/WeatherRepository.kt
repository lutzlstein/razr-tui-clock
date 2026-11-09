package com.coryblische.razrclock.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.coryblische.razrclock.RazrClockApp
import com.coryblische.razrclock.data.ClockPreferences
import com.coryblische.razrclock.data.SettingsRepository
import com.coryblische.razrclock.data.HourCast
import com.coryblische.razrclock.data.WeatherNow
import com.coryblische.razrclock.data.upcomingHours
import com.coryblische.razrclock.data.clockDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class WeatherRepository(
    context: Context,
    private val settings: SettingsRepository,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private val _current = MutableStateFlow<WeatherNow?>(null)
    val current: StateFlow<WeatherNow?> = _current.asStateFlow()

    fun start() {
        if (job != null) return
        job = scope.launch {
            loadCache()
            settings.preferences
                .map { it.latitude to it.longitude to it.useDeviceLocation to it.locationLabel }
                .distinctUntilChanged()
                .collect {
                    refresh()
                }
        }
        scope.launch {
            while (isActive) {
                delay(REFRESH_MS)
                refresh()
            }
        }
    }

    private suspend fun loadCache() {
        val stored = appContext.clockDataStore.data.first()
        val fetched = stored[Keys.FETCHED] ?: return
        val temp = stored[Keys.TEMP] ?: return
        val high = stored[Keys.HIGH] ?: return
        val low = stored[Keys.LOW] ?: return
        val code = stored[Keys.CODE] ?: return
        _current.value = WeatherNow(
            tempC = temp,
            highC = high,
            lowC = low,
            code = code,
            fetchedAtMs = fetched,
            stale = System.currentTimeMillis() - fetched > STALE_MS,
            place = stored[Keys.PLACE] ?: "",
            hours = decodeHours(stored[Keys.HOURS]),
        )
    }

    private suspend fun refresh() {
        val prefs = settings.preferences.value
        val place = if (prefs.useDeviceLocation) "here" else prefs.locationLabel.ifBlank { "saved" }
        val (lat, lon) = resolveLatLon(prefs)
        val fetched = fetch(lat, lon, place)
        if (fetched != null) {
            _current.value = fetched
            appContext.clockDataStore.edit { stored ->
                stored[Keys.TEMP] = fetched.tempC
                stored[Keys.HIGH] = fetched.highC
                stored[Keys.LOW] = fetched.lowC
                stored[Keys.CODE] = fetched.code
                stored[Keys.FETCHED] = fetched.fetchedAtMs
                stored[Keys.PLACE] = fetched.place
                stored[Keys.HOURS] = encodeHours(fetched.hours)
                stored[Keys.OK] = true
            }
        } else {
            _current.value = _current.value?.copy(stale = true)
            Log.i(RazrClockApp.TAG, "Weather fetch failed; showing cache")
        }
    }

    private fun resolveLatLon(prefs: ClockPreferences): Pair<Double, Double> {
        if (prefs.useDeviceLocation) {
            coarseFix()?.let { return it.latitude to it.longitude }
        }
        return prefs.latitude.toDouble() to prefs.longitude.toDouble()
    }

    private fun coarseFix(): Location? {
        val granted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.FUSED_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }

    private fun fetch(latitude: Double, longitude: Double, place: String): WeatherNow? {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&hourly=temperature_2m,weather_code" +
                "&forecast_hours=8" +
                "&temperature_unit=celsius&timezone=America%2FNew_York&forecast_days=1",
        )
        return runCatching {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "RazrClock/1.3")
            }
            try {
                if (connection.responseCode !in 200..299) return null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                val daily = json.getJSONObject("daily")
                val hours = json.optJSONObject("hourly")?.let { hourly ->
                    val times = hourly.optJSONArray("time")
                    val temps = hourly.optJSONArray("temperature_2m")
                    val codes = hourly.optJSONArray("weather_code")
                    if (times == null || temps == null || codes == null) {
                        emptyList()
                    } else {
                        upcomingHours(
                            times = List(times.length()) { times.getString(it) },
                            temps = List(temps.length()) { temps.getDouble(it) },
                            codes = List(codes.length()) { codes.getInt(it) },
                            now = LocalDateTime.now(),
                        )
                    }
                }.orEmpty()
                WeatherNow(
                    tempC = current.getDouble("temperature_2m"),
                    code = current.getInt("weather_code"),
                    highC = daily.getJSONArray("temperature_2m_max").getDouble(0),
                    lowC = daily.getJSONArray("temperature_2m_min").getDouble(0),
                    fetchedAtMs = System.currentTimeMillis(),
                    stale = false,
                    place = place,
                    hours = hours,
                )
            } finally {
                connection.disconnect()
            }
        }.onFailure { Log.w(RazrClockApp.TAG, "Open-Meteo error", it) }.getOrNull()
    }

    private object Keys {
        val TEMP = doublePreferencesKey("weather_temp_c")
        val HIGH = doublePreferencesKey("weather_high_c")
        val LOW = doublePreferencesKey("weather_low_c")
        val CODE = intPreferencesKey("weather_code")
        val FETCHED = longPreferencesKey("weather_fetched_at")
        val PLACE = stringPreferencesKey("weather_place")
        val HOURS = stringPreferencesKey("weather_hours")
        val OK = booleanPreferencesKey("weather_ok")
    }

    private fun encodeHours(hours: List<HourCast>): String =
        hours.joinToString("|") { "${it.hour},${it.tempC},${it.code}" }

    private fun decodeHours(raw: String?): List<HourCast> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("|").mapNotNull { part ->
            val bits = part.split(",")
            if (bits.size != 3) return@mapNotNull null
            val hour = bits[0].toIntOrNull() ?: return@mapNotNull null
            val temp = bits[1].toDoubleOrNull() ?: return@mapNotNull null
            val code = bits[2].toIntOrNull() ?: return@mapNotNull null
            HourCast(hour, temp, code)
        }
    }

    companion object {
        val REFRESH_MS = TimeUnit.MINUTES.toMillis(20)
        val STALE_MS = TimeUnit.MINUTES.toMillis(35)
    }
}
