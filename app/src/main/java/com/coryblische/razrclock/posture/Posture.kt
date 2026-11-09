package com.coryblische.razrclock.posture

enum class DevicePosture {
    CLOSED,
    HALF_OPENED,
    OPEN,
    UNKNOWN,
}

enum class FoldSignal {
    UNKNOWN,
    HALF_OPENED,
    FLAT,
}

enum class BatteryStatus {
    CHARGING,
    FULL,
    DISCHARGING,
    UNKNOWN,
}

object PostureRules {
    /** Below this hinge angle the flip is treated as closed. */
    const val CLOSED_BELOW_DEGREES = 20f

    /** Above this hinge angle the flip is treated as fully open. */
    const val OPEN_ABOVE_DEGREES = 160f

    fun fromHingeAngle(degrees: Float?): DevicePosture {
        if (degrees == null || degrees.isNaN()) return DevicePosture.UNKNOWN
        return when {
            degrees < CLOSED_BELOW_DEGREES -> DevicePosture.CLOSED
            degrees > OPEN_ABOVE_DEGREES -> DevicePosture.OPEN
            else -> DevicePosture.HALF_OPENED
        }
    }

    /**
     * `com.motorola.sensor.hinge_posture` (vendor type 65583) has no public enum.
     * Goldfish and other hinge HALs use small integers: 1 closed, 2 half-open,
     * 3 open, 5 tent. This mapping is only used when the angle sensor is absent.
     * Returns null for codes we refuse to guess.
     */
    fun fromVendorPosture(raw: Float?): DevicePosture? {
        if (raw == null || raw.isNaN()) return null
        return when (raw.toInt()) {
            1 -> DevicePosture.CLOSED
            2, 5 -> DevicePosture.HALF_OPENED
            3 -> DevicePosture.OPEN
            else -> null
        }
    }

    fun merge(angle: Float?, vendor: Float?, folding: FoldSignal): DevicePosture {
        val fromAngle = fromHingeAngle(angle)
        if (fromAngle != DevicePosture.UNKNOWN) return fromAngle
        if (folding == FoldSignal.HALF_OPENED) return DevicePosture.HALF_OPENED
        fromVendorPosture(vendor)?.let { return it }
        if (folding == FoldSignal.FLAT) return DevicePosture.OPEN
        return DevicePosture.UNKNOWN
    }

    fun isSupplyingPower(status: BatteryStatus): Boolean =
        status == BatteryStatus.CHARGING || status == BatteryStatus.FULL

    fun shouldTakeOver(
        posture: DevicePosture,
        standEnabled: Boolean,
        onlyWhileCharging: Boolean,
        batteryStatus: BatteryStatus,
    ): Boolean {
        if (!standEnabled) return false
        if (posture != DevicePosture.HALF_OPENED) return false
        if (onlyWhileCharging && !isSupplyingPower(batteryStatus)) return false
        return true
    }
}

fun DevicePosture.label(): String = when (this) {
    DevicePosture.CLOSED -> "closed"
    DevicePosture.HALF_OPENED -> "half-open"
    DevicePosture.OPEN -> "open"
    DevicePosture.UNKNOWN -> "unknown"
}
