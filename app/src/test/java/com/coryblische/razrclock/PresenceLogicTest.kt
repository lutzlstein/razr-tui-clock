package com.coryblische.razrclock

import com.coryblische.razrclock.presence.CoverView
import com.coryblische.razrclock.presence.PresenceLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceLogicTest {
    private val logic = PresenceLogic()

    @Test
    fun approachWakesAndSpinsOnce() {
        val near = logic.onNear(0L)
        assertTrue(near.active)
        assertTrue(near.near)
        assertEquals(1, near.spin)
        assertEquals(CoverView.CLOCK, near.view)
        assertEquals(null, near.idleAt)
        val held = logic.onNear(500L)
        assertEquals(1, held.spin)
    }

    @Test
    fun farSchedulesIdleAndTimeoutReturnsToDim() {
        logic.onNear(0L)
        val far = logic.onFar(100L)
        assertFalse(far.near)
        assertEquals(10_100L, far.idleAt)
        assertTrue(logic.onTimeout(10_099L).active)
        val idle = logic.onTimeout(10_100L)
        assertFalse(idle.active)
        assertEquals(null, idle.idleAt)
    }

    @Test
    fun waveWithinOneSecondCyclesTheView() {
        logic.onNear(0L)
        logic.onFar(200L)
        val wave = logic.onNear(700L)
        assertEquals(CoverView.WEATHER, wave.view)
        assertEquals(2, wave.spin)
        assertTrue(wave.active)
        logic.onFar(800L)
        val again = logic.onNear(1_200L)
        assertEquals(CoverView.SYSMON, again.view)
        logic.onFar(1_300L)
        assertEquals(CoverView.CLOCK, logic.onNear(1_600L).view)
    }

    @Test
    fun slowReturnIsNotAWave() {
        logic.onNear(0L)
        logic.onFar(200L)
        val late = logic.onNear(1_201L)
        assertEquals(CoverView.CLOCK, late.view)
        assertEquals(1, late.spin)
    }

    @Test
    fun bounceInsideTheDebounceWindowIsIgnored() {
        logic.onNear(0L)
        val bounced = logic.onFar(40L)
        assertTrue(bounced.near)
        assertEquals(null, bounced.idleAt)
        val far = logic.onFar(200L)
        assertFalse(far.near)
    }

    @Test
    fun gestureCyclesAndASecondPulseInsideTheCooldownDoesNot() {
        val first = logic.onGesture(0L)
        assertEquals(CoverView.WEATHER, first.view)
        assertTrue(first.active)
        assertEquals(1, first.spin)
        val second = logic.onGesture(200L)
        assertEquals(CoverView.WEATHER, second.view)
        assertEquals(10_200L, second.idleAt)
        val third = logic.onGesture(800L)
        assertEquals(CoverView.SYSMON, third.view)
    }
}
