package com.coryblische.razrclock.data

data class ClockPreferences(
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val standMode: Boolean = true,
    val standOnlyWhileCharging: Boolean = false,
)
