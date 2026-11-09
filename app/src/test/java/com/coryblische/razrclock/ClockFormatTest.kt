package com.coryblische.razrclock

import com.coryblische.razrclock.data.ClockFaceState
import com.coryblische.razrclock.data.formatDate
import com.coryblische.razrclock.data.formatDayPeriod
import com.coryblische.razrclock.data.formatHoursMinutes
import com.coryblische.razrclock.data.formatSeconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClockFormatTest {
    @Test
    fun sampleIsMondayEvening() {
        val state = ClockFaceState.sample()
        assertEquals("21:34", formatHoursMinutes(state))
        assertEquals("07", formatSeconds(state))
        assertEquals("MON 09 NOV 2026", formatDate(state))
        assertNull(formatDayPeriod(state))
    }

    @Test
    fun twelveHourDropsTheLeadingZero() {
        val state = ClockFaceState.sample().let {
            it.copy(preferences = it.preferences.copy(use24Hour = false))
        }
        assertEquals("9:34", formatHoursMinutes(state))
        assertEquals("PM", formatDayPeriod(state))
    }
}
