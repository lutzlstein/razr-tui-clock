package com.coryblische.razrclock.stand

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.coryblische.razrclock.RazrClockApp

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Log.i(RazrClockApp.TAG, "Rearming stand mode after $action")
        StandModeService.start(context)
    }
}
