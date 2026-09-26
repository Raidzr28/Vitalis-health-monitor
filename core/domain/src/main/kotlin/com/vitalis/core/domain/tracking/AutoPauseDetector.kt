package com.vitalis.core.domain.tracking

import com.vitalis.core.model.AutoPauseProfile

/**
 * Auto-pause with hysteresis and a dwell time (spec §8.4).
 *
 * Two guards against flapping. First, separate pause and resume thresholds: a
 * runner slowing to 0.6 m/s at a junction does not immediately un-pause on a
 * single noisy fix. Second, a dwell requirement — speed must stay below the pause
 * threshold for the whole delay window, so one bad GPS sample cannot stop the clock.
 *
 * Sports with legitimate long stationary periods (climbing) pass a null profile
 * and are never auto-paused.
 */
class AutoPauseDetector(private val profile: AutoPauseProfile?) {

    var isPaused: Boolean = false
        private set

    private var belowThresholdSinceMs: Long? = null
    private var aboveThresholdSinceMs: Long? = null

    /** Emitted when the detector wants the session state to change. */
    enum class Transition { NONE, SHOULD_PAUSE, SHOULD_RESUME }

    /**
     * @param speedMps current speed
     * @param nowMs monotonic timestamp of this sample
     */
    fun update(speedMps: Double, nowMs: Long): Transition {
        val config = profile ?: return Transition.NONE
        val delayMs = config.delaySeconds * 1000L

        return if (!isPaused) {
            if (speedMps < config.pauseBelowMps) {
                val since = belowThresholdSinceMs ?: nowMs.also { belowThresholdSinceMs = it }
                if (nowMs - since >= delayMs) {
                    isPaused = true
                    belowThresholdSinceMs = null
                    aboveThresholdSinceMs = null
                    Transition.SHOULD_PAUSE
                } else {
                    Transition.NONE
                }
            } else {
                belowThresholdSinceMs = null
                Transition.NONE
            }
        } else {
            if (speedMps > config.resumeAboveMps) {
                // Resume is deliberately quicker than pause — a user restarting
                // should not lose seconds of real movement waiting on a dwell timer.
                val since = aboveThresholdSinceMs ?: nowMs.also { aboveThresholdSinceMs = it }
                if (nowMs - since >= RESUME_DWELL_MS) {
                    isPaused = false
                    belowThresholdSinceMs = null
                    aboveThresholdSinceMs = null
                    Transition.SHOULD_RESUME
                } else {
                    Transition.NONE
                }
            } else {
                aboveThresholdSinceMs = null
                Transition.NONE
            }
        }
    }

    /** Called when the user pauses or resumes by hand, so the detector does not fight them. */
    fun setPaused(paused: Boolean) {
        isPaused = paused
        belowThresholdSinceMs = null
        aboveThresholdSinceMs = null
    }

    fun reset() {
        isPaused = false
        belowThresholdSinceMs = null
        aboveThresholdSinceMs = null
    }

    private companion object {
        const val RESUME_DWELL_MS = 2_000L
    }
}

/**
 * Automatic lap splitting every [lapDistanceMeters] (spec §4.5.3).
 *
 * Handles the case where a single GPS fix jumps past a lap boundary — the split
 * is recorded at the boundary rather than wherever the fix happened to land, so
 * a 5 km run always produces five 1 km laps.
 */
class LapTracker(private val lapDistanceMeters: Double = 1000.0) {

    private var lastLapDistanceM = 0.0
    private var lastLapTimeSeconds = 0L
    private var lastLapElevationM = 0.0
    var lapCount: Int = 0
        private set

    data class CompletedLap(
        val index: Int,
        val distanceMeters: Double,
        val durationSeconds: Long,
        val elevationGainM: Double,
    )

    /**
     * @return a lap if the boundary was crossed by this update, otherwise null
     */
    fun update(
        totalDistanceMeters: Double,
        movingSeconds: Long,
        totalElevationGainM: Double,
    ): CompletedLap? {
        if (totalDistanceMeters - lastLapDistanceM < lapDistanceMeters) return null

        lapCount++
        val lap = CompletedLap(
            index = lapCount,
            distanceMeters = lapDistanceMeters,
            durationSeconds = movingSeconds - lastLapTimeSeconds,
            elevationGainM = totalElevationGainM - lastLapElevationM,
        )
        // Advance by exactly one lap distance rather than to the current position,
        // so overshoot carries into the next lap instead of being lost.
        lastLapDistanceM += lapDistanceMeters
        lastLapTimeSeconds = movingSeconds
        lastLapElevationM = totalElevationGainM
        return lap
    }

    /** Closes the final, partial lap when the session ends. */
    fun finish(
        totalDistanceMeters: Double,
        movingSeconds: Long,
        totalElevationGainM: Double,
    ): CompletedLap? {
        val remaining = totalDistanceMeters - lastLapDistanceM
        if (remaining < 1.0) return null
        lapCount++
        return CompletedLap(
            index = lapCount,
            distanceMeters = remaining,
            durationSeconds = movingSeconds - lastLapTimeSeconds,
            elevationGainM = totalElevationGainM - lastLapElevationM,
        )
    }

    fun reset() {
        lastLapDistanceM = 0.0
        lastLapTimeSeconds = 0L
        lastLapElevationM = 0.0
        lapCount = 0
    }
}
