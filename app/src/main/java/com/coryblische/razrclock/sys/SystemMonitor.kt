package com.coryblische.razrclock.sys

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TrafficRates(
    val downBytesPerSec: Double = 0.0,
    val upBytesPerSec: Double = 0.0,
)

class SystemMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var lastRx = TrafficStats.UNSUPPORTED.toLong()
    private var lastTx = TrafficStats.UNSUPPORTED.toLong()
    private var lastMs = 0L

    private val _rates = MutableStateFlow(TrafficRates())
    val rates: StateFlow<TrafficRates> = _rates.asStateFlow()

    private val _ramPercent = MutableStateFlow<Int?>(null)
    val ramPercent: StateFlow<Int?> = _ramPercent.asStateFlow()

    fun start() {
        if (job != null) return
        job = scope.launch {
            while (isActive) {
                sample()
                delay(1_000L)
            }
        }
    }

    fun sample() {
        val now = SystemClock.elapsedRealtime()
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx != TrafficStats.UNSUPPORTED.toLong() && lastRx >= 0L && lastMs > 0L) {
            val dt = (now - lastMs).coerceAtLeast(1L)
            _rates.value = TrafficRates(
                downBytesPerSec = (rx - lastRx).coerceAtLeast(0L) * 1000.0 / dt,
                upBytesPerSec = (tx - lastTx).coerceAtLeast(0L) * 1000.0 / dt,
            )
        }
        if (rx != TrafficStats.UNSUPPORTED.toLong()) {
            lastRx = rx
            lastTx = tx
            lastMs = now
        } else {
            _rates.value = TrafficRates(downBytesPerSec = -1.0, upBytesPerSec = -1.0)
        }
        _ramPercent.value = readRamPercent()
    }

    private fun readRamPercent(): Int? {
        val manager = appContext.getSystemService(ActivityManager::class.java) ?: return null
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        if (info.totalMem <= 0L) return null
        val used = (info.totalMem - info.availMem).coerceAtLeast(0L)
        return ((used * 100L) / info.totalMem).toInt().coerceIn(0, 100)
    }
}
