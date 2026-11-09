package com.coryblische.razrclock.posture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatterySnapshot(
    val percent: Int = -1,
    val status: BatteryStatus = BatteryStatus.UNKNOWN,
)

class BatteryRepository(context: Context) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(readSticky())
    val state: StateFlow<BatterySnapshot> = _state.asStateFlow()

    private var refs = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            _state.value = fromIntent(intent)
        }
    }

    fun acquire() {
        synchronized(this) {
            refs += 1
            if (refs != 1) return
            _state.value = readSticky()
            appContext.registerReceiver(
                receiver,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                Context.RECEIVER_NOT_EXPORTED,
            )
        }
    }

    fun release() {
        synchronized(this) {
            if (refs == 0) return
            refs -= 1
            if (refs == 0) {
                runCatching { appContext.unregisterReceiver(receiver) }
            }
        }
    }

    private fun readSticky(): BatterySnapshot {
        val sticky = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return fromIntent(sticky)
    }

    companion object {
        fun fromIntent(intent: Intent?): BatterySnapshot {
            if (intent == null) return BatterySnapshot()
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val percent = if (level < 0) -1 else (level * 100) / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            val mapped = when {
                status == BatteryManager.BATTERY_STATUS_FULL -> BatteryStatus.FULL
                status == BatteryManager.BATTERY_STATUS_CHARGING || plugged -> BatteryStatus.CHARGING
                status == BatteryManager.BATTERY_STATUS_DISCHARGING ||
                    status == BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryStatus.DISCHARGING
                else -> BatteryStatus.UNKNOWN
            }
            return BatterySnapshot(percent, mapped)
        }
    }
}
