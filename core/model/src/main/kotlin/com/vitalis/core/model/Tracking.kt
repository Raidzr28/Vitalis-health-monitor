package com.vitalis.core.model

/**
 * Per-sport GPS behaviour (spec §8.1, §8.3, §8.4).
 *
 * Sampling rate, accuracy tolerance and auto-pause thresholds all differ by
 * sport: a road cyclist needs 1 Hz fixes and rejects anything above 25 m/s,
 * while a multi-hour trek prioritises battery and tolerates slow, noisy fixes.
 */
data class SportProfile(
    val sportType: SportType,
    /** Location request interval, milliseconds. */
    val samplingIntervalMs: Long,
    /** Fixes worse than this are discarded outright. */
    val maxAccuracyM: Float,
    /** Implied speed above this between two fixes means a GPS jump, not movement. */
    val maxPlausibleSpeedMps: Double,
    val autoPause: AutoPauseProfile?,
) {
    companion object {
        /**
         * Defaults straight from the spec tables. Kept in code rather than in
         * `sport_configs.json` because these are behavioural constants the GPS
         * engine depends on — a malformed asset should not be able to break
         * distance accuracy at runtime.
         */
        val DEFAULTS: Map<SportType, SportProfile> = buildMap {
            fun put(
                sport: SportType,
                intervalMs: Long,
                maxAccuracy: Float,
                maxSpeed: Double,
                autoPause: AutoPauseProfile?,
            ) = put(sport, SportProfile(sport, intervalMs, maxAccuracy, maxSpeed, autoPause))

            put(SportType.RUNNING, 1_000L, 25f, 8.0, AutoPauseProfile(0.5, 1.0, 5))
            put(SportType.TRAIL_RUNNING, 1_000L, 30f, 8.0, AutoPauseProfile(0.5, 1.0, 8))
            put(SportType.WALKING, 3_000L, 30f, 4.0, AutoPauseProfile(0.3, 0.6, 8))
            put(SportType.HIKING, 3_000L, 50f, 4.0, AutoPauseProfile(0.2, 0.5, 15))
            put(SportType.TREKKING, 5_000L, 50f, 4.0, AutoPauseProfile(0.2, 0.5, 20))
            // Auto-pause is disabled while climbing: long legitimate stops at a
            // belay would otherwise chop the session apart (spec §8.4).
            put(SportType.MOUNTAIN_CLIMBING, 5_000L, 50f, 4.0, null)
            put(SportType.CYCLING_ROAD, 1_000L, 25f, 25.0, AutoPauseProfile(1.0, 2.0, 5))
            put(SportType.MOUNTAIN_BIKING, 1_000L, 30f, 20.0, AutoPauseProfile(0.8, 1.8, 6))
            put(SportType.HORSE_RIDING, 2_000L, 30f, 18.0, AutoPauseProfile(0.4, 0.8, 10))
            put(SportType.OPEN_WATER_SWIMMING, 3_000L, 30f, 3.0, null)
            put(SportType.ROWING, 2_000L, 30f, 8.0, AutoPauseProfile(0.4, 0.8, 10))
            put(SportType.KAYAKING, 3_000L, 30f, 8.0, AutoPauseProfile(0.4, 0.8, 10))
            put(SportType.SKATEBOARDING, 2_000L, 25f, 15.0, AutoPauseProfile(0.6, 1.2, 6))
            put(SportType.INLINE_SKATING, 2_000L, 25f, 18.0, AutoPauseProfile(0.6, 1.2, 6))
        }

        fun forSport(sportType: SportType): SportProfile =
            DEFAULTS[sportType] ?: SportProfile(
                sportType = sportType,
                samplingIntervalMs = 3_000L,
                maxAccuracyM = 30f,
                maxPlausibleSpeedMps = 10.0,
                autoPause = null,
            )
    }
}

/**
 * Hysteresis for auto-pause: pausing and resuming use different thresholds so a
 * runner hovering near the limit does not flicker between states.
 */
data class AutoPauseProfile(
    val pauseBelowMps: Double,
    val resumeAboveMps: Double,
    val delaySeconds: Int,
)

/**
 * Live state of an in-progress session, emitted from the tracking service to the UI.
 */
data class TrackingState(
    val status: TrackingStatus = TrackingStatus.IDLE,
    val sessionId: String? = null,
    val sportType: SportType = SportType.RUNNING,
    val elapsedSeconds: Long = 0,
    val movingSeconds: Long = 0,
    val distanceMeters: Double = 0.0,
    val currentSpeedMps: Double = 0.0,
    val avgSpeedMps: Double = 0.0,
    val maxSpeedMps: Double = 0.0,
    val elevationGainM: Double = 0.0,
    val elevationLossM: Double = 0.0,
    val currentAltitudeM: Double? = null,
    val maxAltitudeM: Double? = null,
    val kcalGross: Int = 0,
    val kcalNet: Int = 0,
    val heartRate: Int? = null,
    val cadence: Int? = null,
    val laps: List<Lap> = emptyList(),
    val route: List<TrackPoint> = emptyList(),
    val signalQuality: GpsSignalQuality = GpsSignalQuality.NONE,
    val isBatterySaverActive: Boolean = false,
) {
    val isRecording: Boolean get() = status == TrackingStatus.ACTIVE
    val isPaused: Boolean
        get() = status == TrackingStatus.PAUSED_MANUAL || status == TrackingStatus.PAUSED_AUTO

    val distanceKm: Double get() = distanceMeters / 1000.0

    /** Current pace in seconds per km; null below 10 m so the number is not nonsense at the start. */
    val currentPaceSecPerKm: Double?
        get() = if (currentSpeedMps < 0.3) null else 1000.0 / currentSpeedMps

    val avgPaceSecPerKm: Double?
        get() = if (distanceMeters < 10.0 || movingSeconds <= 0) null
        else movingSeconds / (distanceMeters / 1000.0)
}

/**
 * Small, cheap snapshot persisted every few seconds so a killed process can offer
 * "resume your activity?" on next launch (spec §8.6). Aggressive OEM battery
 * managers make this mandatory, not optional.
 */
data class TrackingSnapshot(
    val sessionId: String,
    val sportType: SportType,
    val startTimeEpochMs: Long,
    val lastPointTimeEpochMs: Long,
    val distanceMeters: Double,
    val movingSeconds: Long,
    val elapsedSeconds: Long,
    val elevationGainM: Double,
    val isPaused: Boolean,
) {
    companion object {
        /** Snapshots older than this are stale — a resume prompt would be confusing. */
        const val MAX_RESUME_AGE_MS: Long = 6 * 60 * 60 * 1000
    }
}
