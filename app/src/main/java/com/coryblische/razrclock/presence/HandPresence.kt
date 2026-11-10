package com.coryblische.razrclock.presence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.coryblische.razrclock.RazrClockApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Public [SensorManager] listeners for a hand near the cover.
 *
 * Listens only while a clock surface is up and the screen is on.
 * The inner proximity is left unused when a cover, CLI, or approach
 * sensor is also readable, because that inner sensor reports near
 * while the phone is folded.
 */
class HandPresence(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val power = appContext.getSystemService(PowerManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val logic = PresenceLogic()

    private val _cue = MutableStateFlow(HandCue(available = catalog()))
    val cue: StateFlow<HandCue> = _cue.asStateFlow()

    private var enabled = false
    private var surfaces = 0
    private var receiverRegistered = false
    private var listening = false
    private val nearBySensor = HashMap<Int, Boolean>()
    private val roles = HashMap<Int, SensorRole>()
    private val pulsed = HashSet<Int>()
    private var idleJob: Job? = null
    private var brightJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var flashUntil = 0L
    private val clearFlash = Runnable {
        if (SystemClock.elapsedRealtime() >= flashUntil) {
            _cue.value = _cue.value.copy(flash = false)
        }
    }

    init {
        Log.i(RazrClockApp.TAG, "Sensors:\n${_cue.value.available}")
    }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val value = event.values.firstOrNull() ?: return
            val now = SystemClock.elapsedRealtime()
            when (roles[event.sensor.id]) {
                SensorRole.CYCLE -> {
                    if (rising(event.sensor.id, value)) {
                        publish(logic.onCycle(now))
                        flash()
                    }
                    return
                }
                SensorRole.WAKE -> {
                    if (rising(event.sensor.id, value)) {
                        publish(logic.onWake(now))
                        flash()
                    }
                    return
                }
                SensorRole.APPROACH, null -> Unit
            }
            val near = isNear(event.sensor, value)
            val id = event.sensor.id
            val was = nearBySensor[id]
            if (was == near) return
            nearBySensor[id] = near
            val anyNear = nearBySensor.values.any { it }
            val wasNear = logic.state.near
            if (anyNear) {
                publish(logic.onNear(now))
                if (!wasNear) flash()
            } else {
                publish(logic.onFar(now))
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Log.i(RazrClockApp.TAG, "Screen off; proximity listeners unregistered")
                    stopSensors()
                }
                Intent.ACTION_SCREEN_ON -> reconcile()
            }
        }
    }

    fun setEnabled(on: Boolean) {
        enabled = on
        if (!on) {
            idleJob?.cancel()
            brightJob?.cancel()
            mainHandler.removeCallbacks(clearFlash)
            reconcile()
            val next = logic.asleep()
            _cue.value = _cue.value.copy(
                enabled = false,
                active = false,
                view = next.view,
                spin = next.spin,
                brightness = IDLE_BRIGHTNESS,
                flash = false,
            )
            return
        }
        _cue.value = _cue.value.copy(enabled = true)
        reconcile()
    }

    fun acquire() {
        surfaces += 1
        reconcile()
    }

    fun release() {
        if (surfaces == 0) return
        surfaces -= 1
        reconcile()
    }

    private fun reconcile() {
        val wantWatch = enabled && surfaces > 0
        if (wantWatch && !receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
            appContext.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
        if (!wantWatch && receiverRegistered) {
            runCatching { appContext.unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        val interactive = power?.isInteractive != false
        if (wantWatch && interactive) startSensors() else stopSensors()
    }

    private fun startSensors() {
        if (listening || sensorManager == null) return
        listening = true
        val all = sensorManager.getSensorList(Sensor.TYPE_ALL).orEmpty()
        val chosen = choose(all)
        val using = ArrayList<String>()
        val skipped = ArrayList<String>()
        roles.clear()
        pulsed.clear()
        nearBySensor.clear()
        for (pick in chosen) {
            val registered = runCatching {
                sensorManager.registerListener(listener, pick.sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }.getOrElse { error ->
                skipped += "${label(pick.sensor)} ${error.javaClass.simpleName}"
                false
            }
            if (!registered) {
                if (skipped.none { it.startsWith(label(pick.sensor)) }) skipped += "${label(pick.sensor)} not accepted"
                continue
            }
            roles[pick.sensor.id] = pick.role
            using += "${label(pick.sensor)} ${pick.role.name.lowercase()}"
            Log.i(
                RazrClockApp.TAG,
                "Hand presence using ${label(pick.sensor)} role=${pick.role} " +
                    "wakeup=${pick.sensor.isWakeUpSensor} range=${pick.sensor.maximumRange}",
            )
        }
        if (using.isEmpty()) {
            Log.i(RazrClockApp.TAG, "Hand presence found no readable proximity or gesture sensor")
        }
        if (skipped.isNotEmpty()) {
            Log.i(RazrClockApp.TAG, "Hand presence skipped ${skipped.joinToString()}")
        }
        _cue.value = _cue.value.copy(
            enabled = true,
            using = if (using.isEmpty()) "none readable" else using.joinToString(),
            available = catalog(all),
        )
    }

    private fun stopSensors() {
        if (!listening) return
        listening = false
        sensorManager?.unregisterListener(listener)
        nearBySensor.clear()
        roles.clear()
        pulsed.clear()
        idleJob?.cancel()
        brightJob?.cancel()
        mainHandler.removeCallbacks(clearFlash)
        val next = logic.asleep()
        _cue.value = _cue.value.copy(
            active = next.active,
            view = next.view,
            spin = next.spin,
            brightness = IDLE_BRIGHTNESS,
            flash = false,
        )
        Log.i(RazrClockApp.TAG, "Hand presence listeners stopped")
    }

    private fun flash() {
        flashUntil = SystemClock.elapsedRealtime() + FLASH_MS
        _cue.value = _cue.value.copy(flash = true)
        mainHandler.removeCallbacks(clearFlash)
        mainHandler.postAtTime(clearFlash, SystemClock.uptimeMillis() + FLASH_MS)
    }

    private fun publish(next: PresenceLogic.State) {
        val wasActive = _cue.value.active
        _cue.value = _cue.value.copy(
            enabled = enabled,
            active = next.active,
            view = next.view,
            spin = next.spin,
        )
        if (wasActive != next.active) {
            animateBrightness(if (next.active) AWAKE_BRIGHTNESS else IDLE_BRIGHTNESS)
        }
        idleJob?.cancel()
        val due = next.idleAt
        if (due != null && !next.near) {
            val wait = due - SystemClock.elapsedRealtime()
            idleJob = scope.launch {
                delay(wait.coerceAtLeast(0L))
                publish(logic.onTimeout(SystemClock.elapsedRealtime()))
            }
        }
    }

    private fun animateBrightness(target: Float) {
        brightJob?.cancel()
        val start = _cue.value.brightness
        if (start == target) return
        val rising = target > start
        val frames = if (rising) 8 else 16
        brightJob = scope.launch {
            repeat(frames) { step ->
                val t = (step + 1) / frames.toFloat()
                val eased = t * t * (3f - 2f * t)
                _cue.value = _cue.value.copy(brightness = start + (target - start) * eased)
                delay(if (rising) 28L else 32L)
            }
        }
    }

    private fun catalog(sensors: List<Sensor> = sensorManager?.getSensorList(Sensor.TYPE_ALL).orEmpty()): String {
        if (sensors.isEmpty()) return "no sensors"
        return sensors.joinToString("\n") { sensor ->
            "${sensor.name}  ${sensor.stringType}  type=${sensor.type}  wakeup=${sensor.isWakeUpSensor}"
        }
    }

    private data class Pick(val sensor: Sensor, val role: SensorRole)

    private fun choose(all: List<Sensor>): List<Pick> {
        val tagged = all.map { sensor -> sensor to (sensorRole(key(sensor)) ?: proximityFallback(sensor)) }
        val cycles = preferNonWake(tagged.filter { it.second == SensorRole.CYCLE }.map { it.first })
        val wakes = preferNonWake(tagged.filter { it.second == SensorRole.WAKE }.map { it.first })
        val approaches = preferNonWake(tagged.filter { it.second == SensorRole.APPROACH }.map { it.first })
        val cover = approaches.filter { isCoverSide(it) || sensorRole(key(it)) == SensorRole.APPROACH }
        val chosenApproach = if (cover.isNotEmpty()) cover else approaches.take(1)
        if (cover.isNotEmpty()) {
            val skippedInner = approaches.filter { it.id !in chosenApproach.map { sensor -> sensor.id }.toSet() }
            if (skippedInner.isNotEmpty()) {
                Log.i(
                    RazrClockApp.TAG,
                    "Hand presence left inner proximity unused: " + skippedInner.joinToString { label(it) },
                )
            }
        } else if (chosenApproach.isNotEmpty()) {
            Log.i(RazrClockApp.TAG, "Hand presence has no cover approach sensor; using ${label(chosenApproach.first())}")
        }
        return chosenApproach.map { Pick(it, SensorRole.APPROACH) } +
            wakes.map { Pick(it, SensorRole.WAKE) } +
            cycles.map { Pick(it, SensorRole.CYCLE) }
    }

    private fun proximityFallback(sensor: Sensor): SensorRole? {
        if (sensor.type != Sensor.TYPE_PROXIMITY) return null
        if (sensor.type in BLOCKED_TYPES) return null
        return SensorRole.APPROACH
    }

    private fun rising(id: Int, value: Float): Boolean {
        val high = value > 0.5f
        val was = id in pulsed
        if (high) pulsed += id else pulsed -= id
        return high && !was
    }

    private fun preferNonWake(sensors: List<Sensor>): List<Sensor> {
        return sensors
            .groupBy { "${it.stringType}:${it.name}" }
            .values
            .map { group -> group.firstOrNull { !it.isWakeUpSensor } ?: group.first() }
    }

    private fun isCoverSide(sensor: Sensor): Boolean {
        val key = key(sensor)
        return COVER_HINTS.any { key.contains(it) }
    }

    private fun key(sensor: Sensor): String = "${sensor.stringType} ${sensor.name}".lowercase()

    private fun label(sensor: Sensor): String = "${sensor.name} (${sensor.stringType})"

    private fun isNear(sensor: Sensor, value: Float): Boolean {
        val max = sensor.maximumRange
        if (!max.isFinite() || max <= 0f) return value <= 0f
        return value < max
    }

    companion object {
        const val IDLE_BRIGHTNESS = 0.12f
        const val AWAKE_BRIGHTNESS = 1f
        const val FLASH_MS = 50L

        private val COVER_HINTS = listOf("cli", "cover", "external", "approach", "fip", "flip", "secondary", "rear", "outer")
        private val BLOCKED_TYPES = setOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_HINGE_ANGLE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR,
        )
    }
}

data class HandCue(
    val enabled: Boolean = false,
    val active: Boolean = false,
    val view: CoverView = CoverView.CLOCK,
    val spin: Int = 0,
    val brightness: Float = HandPresence.IDLE_BRIGHTNESS,
    val flash: Boolean = false,
    val using: String = "not listening",
    val available: String = "",
)
