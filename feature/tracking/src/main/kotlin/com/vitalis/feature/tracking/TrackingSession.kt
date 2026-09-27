package com.vitalis.feature.tracking

import com.vitalis.core.domain.energy.MetCalculator
import com.vitalis.core.domain.tracking.AutoPauseDetector
import com.vitalis.core.domain.tracking.LapTracker
import com.vitalis.core.domain.tracking.LocationPipeline
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.ActivitySource
import com.vitalis.core.model.Lap
import com.vitalis.core.model.SportProfile
import com.vitalis.core.model.SportType
import com.vitalis.core.model.TrackPoint
import com.vitalis.core.model.TrackingState
import com.vitalis.core.model.TrackingStatus
import java.time.Instant
import java.util.UUID

/** Everything the summary screen and the save step need once recording stops. */
data class FinishedActivity(val session: ActivitySession, val route: List<TrackPoint>, val laps: List<Lap>)

/**
 * One recording, from start to stop (spec §8.3 stages 1–8). Pure Kotlin so the clock and the
 * pause logic are testable without a device; [TrackingService] only feeds it fixes and ticks.
 *
 * All timestamps are wall-clock ms, the same clock `Location.time` uses.
 * Not thread-safe: fixes, ticks and user actions all arrive on the main thread.
 */
class TrackingSession(
    val sport: SportType,
    private val weightKg: Double,
    private val bmrKcal: Int,
    private val startMs: Long,
    val id: String = UUID.randomUUID().toString(),
) {
    private val pipeline = LocationPipeline(SportProfile.forSport(sport))
    private val autoPause = AutoPauseDetector(SportProfile.forSport(sport).autoPause)
    private val lapTracker = LapTracker()
    private val laps = mutableListOf<Lap>()
    private val route = mutableListOf<TrackPoint>()

    private var status = TrackingStatus.ACTIVE
    private var movingMs = 0L
    private var lastTickMs = startMs
    private var nowMs = startMs
    private var currentSpeedMps = 0.0
    private var lastAccuracyM: Float? = null

    private val movingSeconds get() = movingMs / 1000

    /** Advances the clocks. Moving time only accrues while ACTIVE. */
    fun tick(now: Long) {
        if (now <= lastTickMs) return // a fix can carry a timestamp older than the last 1 s tick
        if (status == TrackingStatus.ACTIVE) movingMs += now - lastTickMs
        lastTickMs = now
        nowMs = now
    }

    fun onFix(lat: Double, lng: Double, altitudeM: Double?, accuracyM: Float, speedMps: Float?, bearing: Float?, timeMs: Long) {
        tick(timeMs)
        lastAccuracyM = accuracyM
        if (status == TrackingStatus.PAUSED_MANUAL) return

        val r = pipeline.process(lat, lng, altitudeM, accuracyM, speedMps, bearing, timeMs)
        val point = r.point ?: return
        route += point
        currentSpeedMps = r.instantaneousSpeedMps

        when (autoPause.update(currentSpeedMps, timeMs)) {
            AutoPauseDetector.Transition.SHOULD_PAUSE -> status = TrackingStatus.PAUSED_AUTO
            AutoPauseDetector.Transition.SHOULD_RESUME -> status = TrackingStatus.ACTIVE
            AutoPauseDetector.Transition.NONE -> Unit
        }
        lapTracker.update(pipeline.totalDistanceMeters, movingSeconds, pipeline.elevationGainM)?.let { laps += it.toLap() }
    }

    fun togglePause(now: Long) {
        tick(now)
        if (status == TrackingStatus.ACTIVE || status == TrackingStatus.PAUSED_AUTO) {
            status = TrackingStatus.PAUSED_MANUAL
            autoPause.setPaused(true)
            currentSpeedMps = 0.0
        } else {
            status = TrackingStatus.ACTIVE
            autoPause.setPaused(false)
            pipeline.startNewSegment()
        }
    }

    fun state(): TrackingState {
        val kcal = kcal()
        return TrackingState(
            status = status,
            sessionId = id,
            sportType = sport,
            elapsedSeconds = (nowMs - startMs) / 1000,
            movingSeconds = movingSeconds,
            distanceMeters = pipeline.totalDistanceMeters,
            currentSpeedMps = if (status == TrackingStatus.ACTIVE) currentSpeedMps else 0.0,
            avgSpeedMps = avgSpeed(),
            maxSpeedMps = pipeline.maxSpeedMps,
            elevationGainM = pipeline.elevationGainM,
            elevationLossM = pipeline.elevationLossM,
            currentAltitudeM = route.lastOrNull()?.altitudeM,
            maxAltitudeM = pipeline.maxAltitudeM,
            kcalGross = kcal.grossKcal,
            kcalNet = kcal.netKcal,
            laps = laps.toList(),
            // ponytail: copies the whole route every tick (~7k points after 2 h at 1 Hz); fine for now,
            // switch to an append-only flow if the live screen shows jank on long sessions.
            route = route.toList(),
            signalQuality = LocationPipeline.signalQuality(lastAccuracyM),
        )
    }

    fun finish(endMs: Long): FinishedActivity {
        tick(endMs)
        val allLaps = laps + listOfNotNull(
            lapTracker.finish(pipeline.totalDistanceMeters, movingSeconds, pipeline.elevationGainM)?.toLap(),
        )
        val kcal = kcal()
        val session = ActivitySession(
            id = id,
            sportType = sport,
            source = ActivitySource.GPS,
            startTime = Instant.ofEpochMilli(startMs),
            endTime = Instant.ofEpochMilli(nowMs),
            elapsedSeconds = (nowMs - startMs) / 1000,
            movingSeconds = movingSeconds,
            distanceMeters = pipeline.totalDistanceMeters,
            avgSpeedMps = avgSpeed(),
            maxSpeedMps = pipeline.maxSpeedMps,
            elevationGainM = pipeline.elevationGainM,
            elevationLossM = pipeline.elevationLossM,
            maxAltitudeM = pipeline.maxAltitudeM,
            kcalGross = kcal.grossKcal,
            kcalNet = kcal.netKcal,
            calorieModel = kcal.model,
            metValue = kcal.metValue,
        )
        return FinishedActivity(session, route.toList(), allLaps)
    }

    private fun avgSpeed() = if (movingMs <= 0) 0.0 else pipeline.totalDistanceMeters / (movingMs / 1000.0)

    // ponytail: static MET per sport; swap in AcsmCalculator/CyclingPowerCalculator per segment (spec §5.5)
    // once the summary needs speed-accurate calories.
    private fun kcal() = MetCalculator.estimate(sport.baseMet, weightKg, movingMs / 60_000.0, bmrKcal)
}

private fun LapTracker.CompletedLap.toLap() = Lap(index, distanceMeters, durationSeconds, elevationGainM)
