package com.coryblische.razrclock

import android.app.Application
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.data.SettingsRepository
import com.coryblische.razrclock.display.CoverDisplayController
import com.coryblische.razrclock.posture.BatteryRepository
import com.coryblische.razrclock.posture.PostureRepository
import com.coryblische.razrclock.posture.PostureRules
import com.coryblische.razrclock.presence.CoverView
import com.coryblische.razrclock.presence.HandPresence
import com.coryblische.razrclock.sys.SystemMonitor
import com.coryblische.razrclock.theme.LoadedPalette
import com.coryblische.razrclock.theme.loadPalette
import com.coryblische.razrclock.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
    lateinit var handPresence: HandPresence
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
        handPresence = HandPresence(this)
        appScope.launch {
            settings.preferences.map { it.handGestures }.distinctUntilChanged().collect { enabled ->
                handPresence.setEnabled(enabled)
            }
        }
        appScope.launch {
            handPresence.cue.collect { cue ->
                cover.render(face())
                cover.applyBrightness(if (cue.enabled) cue.brightness else null)
            }
        }
    }

    fun face(now: LocalDateTime = LocalDateTime.now()): ClockFaceState {
        val prefs = settings.preferences.value
        val posture = postureMonitor.snapshot.value
        val battery = batteryMonitor.state.value
        val rates = systemMonitor.rates.value
        val weatherNow = weather.current.value
        val hand = handPresence.cue.value
        val gestures = prefs.handGestures && hand.enabled
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
            handActive = gestures && hand.active,
            coverView = if (gestures) hand.view else CoverView.CLOCK,
            handSpin = if (gestures) hand.spin else 0,
        )
    }

    companion object {
        const val TAG = "RazrClock"
    }
}
