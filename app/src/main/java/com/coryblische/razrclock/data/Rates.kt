package com.coryblische.razrclock.data

import java.util.Locale

fun formatRate(bytesPerSec: Double): String {
    if (bytesPerSec < 0.0) return "--"
    val v = bytesPerSec
    return when {
        v >= 1_000_000.0 -> String.format(Locale.US, "%.1f MB/s", v / 1_000_000.0)
        v >= 1_000.0 -> String.format(Locale.US, "%.0f KB/s", v / 1_000.0)
        else -> String.format(Locale.US, "%.0f B/s", v)
    }
}

fun formatSysmon(downBytesPerSec: Double, upBytesPerSec: Double, ramPercent: Int?): String {
    val ram = ramPercent?.let { "   ram $it%" }.orEmpty()
    return "↓ ${formatRate(downBytesPerSec)}   ↑ ${formatRate(upBytesPerSec)}$ram"
}
