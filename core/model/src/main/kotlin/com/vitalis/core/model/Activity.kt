package com.vitalis.core.model

import java.time.Instant

/**
 * A completed workout, whether recorded by GPS, entered by hand, or imported.
 *
 * Sessions are immutable once saved (spec §14.2); editing produces a new row so
 * that gamification awards and personal records stay reproducible.
 */
data class ActivitySession(
    val id: String,
    val sportType: SportType,
    val source: ActivitySource,
    val startTime: Instant,
    val endTime: Instant,
    val elapsedSeconds: Long,
    /** Elapsed minus auto-paused and manually paused time. Drives average pace. */
    val movingSeconds: Long,
    val distanceMeters: Double? = null,
    val avgSpeedMps: Double? = null,
    val maxSpeedMps: Double? = null,
    val elevationGainM: Double? = null,
    val elevationLossM: Double? = null,
    val maxAltitudeM: Double? = null,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val avgCadence: Int? = null,
    /** Familiar "calories burnt" number. Shown, but never added to the budget. */
    val kcalGross: Int = 0,
    /** Gross minus resting burn — this is what reaches the energy budget. */
    val kcalNet: Int = 0,
    val calorieModel: CalorieModel = CalorieModel.MET_STATIC,
    val metValue: Double? = null,
    val trainingLoad: Double? = null,
    val title: String? = null,
    val note: String? = null,
    /** Compressed route for map thumbnails, so list screens never load raw points. */
    val encodedPolyline: String? = null,
    val isSynced: Boolean = false,
) {
    /** Average pace in seconds per kilometre, or null when distance is unknown/zero. */
    val avgPaceSecPerKm: Double?
        get() {
            val meters = distanceMeters ?: return null
            if (meters < 1.0 || movingSeconds <= 0) return null
            return movingSeconds / (meters / 1000.0)
        }

    val distanceKm: Double get() = (distanceMeters ?: 0.0) / 1000.0
}

/** One recorded GPS fix, after filtering. */
data class TrackPoint(
    val latitude: Double,
    val longitude: Double,
    val altitudeM: Double? = null,
    val accuracyM: Float = 0f,
    val speedMps: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = 0L,
    /** Marked true for points the pipeline rejected; kept for debugging accuracy issues. */
    val isFiltered: Boolean = false,
)

/** An automatic (per km) or manual lap (spec §4.5.3). */
data class Lap(
    val index: Int,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val elevationGainM: Double = 0.0,
    val avgHeartRate: Int? = null,
    val isManual: Boolean = false,
) {
    val paceSecPerKm: Double
        get() = if (distanceMeters < 1.0) 0.0 else durationSeconds / (distanceMeters / 1000.0)
}

/**
 * An entry from the 2024 Adult Compendium of Physical Activities, loaded from
 * `met_activities.json`. [code] is the Compendium's five-digit code, kept for
 * traceability back to the source table (spec §5.4).
 */
data class MetActivity(
    val code: String,
    val category: String,
    val nameId: String,
    val nameEn: String,
    val met: Double,
    val speedRangeKmh: ClosedFloatingPointRange<Double>? = null,
    val sportType: SportType? = null,
)

/** Compact row for activity history lists. */
data class ActivitySummary(
    val id: String,
    val sportType: SportType,
    val startTime: Instant,
    val distanceMeters: Double?,
    val movingSeconds: Long,
    val kcalNet: Int,
    val elevationGainM: Double?,
    val encodedPolyline: String?,
)
