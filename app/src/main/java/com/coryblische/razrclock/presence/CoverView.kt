package com.coryblische.razrclock.presence

/** How a registered Motorola sensor is allowed to affect the clock. */
enum class SensorRole {
    /** approach_prox and fip_approach: near wakes, far starts the idle timer. */
    APPROACH,

    /** glance and camera_activate: wake only, never change the view. */
    WAKE,

    /** double_tap: cycle clock / forecast / sysmon, not a hand over the glass. */
    CYCLE,
}

/**
 * Classify a sensor from its string type and name. Double-tap wins over the
 * other hints so a "double tap gesture" is not treated as presence.
 */
fun sensorRole(key: String): SensorRole? {
    val text = key.lowercase()
    if ("double_tap" in text || "doubletap" in text) return SensorRole.CYCLE
    if ("camera_activate" in text || "glance" in text) return SensorRole.WAKE
    if ("approach" in text) return SensorRole.APPROACH
    return null
}

enum class CoverView {
    CLOCK,
    WEATHER,
    SYSMON,
    ;

    fun next(): CoverView = when (this) {
        CLOCK -> WEATHER
        WEATHER -> SYSMON
        SYSMON -> CLOCK
    }
}
