package com.coryblische.razrclock.dream

import android.service.dreams.DreamService
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.coryblische.razrclock.RazrClockApp
import com.coryblische.razrclock.display.ComposeHostLifecycle
import com.coryblische.razrclock.display.STAND_WINDOW_BRIGHTNESS
import com.coryblische.razrclock.display.attachComposeOwners
import com.coryblische.razrclock.theme.RazrTheme
import com.coryblische.razrclock.ui.ClockScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Screensaver fallback. Motorola Desk display is separate and will compete with this. */
class RazrClockDream : DreamService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val composeLifecycle = ComposeHostLifecycle()
    private var tick: Job? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false
        val app = application as RazrClockApp
        app.postureMonitor.acquire()
        app.batteryMonitor.acquire()
        app.handPresence.acquire()
        val face = mutableStateOf(app.face(app.secondClock.now.value))
        tick = scope.launch {
            app.secondClock.now.collect { tickNow ->
                face.value = app.face(tickNow)
            }
        }
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            attachComposeOwners(composeLifecycle)
            setContent {
                RazrTheme(palette = app.loadedPalette.palette) {
                    ClockScreen(state = face.value)
                }
            }
        }
        composeLifecycle.create()
        setContentView(view)
        window?.decorView?.attachComposeOwners(composeLifecycle)
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.attributes = window?.attributes?.apply {
            screenBrightness = STAND_WINDOW_BRIGHTNESS
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        composeLifecycle.resume()
        scope.launch {
            app.handPresence.cue
                .map { it.enabled to it.brightness }
                .distinctUntilChanged()
                .collect { (enabled, level) ->
                    window?.attributes = window?.attributes?.apply {
                        screenBrightness = if (enabled) level else STAND_WINDOW_BRIGHTNESS
                    }
                }
        }
        scope.launch {
            app.handPresence.cue
                .map { Triple(it.active, it.view, it.spin) }
                .distinctUntilChanged()
                .collect { face.value = app.face() }
        }
    }

    override fun onDetachedFromWindow() {
        tick?.cancel()
        scope.cancel()
        composeLifecycle.destroy()
        val app = application as? RazrClockApp
        app?.postureMonitor?.release()
        app?.batteryMonitor?.release()
        app?.handPresence?.release()
        super.onDetachedFromWindow()
    }
}
