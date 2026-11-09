package com.coryblische.razrclock

import android.app.Application
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.data.SettingsRepository
import com.coryblische.razrclock.display.CoverDisplayController
import com.coryblische.razrclock.posture.BatteryRepository
import com.coryblische.razrclock.posture.PostureRepository
import com.coryblische.razrclock.posture.PostureRules
import com.coryblische.razrclock.sys.SystemMonitor
import com.coryblische.razrclock.theme.LoadedPalette
import com.coryblische.razrclock.theme.loadPalette
import com.coryblische.razrclock.weather.WeatherRepository
import java.time.LocalDateTime

class RazrClockApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    lateinit var postureMonitor: PostureRepository
        private set
    lateinit var batteryMonitor: BatteryRepository
        private set
    lateinit var cover: CoverDisplayController
        private set
    lateinit var loadedPalette: LoadedPalette
        private set
    lateinit var systemMonitor: SystemMonitor
        private set
    lateinit var weather: WeatherRepository
        private set

    @Volatile var mainResumed: Boolean = false
    @Volatile var rearSessionActive: Boolean = false

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        postureMonitor = PostureRepository(this)
        batteryMonitor = BatteryRepository(this)
        loadedPalette = loadPalette(this)
        cover = CoverDisplayController(this, loadedPalette.palette)
        systemMonitor = SystemMonitor(this).also { it.start() }
        weather = WeatherRepository(this, settings).also { it.start() }
    }

    fun face(now: LocalDateTime = LocalDateTime.now()): ClockFaceState {
        val prefs = settings.preferences.value
        val posture = postureMonitor.snapshot.value
        val battery = batteryMonitor.state.value
        val rates = systemMonitor.rates.value
        val weatherNow = weather.current.value
        val stand = PostureRules.shouldTakeOver(
            posture = posture.posture,
            standEnabled = prefs.standMode,
            onlyWhileCharging = prefs.standOnlyWhileCharging,
            batteryStatus = battery.status,
        )
        return ClockFaceState(
            now = now,
            preferences = prefs,
            batteryPercent = battery.percent,
            batteryStatus = battery.status,
            posture = posture.posture,
            hingeAngleDegrees = posture.hingeAngle,
            standActive = stand,
            downBytesPerSec = rates.downBytesPerSec,
            upBytesPerSec = rates.upBytesPerSec,
            ramPercent = systemMonitor.ramPercent.value,
            weather = weatherNow,
        )
    }

    companion object {
        const val TAG = "RazrClock"
    }
}
