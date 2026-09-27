package com.vitalis.core.domain.tracking

import com.vitalis.core.model.GpsSignalQuality
import com.vitalis.core.model.SportProfile
import com.vitalis.core.model.TrackPoint

/**
 * The nine-stage location pipeline from spec §8.3.
 *
 * Every stage exists because of a specific, observed failure mode:
 *
 * 1. **Accuracy gate** — a 60 m fix in an urban canyon is noise, not position.
 * 2. **Warm-up discard** — the first fixes after a cold start are wildly off; keeping
 *    them puts a spurious 200 m spike at the very start of every route.
 * 3. **Speed sanity** — losing signal in a tunnel and regaining it 800 m later would
 *    otherwise register as an instantaneous 800 m sprint.
 * 4. **Kalman smoothing** — removes the standing-still jitter that inflates distance.
 * 5. **Distance accumulation** — Haversine between consecutive accepted points.
 * 6–7. **Altitude smoothing and gain thresholding** — see [ElevationProcessor].
 *
 * Auto-pause (stage 8) and persistence (stage 9) are driven by the caller, since
 * they need session state and I/O that this pure component deliberately avoids.
 *
 * Not thread-safe: one instance per session, updated from a single coroutine.
 */
class LocationPipeline(
    private val profile: SportProfile,
    private val warmUpFixesToDiscard: Int = 3,
) {
    private val kalman = KalmanLocationFilter(
        // Fast sports need the filter to track quickly; slow ones can smooth harder.
        processNoiseMps = if (profile.maxPlausibleSpeedMps > 10) 5f else 3f,
    )
    private val elevation = ElevationProcessor()

    private var fixesSeen = 0
    private var lastAccepted: TrackPoint? = null

    var totalDistanceMeters: Double = 0.0
        private set

    var maxSpeedMps: Double = 0.0
        private set

    val elevationGainM: Double get() = elevation.totalGainM
    val elevationLossM: Double get() = elevation.totalLossM
    val maxAltitudeM: Double? get() = elevation.maxAltitudeM

    /** Why a fix was rejected — surfaced in debug builds to diagnose accuracy complaints. */
    enum class Rejection { NONE, LOW_ACCURACY, WARM_UP, IMPLAUSIBLE_SPEED, DUPLICATE }

    data class Result(
        val point: TrackPoint?,
        val rejection: Rejection,
        val distanceAddedMeters: Double = 0.0,
        val instantaneousSpeedMps: Double = 0.0,
    ) {
        val accepted: Boolean get() = point != null
    }

    fun process(
        latitude: Double,
        longitude: Double,
        altitudeM: Double?,
        accuracyM: Float,
        reportedSpeedMps: Float?,
        bearing: Float?,
        timestampMs: Long,
    ): Result {
        fixesSeen++

        if (accuracyM > profile.maxAccuracyM) {
            return Result(null, Rejection.LOW_ACCURACY)
        }

        // The receiver needs a few fixes to settle; those early ones are the worst
        // of the session and would anchor the route in the wrong place.
        if (fixesSeen <= warmUpFixesToDiscard) {
            return Result(null, Rejection.WARM_UP)
        }

        val (smoothedLat, smoothedLng) = kalman.process(latitude, longitude, accuracyM, timestampMs)

        val previous = lastAccepted
        var segmentDistance = 0.0
        var instantaneousSpeed = reportedSpeedMps?.toDouble() ?: 0.0

        if (previous != null) {
            val elapsedSeconds = (timestampMs - previous.timestamp) / 1000.0
            if (elapsedSeconds <= 0) return Result(null, Rejection.DUPLICATE)

            segmentDistance = GeoMath.distanceMeters(
                previous.latitude, previous.longitude, smoothedLat, smoothedLng,
            )
            val impliedSpeed = segmentDistance / elapsedSeconds

            if (impliedSpeed > profile.maxPlausibleSpeedMps) {
                return Result(null, Rejection.IMPLAUSIBLE_SPEED)
            }

            // Prefer the receiver's Doppler speed when present — it is measured
            // rather than differentiated, so it is both faster and less noisy.
            if (reportedSpeedMps == null) instantaneousSpeed = impliedSpeed

            totalDistanceMeters += segmentDistance
            maxSpeedMps = maxOf(maxSpeedMps, instantaneousSpeed)
        }

        val smoothedAltitude = altitudeM?.let { elevation.process(it) }

        val point = TrackPoint(
            latitude = smoothedLat,
            longitude = smoothedLng,
            altitudeM = smoothedAltitude,
            accuracyM = accuracyM,
            speedMps = instantaneousSpeed.toFloat(),
            bearing = bearing,
            timestamp = timestampMs,
        )
        lastAccepted = point

        return Result(point, Rejection.NONE, segmentDistance, instantaneousSpeed)
    }

    /** Feeds barometric altitude, which is far more precise than the GPS vertical channel. */
    fun processBarometricAltitude(altitudeM: Double) {
        elevation.process(altitudeM)
    }

    /**
     * Breaks the route after a manual pause: whatever the user walked while paused must not
     * be bridged as one straight segment on resume. Totals are kept.
     */
    fun startNewSegment() {
        kalman.reset()
        lastAccepted = null
    }

    fun reset() {
        kalman.reset()
        elevation.reset()
        fixesSeen = 0
        lastAccepted = null
        totalDistanceMeters = 0.0
        maxSpeedMps = 0.0
    }

    companion object {
        /** Maps a raw accuracy reading to the signal bars shown on the pre-start screen. */
        fun signalQuality(accuracyM: Float?): GpsSignalQuality = when {
            accuracyM == null -> GpsSignalQuality.NONE
            accuracyM <= 5f -> GpsSignalQuality.EXCELLENT
            accuracyM <= 10f -> GpsSignalQuality.GOOD
            accuracyM <= 20f -> GpsSignalQuality.FAIR
            else -> GpsSignalQuality.POOR
        }
    }
}
