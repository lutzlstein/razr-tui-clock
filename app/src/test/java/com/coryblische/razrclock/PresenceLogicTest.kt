package com.coryblische.razrclock

import com.coryblische.razrclock.presence.CoverView
import com.coryblische.razrclock.presence.PresenceLogic
import com.coryblische.razrclock.presence.SensorRole
import com.coryblische.razrclock.presence.sensorRole
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
    fun wakeDoesNotChangeTheView() {
        val woke = logic.onWake(0L)
        assertTrue(woke.active)
        assertEquals(CoverView.CLOCK, woke.view)
        assertEquals(1, woke.spin)
        val again = logic.onWake(500L)
        assertEquals(1, again.spin)
        assertEquals(CoverView.CLOCK, again.view)
    }

    @Test
    fun doubleTapCyclesWithoutWaking() {
        val first = logic.onCycle(0L)
        assertEquals(CoverView.WEATHER, first.view)
        assertFalse(first.active)
        assertEquals(0, first.spin)
        val bounced = logic.onCycle(200L)
        assertEquals(CoverView.WEATHER, bounced.view)
        val next = logic.onCycle(800L)
        assertEquals(CoverView.SYSMON, next.view)
        assertFalse(next.active)
    }
}

class SensorRoleTest {
    @Test
    fun liveRazrSensors() {
        assertEquals(SensorRole.APPROACH, sensorRole("com.motorola.sensor.approach_prox Approach prox"))
        assertEquals(SensorRole.APPROACH, sensorRole("com.motorola.sensor.fip_approach fip_approach"))
        assertEquals(SensorRole.WAKE, sensorRole("com.motorola.sensor.camera_activate camera_activate gesture"))
        assertEquals(SensorRole.WAKE, sensorRole("com.motorola.sensor.glance Glance"))
        assertEquals(SensorRole.CYCLE, sensorRole("com.motorola.sensor.double_tap Double Tap Gesture"))
        assertEquals(SensorRole.CYCLE, sensorRole("com.motorola.sensor.double_tap_2 doubletap"))
        assertEquals(null, sensorRole("android.sensor.proximity Proximity"))
    }
}
