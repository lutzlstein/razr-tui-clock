package com.coryblische.razrclock.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.coryblische.razrclock.MainActivity
import com.coryblische.razrclock.RazrClockApp
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.display.applyDeskWindow
import com.coryblische.razrclock.posture.PostureRules
import com.coryblische.razrclock.posture.label
import com.coryblische.razrclock.stand.StandModeService
import com.coryblische.razrclock.theme.RazrTheme
import com.coryblische.razrclock.wantsClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun AppRoot(activity: MainActivity) {
    val app = activity.application as RazrClockApp
    val prefs by app.settings.preferences.collectAsState()
    val posture by app.postureMonitor.snapshot.collectAsState()
    val battery by app.batteryMonitor.state.collectAsState()
    val report by app.cover.report.collectAsState()
    val opens by activity.openRequests.collectAsState()
    val scope = rememberCoroutineScope()

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(prefs.showSeconds) {
        while (true) {
            now = LocalDateTime.now()
            val step = if (prefs.showSeconds) 1_000L else 60_000L
            val wait = step - (System.currentTimeMillis() % step)
            delay(wait.coerceIn(200L, step))
        }
    }

    val standActive = PostureRules.shouldTakeOver(
        posture = posture.posture,
        standEnabled = prefs.standMode,
        onlyWhileCharging = prefs.standOnlyWhileCharging,
        batteryStatus = battery.status,
    )
    val clock = ClockFaceState(
        now = now,
        preferences = prefs,
        batteryPercent = battery.percent,
        batteryStatus = battery.status,
        posture = posture.posture,
        hingeAngleDegrees = posture.hingeAngle,
        standActive = standActive,
    )
    SideEffect { activity.renderRear(clock) }

    var onCover by remember { mutableStateOf(activity.isOnCover()) }
    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onCover = activity.isOnCover()
        }
        activity.lifecycle.addObserver(observer)
        onDispose { activity.lifecycle.removeObserver(observer) }
    }

    var screen by rememberSaveable {
        mutableStateOf(if (onCover || activity.intent.wantsClock()) SCREEN_CLOCK else SCREEN_SETTINGS)
    }
    var seenStand by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(opens) {
        if (activity.intent.wantsClock()) screen = SCREEN_CLOCK
    }
    LaunchedEffect(onCover) {
        if (onCover) screen = SCREEN_CLOCK
    }
    LaunchedEffect(standActive) {
        if (standActive && !seenStand) {
            seenStand = true
            screen = SCREEN_CLOCK
            activity.presentOnRear()
        }
        if (!standActive) {
            seenStand = false
            activity.closeRearSession()
        }
    }
    LaunchedEffect(prefs.standMode) {
        if (prefs.standMode) StandModeService.start(activity) else StandModeService.stop(activity)
    }

    BackHandler(enabled = screen == SCREEN_CLOCK) {
        screen = SCREEN_SETTINGS
    }

    DisposableEffect(screen, standActive) {
        val desk = screen == SCREEN_CLOCK && standActive
        activity.applyDeskWindow(desk)
        val insets = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (screen == SCREEN_CLOCK) {
            insets.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insets.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insets.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (desk) activity.applyDeskWindow(false)
        }
    }

    val diagnostics = buildString {
        appendLine("palette: ${app.loadedPalette.source}")
        appendLine(
            "posture: ${posture.posture.label()}  hinge=${posture.hingeAngle ?: "none"}  " +
                "vendor=${posture.vendorRaw ?: "none"}  folding=${posture.folding}",
        )
        appendLine("sensors: hinge=${posture.hingePresent}  motorola posture=${posture.vendorPresent}")
        appendLine("battery: ${if (battery.percent < 0) "--" else "${battery.percent}%"} ${battery.status}")
        appendLine("stand=$standActive  onCover=$onCover  rear=${app.rearSessionActive}")
        appendLine(report)
        append(app.cover.describe(activity))
    }

    RazrTheme(palette = app.loadedPalette.palette) {
        if (screen == SCREEN_CLOCK) {
            ClockScreen(state = clock)
        } else {
            SettingsScreen(
                prefs = prefs,
                paletteSource = app.loadedPalette.source,
                diagnostics = diagnostics,
                onChange = { next -> scope.launch { app.settings.update { next } } },
                onPreview = { screen = SCREEN_CLOCK },
                onShowCover = {
                    val display = app.cover.preferredCover()
                    if (display == null) {
                        app.cover.note("No secondary display.")
                    } else {
                        val shown = app.cover.showPresentation(activity, display, clock)
                        if (shown.isFailure) app.cover.launchOnDisplay(activity, display)
                    }
                },
                onRearTransfer = { activity.transferToRear() },
                onDreamSettings = {
                    activity.tryStart(Intent(Settings.ACTION_DREAM_SETTINGS), "Dream settings are not available.")
                },
                onFullScreenIntent = {
                    activity.tryStart(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                            .setData(Uri.parse("package:${activity.packageName}")),
                        "Full-screen intent settings are not available.",
                    )
                },
                onBatterySettings = {
                    activity.tryStart(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(Uri.parse("package:${activity.packageName}")),
                        "Battery optimization settings are not available.",
                    )
                },
                onNotificationSettings = {
                    activity.tryStart(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName),
                        "Notification settings are not available.",
                    )
                },
            )
        }
    }
}

private fun Activity.tryStart(intent: Intent, failure: String) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        (application as RazrClockApp).cover.note(failure)
    }
}

private const val SCREEN_CLOCK = "clock"
private const val SCREEN_SETTINGS = "settings"
