package com.coryblische.razrclock.display

import android.app.Activity
import android.view.WindowManager

/** Dim enough for an OLED desk clock, still readable across a room. */
const val STAND_WINDOW_BRIGHTNESS = 0.12f

fun Activity.applyDeskWindow(active: Boolean) {
    val params = window.attributes
    if (active) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(false)
    }
    window.attributes = params
}
