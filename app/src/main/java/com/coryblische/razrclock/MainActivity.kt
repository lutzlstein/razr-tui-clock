package com.coryblische.razrclock

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSession
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.core.ExperimentalWindowApi
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.display.ComposeHostLifecycle
import com.coryblische.razrclock.display.CoverGeometry
import com.coryblische.razrclock.display.STAND_WINDOW_BRIGHTNESS
import com.coryblische.razrclock.display.attachComposeOwners
import com.coryblische.razrclock.posture.FoldSignal
import com.coryblische.razrclock.theme.RazrTheme
import com.coryblische.razrclock.ui.AppRoot
import com.coryblische.razrclock.ui.ClockScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor

@OptIn(ExperimentalWindowApi::class)
class MainActivity : ComponentActivity(), WindowAreaPresentationSessionCallback, WindowAreaSessionCallback {
    val openRequests = MutableStateFlow(0)
    private val rearFace = mutableStateOf<ClockFaceState?>(null)
    private var rearLifecycle = ComposeHostLifecycle()
    private var presentSession: WindowAreaSessionPresenter? = null
    private var transferSession: WindowAreaSession? = null
    private var rearAttempted = false
    private val windowAreas = WindowAreaController.getOrCreate()
    private val uiExecutor: Executor by lazy { ContextCompat.getMainExecutor(this) }

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        Log.i(RazrClockApp.TAG, "Notifications granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { info ->
                        val feature = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                        val signal = when (feature?.state) {
                            FoldingFeature.State.HALF_OPENED -> FoldSignal.HALF_OPENED
                            FoldingFeature.State.FLAT -> FoldSignal.FLAT
                            else -> FoldSignal.UNKNOWN
                        }
                        (application as RazrClockApp).postureMonitor.noteFolding(signal)
                    }
            }
        }
        setContent {
            AppRoot(activity = this)
        }
    }

    override fun onStart() {
        super.onStart()
        val app = application as RazrClockApp
        app.postureMonitor.acquire()
        app.batteryMonitor.acquire()
    }

    override fun onResume() {
        super.onResume()
        (application as RazrClockApp).mainResumed = true
    }

    override fun onPause() {
        (application as RazrClockApp).mainResumed = false
        super.onPause()
    }

    override fun onStop() {
        val app = application as RazrClockApp
        app.postureMonitor.release()
        app.batteryMonitor.release()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.wantsClock()) openRequests.value += 1
    }

    override fun onDestroy() {
        closeRearSession()
        super.onDestroy()
    }

    fun isOnCover(): Boolean {
        val current = display ?: return false
        if (current.displayId != Display.DEFAULT_DISPLAY) return true
        val mode = current.mode ?: return false
        return CoverGeometry.isCoverAspect(mode.physicalWidth.toFloat(), mode.physicalHeight.toFloat())
    }

    fun renderRear(state: ClockFaceState) {
        rearFace.value = state
    }

    fun presentOnRear() {
        if (rearAttempted || presentSession != null) return
        rearAttempted = true
        lifecycleScope.launch {
            val infos = withTimeoutOrNull(1_500) { windowAreas.windowAreaInfos.first() }
            val rear = infos?.firstOrNull { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING }
            if (rear == null) {
                report("No TYPE_REAR_FACING window area.")
                rearAttempted = false
                return@launch
            }
            val status = rear.getCapability(WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA).status
            report("Rear present status $status.")
            if (status == WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE) {
                windowAreas.presentContentOnWindowArea(
                    token = rear.token,
                    activity = this@MainActivity,
                    executor = uiExecutor,
                    windowAreaPresentationSessionCallback = this@MainActivity,
                )
            } else {
                rearAttempted = false
            }
        }
    }

    fun transferToRear() {
        lifecycleScope.launch {
            val infos = withTimeoutOrNull(1_500) { windowAreas.windowAreaInfos.first() }
            val rear = infos?.firstOrNull { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING }
            if (rear == null) {
                report("No TYPE_REAR_FACING window area.")
                return@launch
            }
            val operation = WindowAreaCapability.Operation.OPERATION_TRANSFER_ACTIVITY_TO_AREA
            val status = rear.getCapability(operation).status
            report("Rear transfer status $status.")
            when (status) {
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE ->
                    windowAreas.transferActivityToWindowArea(
                        token = rear.token,
                        activity = this@MainActivity,
                        executor = uiExecutor,
                        windowAreaSessionCallback = this@MainActivity,
                    )
                WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE ->
                    runCatching { rear.getActiveSession(operation)?.close() }
                        .onFailure { report("Could not close rear transfer: ${it.message}") }
                else -> Unit
            }
        }
    }

    fun closeRearSession() {
        rearAttempted = false
        val session = presentSession
        presentSession = null
        (application as RazrClockApp).rearSessionActive = false
        runCatching { session?.close() }
        rearLifecycle.destroy()
    }

    override fun onSessionStarted(session: WindowAreaSessionPresenter) {
        presentSession = session
        (application as RazrClockApp).rearSessionActive = true
        rearLifecycle = ComposeHostLifecycle()
        report("Rear presentation session started.")
        val app = application as RazrClockApp
        val view = ComposeView(session.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            attachComposeOwners(rearLifecycle)
            setContent {
                RazrTheme(palette = app.loadedPalette.palette) {
                    rearFace.value?.let { ClockScreen(state = it) }
                }
            }
        }
        rearLifecycle.create()
        session.setContentView(view)
        session.window?.let { window ->
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.attributes = window.attributes.apply { screenBrightness = STAND_WINDOW_BRIGHTNESS }
            WindowCompat.getInsetsController(window, view).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        view.attachComposeOwners(rearLifecycle)
        rearLifecycle.resume()
    }

    override fun onSessionStarted(session: WindowAreaSession) {
        transferSession = session
        (application as RazrClockApp).rearSessionActive = true
        report("Activity transferred to the rear display.")
    }

    override fun onSessionEnded(t: Throwable?) {
        presentSession = null
        transferSession = null
        rearAttempted = false
        (application as RazrClockApp).rearSessionActive = false
        rearLifecycle.destroy()
        rearLifecycle = ComposeHostLifecycle()
        val detail = t?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "closed"
        report("Rear display session ended ($detail).")
    }

    override fun onContainerVisibilityChanged(isVisible: Boolean) {
        report(if (isVisible) "Rear display container visible." else "Rear display container hidden.")
    }

    private fun report(message: String) {
        (application as RazrClockApp).cover.note(message)
    }

    companion object {
        const val EXTRA_FORCE_CLOCK = "force_clock"
        const val EXTRA_STAND = "stand"
    }
}

fun Intent?.wantsClock(): Boolean =
    this?.getBooleanExtra(MainActivity.EXTRA_FORCE_CLOCK, false) == true ||
        this?.getBooleanExtra(MainActivity.EXTRA_STAND, false) == true
