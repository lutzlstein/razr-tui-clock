package com.coryblische.razrclock.ui

import android.view.Choreographer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Publishes the local time once per wall-clock second, on the choreographer
 * frame at or just after that boundary. Callers redraw from this emission
 * instead of a delay loop that drifts and instead of a tween between seconds.
 */
class SecondClock {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _now = MutableStateFlow(LocalDateTime.now().withNano(0))
    val now: StateFlow<LocalDateTime> = _now.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            while (isActive) {
                awaitSecondBoundary()
                _now.value = LocalDateTime.now().withNano(0)
            }
        }
    }

    private suspend fun awaitSecondBoundary() {
        val target = (System.currentTimeMillis() / 1_000L + 1L) * 1_000L
        while (System.currentTimeMillis() < target) {
            val left = target - System.currentTimeMillis()
            if (left > 0L) delay(left)
        }
        awaitFrame()
    }

    private suspend fun awaitFrame() = suspendCancellableCoroutine { continuation ->
        val choreographer = Choreographer.getInstance()
        val callback = Choreographer.FrameCallback {
            if (continuation.isActive) continuation.resume(Unit)
        }
        choreographer.postFrameCallback(callback)
        continuation.invokeOnCancellation { choreographer.removeFrameCallback(callback) }
    }
}
