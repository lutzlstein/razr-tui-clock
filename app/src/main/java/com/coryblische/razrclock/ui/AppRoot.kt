package com.coryblische.razrclock.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.coryblische.razrclock.data.LensPair
import com.coryblische.razrclock.display.STAND_WINDOW_BRIGHTNESS
import com.coryblische.razrclock.display.applyDeskWindow
import com.coryblische.razrclock.posture.PostureRules
import com.coryblische.razrclock.posture.label
import com.coryblische.razrclock.presence.CoverView
import com.coryblische.razrclock.presence.HandCue
import com.coryblische.razrclock.stand.StandModeService
import com.coryblische.razrclock.theme.RazrTheme
import com.coryblische.razrclock.wantsCalibration
import com.coryblische.razrclock.wantsClock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun AppRoot(activity: MainActivity) {
    val app = activity.application as RazrClockApp
    val prefs by app.settings.preferences.collectAsState()
    val posture by app.postureMonitor.snapshot.collectAsState()
    val battery by app.batteryMonitor.state.collectAsState()
    val report by app.cover.report.collectAsState()
    val opens by activity.openRequests.collectAsState()
    val rates by app.systemMonitor.rates.collectAsState()
    val ram by app.systemMonitor.ramPercent.collectAsState()
    val weather by app.weather.current.collectAsState()
    val handFlow = remember(app) {
        app.handPresence.cue
            .map {
                HandCue(
                    enabled = it.enabled,
                    active = it.active,
                    view = it.view,
                    spin = it.spin,
                    flash = it.flash,
                    using = it.using,
                    available = it.available,
                )
            }
            .distinctUntilChanged()
    }
    val hand by handFlow.collectAsState(initial = HandCue())
    val scope = rememberCoroutineScope()
    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch { app.settings.update { it.copy(useDeviceLocation = granted) } }
    }
    var lensWrite by remember { mutableStateOf<Job?>(null) }

    val now by app.secondClock.now.collectAsState()

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
        downBytesPerSec = rates.downBytesPerSec,
        upBytesPerSec = rates.upBytesPerSec,
        ramPercent = ram,
        weather = weather,
        handActive = prefs.handGestures && hand.enabled && hand.active,
        coverView = if (prefs.handGestures && hand.enabled) hand.view else CoverView.CLOCK,
        handSpin = if (prefs.handGestures && hand.enabled) hand.spin else 0,
        gearLit = prefs.handGestures && hand.enabled && hand.flash,
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
    var calibrating by rememberSaveable { mutableStateOf(false) }
    var seenStand by rememberSaveable { mutableStateOf(false) }
    var handledCalibrateIntent by rememberSaveable { mutableStateOf(false) }

    fun openCoverCalibration() {
        scope.launch {
            app.settings.update { it.copy(requestCalibration = true) }
            val display = app.cover.preferredCover()
            if (display != null) {
                app.cover.launchOnDisplay(activity, display, calibrate = true)
            } else {
                app.cover.note("Open the clock on the cover, then long-press it.")
            }
        }
    }

    LaunchedEffect(opens) {
        if (activity.intent.wantsClock()) screen = SCREEN_CLOCK
    }
    LaunchedEffect(onCover, prefs.requestCalibration, opens) {
        val fromIntent = activity.intent.wantsCalibration() && !handledCalibrateIntent
        if (onCover && (prefs.requestCalibration || fromIntent)) {
            screen = SCREEN_CLOCK
            calibrating = true
            handledCalibrateIntent = true
            if (prefs.requestCalibration) {
                scope.launch { app.settings.update { it.copy(requestCalibration = false) } }
            }
        }
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

    DisposableEffect(screen) {
        if (screen == SCREEN_CLOCK) app.handPresence.acquire()
        onDispose { if (screen == SCREEN_CLOCK) app.handPresence.release() }
    }

    LaunchedEffect(screen, standActive, prefs.handGestures) {
        if (screen != SCREEN_CLOCK) {
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
            return@LaunchedEffect
        }
        if (!prefs.handGestures) {
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = if (standActive) {
                    STAND_WINDOW_BRIGHTNESS
                } else {
                    WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
            return@LaunchedEffect
        }
        app.handPresence.cue.collect { cue ->
            activity.window.attributes = activity.window.attributes.apply {
                screenBrightness = cue.brightness
            }
        }
    }

    DisposableEffect(screen, standActive, prefs.handGestures) {
        val desk = screen == SCREEN_CLOCK && (standActive || prefs.handGestures)
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
        appendLine(
            "hand: ${if (prefs.handGestures) "on" else "off"} active=${hand.active} " +
                "view=${hand.view} using=${hand.using}",
        )
        if (hand.available.isNotBlank()) appendLine(hand.available)
        appendLine(report)
        append(app.cover.describe(activity))
    }

    RazrTheme(palette = app.loadedPalette.palette) {
        if (screen == SCREEN_CLOCK) {
            ClockScreen(
                state = clock,
                calibrating = calibrating && onCover,
                onLongPress = {
                    if (onCover) {
                        calibrating = true
                    } else {
                        openCoverCalibration()
                    }
                },
                onLenses = { pair: LensPair ->
                    lensWrite?.cancel()
                    lensWrite = scope.launch {
                        delay(40)
                        app.settings.update { it.copy(lenses = pair) }
                    }
                },
                onCalibrationDone = {
                    calibrating = false
                    activity.intent.putExtra(MainActivity.EXTRA_CALIBRATE, false)
                    scope.launch { app.settings.update { it.copy(requestCalibration = false) } }
                },
            )
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
                onCalibrate = { openCoverCalibration() },
                onRequestLocation = {
                    locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
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
