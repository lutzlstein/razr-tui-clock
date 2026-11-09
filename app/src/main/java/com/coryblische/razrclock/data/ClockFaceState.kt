package com.coryblische.razrclock.data

import com.coryblische.razrclock.posture.BatteryStatus
import com.coryblische.razrclock.posture.DevicePosture
import java.time.LocalDateTime

data class ClockFaceState(
    val now: LocalDateTime,
    val preferences: ClockPreferences,
    val batteryPercent: Int,
    val batteryStatus: BatteryStatus,
    val posture: DevicePosture,
    val hingeAngleDegrees: Float?,
    val cursorVisible: Boolean = true,
    val standActive: Boolean = false,
) {
    companion object {
        fun sample(): ClockFaceState = ClockFaceState(
            now = LocalDateTime.of(2026, 11, 9, 21, 34, 7),
            preferences = ClockPreferences(),
            batteryPercent = 84,
            batteryStatus = BatteryStatus.CHARGING,
            posture = DevicePosture.HALF_OPENED,
            hingeAngleDegrees = 92f,
            cursorVisible = true,
            standActive = true,
        )
    }
}
