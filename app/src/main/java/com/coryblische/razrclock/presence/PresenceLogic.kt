package com.coryblische.razrclock.presence

/**
 * Near / far / wave decisions with no sensor or clock of its own.
 * Times are elapsed milliseconds supplied by the caller.
 *
 * A wave is near, then far, then near again, all inside [waveMs].
 * Edges closer than [debounceMs] are dropped. View changes are limited
 * to one per [cycleCooldownMs] so a vendor gesture and the proximity
 * wave cannot both advance the view.
 */
class PresenceLogic(
    private val idleMs: Long = IDLE_MS,
    private val waveMs: Long = WAVE_MS,
    private val debounceMs: Long = DEBOUNCE_MS,
    private val cycleCooldownMs: Long = CYCLE_COOLDOWN_MS,
) {
    data class State(
        val active: Boolean = false,
        val view: CoverView = CoverView.CLOCK,
        val spin: Int = 0,
        val near: Boolean = false,
        val idleAt: Long? = null,
    )

    var state: State = State()
        private set

    private var lastEdgeAt: Long? = null
    private var waveAnchor = 0L
    private var waveArmed = false
    private var lastCycleAt: Long? = null

    fun onNear(at: Long): State {
        if (state.near) return state
        if (lastEdgeAt != null && at - lastEdgeAt!! < debounceMs) return state
        lastEdgeAt = at
        val wave = waveArmed && at - waveAnchor <= waveMs
        waveArmed = false
        val wasActive = state.active
        var view = state.view
        var spin = state.spin
        if (wave && cycle(at)) {
            view = view.next()
            spin += 1
        } else if (!wasActive) {
            spin += 1
        }
        waveAnchor = at
        state = state.copy(active = true, view = view, spin = spin, near = true, idleAt = null)
        return state
    }

    fun onFar(at: Long): State {
        if (!state.near) return state
        if (lastEdgeAt != null && at - lastEdgeAt!! < debounceMs) return state
        lastEdgeAt = at
        waveArmed = at - waveAnchor <= waveMs
        state = state.copy(near = false, idleAt = at + idleMs)
        return state
    }

    /** A one-shot vendor gesture: wake, spin, and advance the view. */
    fun onGesture(at: Long): State {
        val view = if (cycle(at)) state.view.next() else state.view
        val spun = view != state.view || !state.active
        state = state.copy(
            active = true,
            view = view,
            spin = if (spun) state.spin + 1 else state.spin,
            near = state.near,
            idleAt = at + idleMs,
        )
        if (spun) waveArmed = false
        return state
    }

    fun onTimeout(at: Long): State {
        val due = state.idleAt ?: return state
        if (state.near || at < due) return state
        waveArmed = false
        state = state.copy(active = false, idleAt = null)
        return state
    }

    /** Screen off or the feature was disabled. Keeps the selected view. */
    fun asleep(): State {
        waveArmed = false
        state = state.copy(active = false, near = false, idleAt = null)
        return state
    }

    private fun cycle(at: Long): Boolean {
        if (lastCycleAt != null && at - lastCycleAt!! < cycleCooldownMs) return false
        lastCycleAt = at
        return true
    }

    companion object {
        const val IDLE_MS = 10_000L
        const val WAVE_MS = 1_000L
        const val DEBOUNCE_MS = 80L
        const val CYCLE_COOLDOWN_MS = 400L
    }
}
