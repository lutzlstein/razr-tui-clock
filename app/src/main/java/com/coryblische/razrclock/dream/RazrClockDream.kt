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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime

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
        val face = mutableStateOf(app.face(LocalDateTime.now()))
        tick = scope.launch {
            while (isActive) {
                val state = app.face(LocalDateTime.now())
                face.value = state
                val step = if (state.preferences.showSeconds) 1000L else 60_000L
                val wait = step - (System.currentTimeMillis() % step)
                delay(wait.coerceIn(200L, step))
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
    }

    override fun onDetachedFromWindow() {
        tick?.cancel()
        scope.cancel()
        composeLifecycle.destroy()
        val app = application as? RazrClockApp
        app?.postureMonitor?.release()
        app?.batteryMonitor?.release()
        super.onDetachedFromWindow()
    }
}
