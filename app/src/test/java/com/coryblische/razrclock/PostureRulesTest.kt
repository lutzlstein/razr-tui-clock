package com.coryblische.razrclock

import com.coryblische.razrclock.posture.BatteryStatus
import com.coryblische.razrclock.posture.DevicePosture
import com.coryblische.razrclock.posture.FoldSignal
import com.coryblische.razrclock.posture.PostureRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostureRulesTest {
    @Test
    fun hingeAngleBands() {
        assertEquals(DevicePosture.CLOSED, PostureRules.fromHingeAngle(0f))
        assertEquals(DevicePosture.CLOSED, PostureRules.fromHingeAngle(19.9f))
        assertEquals(DevicePosture.HALF_OPENED, PostureRules.fromHingeAngle(20f))
        assertEquals(DevicePosture.HALF_OPENED, PostureRules.fromHingeAngle(92f))
        assertEquals(DevicePosture.HALF_OPENED, PostureRules.fromHingeAngle(160f))
        assertEquals(DevicePosture.OPEN, PostureRules.fromHingeAngle(160.1f))
        assertEquals(DevicePosture.OPEN, PostureRules.fromHingeAngle(180f))
        assertEquals(DevicePosture.UNKNOWN, PostureRules.fromHingeAngle(null))
    }

    @Test
    fun angleBeatsVendorAndFolding() {
        assertEquals(
            DevicePosture.OPEN,
            PostureRules.merge(angle = 180f, vendor = 2f, folding = FoldSignal.HALF_OPENED),
        )
        assertEquals(
            DevicePosture.HALF_OPENED,
            PostureRules.merge(angle = null, vendor = 3f, folding = FoldSignal.HALF_OPENED),
        )
        assertEquals(
            DevicePosture.HALF_OPENED,
            PostureRules.merge(angle = null, vendor = 5f, folding = FoldSignal.UNKNOWN),
        )
        assertEquals(
            DevicePosture.OPEN,
            PostureRules.merge(angle = null, vendor = null, folding = FoldSignal.FLAT),
        )
        assertEquals(null, PostureRules.fromVendorPosture(4f))
    }

    @Test
    fun standModeGate() {
        assertTrue(
            PostureRules.shouldTakeOver(
                DevicePosture.HALF_OPENED,
                standEnabled = true,
                onlyWhileCharging = false,
                batteryStatus = BatteryStatus.DISCHARGING,
            ),
        )
        assertFalse(
            PostureRules.shouldTakeOver(
                DevicePosture.HALF_OPENED,
                standEnabled = true,
                onlyWhileCharging = true,
                batteryStatus = BatteryStatus.DISCHARGING,
            ),
        )
        assertTrue(
            PostureRules.shouldTakeOver(
                DevicePosture.HALF_OPENED,
                standEnabled = true,
                onlyWhileCharging = true,
                batteryStatus = BatteryStatus.FULL,
            ),
        )
        assertFalse(
            PostureRules.shouldTakeOver(
                DevicePosture.OPEN,
                standEnabled = true,
                onlyWhileCharging = false,
                batteryStatus = BatteryStatus.CHARGING,
            ),
        )
        assertFalse(
            PostureRules.shouldTakeOver(
                DevicePosture.HALF_OPENED,
                standEnabled = false,
                onlyWhileCharging = false,
                batteryStatus = BatteryStatus.CHARGING,
            ),
        )
    }
}
