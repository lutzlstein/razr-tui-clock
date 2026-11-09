package com.coryblische.razrclock.data

import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE dd MMM yyyy", Locale.US)

fun formatHoursMinutes(state: ClockFaceState): String {
    val time = state.now
    return if (state.preferences.use24Hour) {
        "%02d:%02d".format(Locale.US, time.hour, time.minute)
    } else {
        val hour = time.hour % 12
        "%d:%02d".format(Locale.US, if (hour == 0) 12 else hour, time.minute)
    }
}

fun formatDayPeriod(state: ClockFaceState): String? =
    if (state.preferences.use24Hour) {
        null
    } else if (state.now.hour < 12) {
        "AM"
    } else {
        "PM"
    }

fun formatSeconds(state: ClockFaceState): String? =
    if (state.preferences.showSeconds) "%02d".format(Locale.US, state.now.second) else null

fun formatDate(state: ClockFaceState): String =
    state.now.format(dateFormatter).uppercase(Locale.US)

fun accessibleClockLabel(state: ClockFaceState): String = buildString {
    append(formatHoursMinutes(state))
    formatSeconds(state)?.let {
        append(':')
        append(it)
    }
    formatDayPeriod(state)?.let {
        append(' ')
        append(it)
    }
    if (state.preferences.showDate) {
        append(", ")
        append(formatDate(state))
    }
    if (state.preferences.showBattery) {
        append(", battery ")
        if (state.batteryPercent < 0) append("unknown") else append("${state.batteryPercent} percent")
        append(", ")
        append(state.batteryStatus.name.lowercase(Locale.US))
    }
}
