package com.coryblische.razrclock.data

data class ClockPreferences(
    val use24Hour: Boolean = true,
    val showSeconds: Boolean = true,
    val showDate: Boolean = true,
    val showBattery: Boolean = true,
    val standMode: Boolean = true,
    val standOnlyWhileCharging: Boolean = false,
    val useFahrenheit: Boolean = true,
    val useDeviceLocation: Boolean = false,
    val latitude: Float = NEW_YORK_LATITUDE,
    val longitude: Float = NEW_YORK_LONGITUDE,
    val locationLabel: String = "New York",
    val lenses: LensPair = LensPair(),
    val requestCalibration: Boolean = false,
    /** White alignment grid. Default on for 1.2.1-align only; next release should default false. */
    val lensAlign: Boolean = true,
) {
    companion object {
        const val NEW_YORK_LATITUDE = 40.7128f
        const val NEW_YORK_LONGITUDE = -74.0060f
    }
}
