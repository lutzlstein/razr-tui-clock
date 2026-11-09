package com.coryblische.razrclock.display

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import com.coryblische.razrclock.RazrClockApp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.theme.OmarchyPalette
import com.coryblische.razrclock.theme.RazrTheme
import com.coryblische.razrclock.ui.ClockScreen

class ClockPresentation(
    outerContext: Context,
    display: Display,
    initial: ClockFaceState,
    private val palette: OmarchyPalette,
) : Presentation(outerContext, display, android.R.style.Theme_Material_NoActionBar_Fullscreen) {
    private val face = mutableStateOf(initial)
    private val composeLifecycle = ComposeHostLifecycle()

    fun render(state: ClockFaceState) {
        face.value = state
    }

    fun applyBrightness(level: Float?) {
        val window = window ?: return
        window.attributes = window.attributes.apply {
            screenBrightness = level ?: STAND_WINDOW_BRIGHTNESS
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val window = window ?: return
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
        )
        window.attributes = window.attributes.apply {
            screenBrightness = STAND_WINDOW_BRIGHTNESS
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            attachComposeOwners(composeLifecycle)
            setContent {
                RazrTheme(palette = palette) {
                    ClockScreen(state = face.value)
                }
            }
        }
        composeLifecycle.create()
        setContentView(view)
        window.decorView.attachComposeOwners(composeLifecycle)
        composeLifecycle.resume()
    }

    override fun onStart() {
        super.onStart()
        (context.applicationContext as? RazrClockApp)?.handPresence?.let { presence ->
            presence.acquire()
            val cue = presence.cue.value
            if (cue.enabled) applyBrightness(cue.brightness)
        }
    }

    override fun onStop() {
        (context.applicationContext as? RazrClockApp)?.handPresence?.release()
        composeLifecycle.destroy()
        super.onStop()
    }
}
