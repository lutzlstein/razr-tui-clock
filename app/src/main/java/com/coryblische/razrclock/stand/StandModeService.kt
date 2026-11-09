package com.coryblische.razrclock.stand

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.coryblische.razrclock.R
import com.coryblische.razrclock.RazrClockApp
import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.display.CoverDisplayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class StandModeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var commandJob: Job? = null
    private var tickJob: Job? = null
    private var latched = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as RazrClockApp
        app.postureMonitor.acquire()
        app.batteryMonitor.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannels()
        startInForeground(active = false)
        val app = application as RazrClockApp
        commandJob?.cancel()
        commandJob = scope.launch {
            app.settings.loaded.first { it }
            if (!app.settings.preferences.value.standMode) {
                stopSelf()
                return@launch
            }
            combine(
                app.settings.preferences,
                app.postureMonitor.snapshot,
                app.batteryMonitor.state,
            ) { prefs, posture, battery ->
                Triple(prefs, posture, battery)
            }.collectLatest { (prefs, posture, battery) ->
                if (!prefs.standMode) {
                    stopSelf()
                    return@collectLatest
                }
                val stand = com.coryblische.razrclock.posture.PostureRules.shouldTakeOver(
                    posture.posture,
                    prefs.standMode,
                    prefs.standOnlyWhileCharging,
                    battery.status,
                )
                val state = app.face(LocalDateTime.now())
                if (stand && !latched) {
                    latched = true
                    enter(state)
                } else if (!stand && latched) {
                    latched = false
                    exit()
                } else if (stand) {
                    app.cover.ensurePresentation(this@StandModeService, state)
                }
            }
        }
        return START_STICKY
    }

    private fun enter(state: ClockFaceState) {
        val app = application as RazrClockApp
        Log.i(RazrClockApp.TAG, "Stand posture entered")
        wakeBriefly()
        startInForeground(active = true)
        tickJob?.cancel()
        tickJob = scope.launch {
            if (app.mainResumed) delay(600)
            if (!app.rearSessionActive) {
                app.cover.resetFailures()
                app.cover.ensurePresentation(this@StandModeService, app.face())
            }
            if (!app.cover.isPresentationShowing() && !app.rearSessionActive && !app.mainResumed) {
                val display = app.cover.preferredCover()
                val launched = display?.let { app.cover.launchOnDisplay(this@StandModeService, it).isSuccess } == true
                if (!launched) raiseAlert()
            }
            while (latched) {
                val latest = app.face()
                if (!app.rearSessionActive) {
                    app.cover.ensurePresentation(this@StandModeService, latest)
                }
                val wait = 1_000L - (System.currentTimeMillis() % 1_000L)
                delay(wait.coerceIn(200L, 1_000L))
            }
        }
    }

    private fun exit() {
        Log.i(RazrClockApp.TAG, "Stand posture left")
        tickJob?.cancel()
        val app = application as RazrClockApp
        app.cover.dismissPresentation()
        app.cover.resetFailures()
        val manager = getSystemService(NotificationManager::class.java)
        manager.cancel(ALERT_ID)
        startInForeground(active = false)
    }

    private fun wakeBriefly() {
        val power = getSystemService(PowerManager::class.java) ?: return
        runCatching {
            @Suppress("DEPRECATION")
            val lock = power.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "razrclock:stand",
            )
            lock.acquire(3_000L)
        }.onFailure { Log.i(RazrClockApp.TAG, "Screen wake not granted: ${it.message}") }
    }

    private fun raiseAlert() {
        val app = application as RazrClockApp
        val manager = getSystemService(NotificationManager::class.java)
        val pending = CoverDisplayController.fullScreenPendingIntent(this)
        val builder = NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentTitle(getString(R.string.notif_active_title))
            .setContentText(getString(R.string.notif_active_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pending)
            .setAutoCancel(true)
        if (manager.canUseFullScreenIntent()) {
            builder.setFullScreenIntent(pending, true)
        }
        manager.notify(ALERT_ID, builder.build())
        Log.i(RazrClockApp.TAG, "Posted desk-clock alert. fullScreen=${manager.canUseFullScreenIntent()}")
    }

    private fun startInForeground(active: Boolean) {
        val notification = if (active) {
            ongoing(getString(R.string.notif_active_title), getString(R.string.notif_active_text))
        } else {
            ongoing(getString(R.string.notif_armed_title), getString(R.string.notif_armed_text))
        }
        ServiceCompat.startForeground(
            this,
            ONGOING_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    private fun ongoing(title: String, text: String): Notification {
        val pending = CoverDisplayController.fullScreenPendingIntent(this)
        return NotificationCompat.Builder(this, ONGOING_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val ongoing = NotificationChannel(
            ONGOING_CHANNEL,
            getString(R.string.notif_channel_stand),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.notif_channel_stand_desc) }
        val alert = NotificationChannel(
            ALERT_CHANNEL,
            getString(R.string.notif_channel_alert),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = getString(R.string.notif_channel_alert_desc) }
        manager.createNotificationChannel(ongoing)
        manager.createNotificationChannel(alert)
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        val app = application as RazrClockApp
        app.cover.dismissPresentation()
        app.postureMonitor.release()
        app.batteryMonitor.release()
        super.onDestroy()
    }

    companion object {
        private const val ONGOING_ID = 1001
        private const val ALERT_ID = 1002
        private const val ONGOING_CHANNEL = "stand_mode"
        private const val ALERT_CHANNEL = "stand_alert"

        fun start(context: Context) {
            val intent = Intent(context, StandModeService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (error: RuntimeException) {
                Log.w(RazrClockApp.TAG, "Stand service not started", error)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StandModeService::class.java))
        }
    }
}
