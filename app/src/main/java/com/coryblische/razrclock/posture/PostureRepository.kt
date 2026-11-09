package com.coryblische.razrclock.posture

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.coryblische.razrclock.RazrClockApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PostureSnapshot(
    val posture: DevicePosture = DevicePosture.UNKNOWN,
    val hingeAngle: Float? = null,
    val vendorRaw: Float? = null,
    val folding: FoldSignal = FoldSignal.UNKNOWN,
    val hingePresent: Boolean = false,
    val vendorPresent: Boolean = false,
)

class PostureRepository(context: Context) {
    private val sensorManager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val hingeSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE, true)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_HINGE_ANGLE)
    private val vendorSensor: Sensor? = sensorManager?.getSensorList(Sensor.TYPE_ALL)
        ?.firstOrNull { it.stringType == VENDOR_POSTURE }

    private val angle = MutableStateFlow<Float?>(null)
    private val vendor = MutableStateFlow<Float?>(null)
    private val folding = MutableStateFlow(FoldSignal.UNKNOWN)

    private val _snapshot = MutableStateFlow(
        PostureSnapshot(
            hingePresent = hingeSensor != null,
            vendorPresent = vendorSensor != null,
        ),
    )
    val snapshot: StateFlow<PostureSnapshot> = _snapshot.asStateFlow()

    private var refs = 0
    private var running = false

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val value = event.values.firstOrNull() ?: return
            when {
                event.sensor.type == Sensor.TYPE_HINGE_ANGLE -> angle.value = value
                event.sensor.stringType == VENDOR_POSTURE -> vendor.value = value
            }
            publish()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    fun acquire() {
        synchronized(this) {
            refs += 1
            if (refs == 1) start()
        }
    }

    fun release() {
        synchronized(this) {
            if (refs == 0) return
            refs -= 1
            if (refs == 0) stop()
        }
    }

    fun noteFolding(signal: FoldSignal) {
        folding.value = signal
        publish()
    }

    private fun start() {
        if (running || sensorManager == null) return
        running = true
        hingeSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.i(RazrClockApp.TAG, "Hinge angle sensor ${it.name} wakeup=${it.isWakeUpSensor}")
        } ?: Log.i(RazrClockApp.TAG, "No android.sensor.hinge_angle on this device")
        vendorSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
            Log.i(RazrClockApp.TAG, "Vendor posture sensor ${it.stringType}")
        }
        publish()
    }

    private fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(listener)
    }

    private fun publish() {
        val degrees = angle.value
        val raw = vendor.value
        val fold = folding.value
        _snapshot.value = PostureSnapshot(
            posture = PostureRules.merge(degrees, raw, fold),
            hingeAngle = degrees,
            vendorRaw = raw,
            folding = fold,
            hingePresent = hingeSensor != null,
            vendorPresent = vendorSensor != null,
        )
    }

    companion object {
        const val VENDOR_POSTURE = "com.motorola.sensor.hinge_posture"
    }
}
