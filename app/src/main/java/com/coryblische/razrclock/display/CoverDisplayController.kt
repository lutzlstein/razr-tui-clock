package com.coryblische.razrclock.display

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import com.coryblische.razrclock.MainActivity
import com.coryblische.razrclock.RazrClockApp
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.theme.OmarchyPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ways onto the Razr cover, which adb reports as display 1 with
 * FLAG_PRESENTATION. canHostTasks is a hidden API and is not queried.
 *
 * 1. A Presentation on that display. This is the API the presentation flag is for.
 *    Android 16 allows it on internal displays, then rejects it if it would cover
 *    the app's own task or if the app is not allowed to use the display.
 * 2. ActivityOptions.setLaunchDisplayId. The public call either works or throws;
 *    there is no preflight. Motorola's "Allow on external display" setting is
 *    what actually makes the cover host the task.
 * 3. A full-screen intent that brings [MainActivity] forward on whichever display
 *    the system will actually put a task on.
 */
class CoverDisplayController(
    context: Context,
    private val palette: OmarchyPalette,
) {
    private val appContext = context.applicationContext
    private val displayManager = appContext.getSystemService(DisplayManager::class.java)
    private var presentation: ClockPresentation? = null
    private var presentFailures = 0

    private val _report = MutableStateFlow("Cover display not requested yet.")
    val report: StateFlow<String> = _report.asStateFlow()

    fun preferredCover(): Display? {
        val displays = displayManager?.displays?.toList().orEmpty()
        return displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && hasPresentationFlag(it) }
            ?: displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
    }

    fun ensurePresentation(host: Context, state: ClockFaceState) {
        val current = presentation
        if (current != null && current.isShowing) {
            current.render(state)
            return
        }
        if (presentFailures >= 3) return
        val display = preferredCover()
        if (display == null) {
            _report.value = "No secondary display."
            return
        }
        showPresentation(host, display, state)
    }

    fun showPresentation(host: Context, display: Display, state: ClockFaceState): Result<Unit> {
        dismissPresentation()
        return runCatching {
            val next = ClockPresentation(host, display, state, palette)
            next.show()
            presentation = next
            presentFailures = 0
            _report.value = "Presentation showing on display ${display.displayId}."
            Log.i(RazrClockApp.TAG, _report.value)
            Unit
        }.onFailure { error ->
            presentFailures += 1
            val message = "Presentation on display ${display.displayId} failed: ${error.javaClass.simpleName}: ${error.message}"
            _report.value = message
            Log.w(RazrClockApp.TAG, message, error)
        }
    }

    fun dismissPresentation() {
        val current = presentation ?: return
        presentation = null
        runCatching { if (current.isShowing) current.dismiss() }
    }

    fun isPresentationShowing(): Boolean = presentation?.isShowing == true

    fun note(message: String) {
        _report.value = message
        Log.i(RazrClockApp.TAG, message)
    }

    fun resetFailures() {
        presentFailures = 0
    }

    fun launchOnDisplay(context: Context, display: Display, calibrate: Boolean = false): Result<Unit> {
        return runCatching {
            val intent = clockIntent(context, calibrate)
            val options = ActivityOptions.makeBasic().apply { launchDisplayId = display.displayId }
            context.startActivity(intent, options.toBundle())
            _report.value = "startActivity on display ${display.displayId}."
            Log.i(RazrClockApp.TAG, _report.value)
            Unit
        }.onFailure { error ->
            val message = "setLaunchDisplayId ${display.displayId} failed: ${error.javaClass.simpleName}: ${error.message}"
            _report.value = message
            Log.w(RazrClockApp.TAG, message, error)
        }
    }

    fun clockIntent(context: Context, calibrate: Boolean = false): Intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        putExtra(MainActivity.EXTRA_FORCE_CLOCK, true)
        putExtra(MainActivity.EXTRA_STAND, true)
        if (calibrate) putExtra(MainActivity.EXTRA_CALIBRATE, true)
    }

    fun describe(context: Context): String {
        val displays = displayManager?.displays?.toList().orEmpty()
        if (displays.isEmpty()) return "No displays."
        return displays.joinToString("\n") { display ->
            val mode = display.mode
            val dpi = runCatching {
                context.createDisplayContext(display).resources.displayMetrics.densityDpi
            }.getOrNull()
            val cutout = display.cutout?.boundingRects?.joinToString { rect ->
                "(${rect.left},${rect.top})-(${rect.right},${rect.bottom})"
            } ?: "none"
            "id=${display.displayId} ${mode.physicalWidth}x${mode.physicalHeight} dpi=$dpi " +
                "flags=0x${display.flags.toString(16)} (${flagNames(display.flags)}) " +
                "cutout=$cutout name=${display.name}"
        }
    }

    private fun hasPresentationFlag(display: Display): Boolean =
        display.flags and Display.FLAG_PRESENTATION != 0

    companion object {
        fun flagNames(flags: Int): String {
            val names = buildList {
                if (flags and Display.FLAG_SUPPORTS_PROTECTED_BUFFERS != 0) add("PROTECTED_BUFFERS")
                if (flags and Display.FLAG_SECURE != 0) add("SECURE")
                if (flags and Display.FLAG_PRIVATE != 0) add("PRIVATE")
                if (flags and Display.FLAG_PRESENTATION != 0) add("PRESENTATION")
                if (flags and Display.FLAG_ROUND != 0) add("ROUND")
                // Hidden in the SDK stub. Values follow Display.java: OWN_CONTENT_ONLY is bit 7
                // on some releases and bit 8 on others, so the hex above is the source of truth.
                // dumpsys on leap names FLAG_OWN_CONTENT_ONLY and FLAG_OWN_DISPLAY_GROUP.
            }
            return if (names.isEmpty()) "none" else names.joinToString(",")
        }

        fun fullScreenPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(MainActivity.EXTRA_FORCE_CLOCK, true)
                putExtra(MainActivity.EXTRA_STAND, true)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getActivity(context, 7, intent, flags)
        }

    }
}
