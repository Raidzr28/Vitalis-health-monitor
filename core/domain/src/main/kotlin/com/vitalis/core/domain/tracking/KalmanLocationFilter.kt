package com.vitalis.core.domain.tracking

/**
 * One-dimensional Kalman filter applied independently to latitude and longitude
 * (spec §8.3, step 4).
 *
 * Consumer GPS jitters by several metres even while standing still, and that
 * jitter integrates into phantom distance — a runner waiting at a traffic light
 * can accumulate tens of metres. The filter weighs each new fix against the
 * accumulated uncertainty: a fix reported as accurate to 5 m moves the estimate
 * much more than one reported at 40 m.
 *
 * Not thread-safe; the tracking engine owns exactly one instance per session.
 */
class KalmanLocationFilter(
    /**
     * Expected movement noise in metres per second. Higher values trust new fixes
     * more (good for cycling), lower values smooth harder (good for hiking).
     */
    private val processNoiseMps: Float = 3f,
) {
    private var variance: Float = -1f
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var timestampMs: Long = 0L

    val isInitialised: Boolean get() = variance >= 0

    /**
     * Folds a raw fix into the running estimate.
     *
     * @param accuracyM the fix's reported horizontal accuracy; floored at 1 m
     *   because a reported accuracy of zero would make the gain infinite
     * @return the smoothed coordinate
     */
    fun process(
        newLatitude: Double,
        newLongitude: Double,
        accuracyM: Float,
        timeMs: Long,
    ): Pair<Double, Double> {
        val accuracy = accuracyM.coerceAtLeast(1f)

        if (variance < 0) {
            latitude = newLatitude
            longitude = newLongitude
            timestampMs = timeMs
            variance = accuracy * accuracy
            return latitude to longitude
        }

        // Time passing without a fix widens the uncertainty.
        val deltaSeconds = (timeMs - timestampMs) / 1000f
        if (deltaSeconds > 0) {
            variance += deltaSeconds * processNoiseMps * processNoiseMps
            timestampMs = timeMs
        }

        // Kalman gain: how much of the residual to accept.
        val gain = variance / (variance + accuracy * accuracy)
        latitude += gain * (newLatitude - latitude)
        longitude += gain * (newLongitude - longitude)
        variance *= (1 - gain)

        return latitude to longitude
    }

    fun reset() {
        variance = -1f
        latitude = 0.0
        longitude = 0.0
        timestampMs = 0L
    }
}

/**
 * Altitude smoothing and elevation gain (spec §5.9, §8.3 steps 6–7).
 *
 * GPS altitude is roughly three times noisier than its horizontal position, so
 * raw sample-to-sample differences would report hundreds of metres of "climb" on
 * a flat road. Two defences: a moving average, then a threshold below which a
 * change is treated as noise rather than ascent.
 */
class ElevationProcessor(
    private val windowSize: Int = 5,
    /** Changes smaller than this are discarded as noise. */
    private val noiseThresholdM: Double = 3.0,
) {
    private val window = ArrayDeque<Double>()
    private var lastAcceptedAltitude: Double? = null

    var totalGainM: Double = 0.0
        private set

    var totalLossM: Double = 0.0
        private set

    var maxAltitudeM: Double? = null
        private set

    var smoothedAltitudeM: Double? = null
        private set

    /**
     * @param altitudeM raw altitude, from GPS or (much better) a barometer
     * @return the smoothed altitude
     */
    fun process(altitudeM: Double): Double {
        window.addLast(altitudeM)
        if (window.size > windowSize) window.removeFirst()

        val smoothed = window.average()
        smoothedAltitudeM = smoothed

        maxAltitudeM = maxOf(maxAltitudeM ?: smoothed, smoothed)

        val last = lastAcceptedAltitude
        if (last == null) {
            // Wait for a full window before establishing the baseline, otherwise
            // the first few noisy samples become permanent gain.
            if (window.size >= windowSize) lastAcceptedAltitude = smoothed
            return smoothed
        }

        val delta = smoothed - last
        if (delta >= noiseThresholdM) {
            totalGainM += delta
            lastAcceptedAltitude = smoothed
        } else if (delta <= -noiseThresholdM) {
            totalLossM += -delta
            lastAcceptedAltitude = smoothed
        }

        return smoothed
    }

    fun reset() {
        window.clear()
        lastAcceptedAltitude = null
        totalGainM = 0.0
        totalLossM = 0.0
        maxAltitudeM = null
        smoothedAltitudeM = null
    }
}
