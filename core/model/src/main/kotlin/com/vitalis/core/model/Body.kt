package com.vitalis.core.model

import java.time.LocalDate

/** One dated set of body readings. Every field is optional — users log what they have. */
data class BodyMeasurement(
    val id: String,
    val date: LocalDate,
    val weightKg: Float? = null,
    val bodyFatPercent: Float? = null,
    val waistCm: Float? = null,
    val hipCm: Float? = null,
    val chestCm: Float? = null,
    val armCm: Float? = null,
    val thighCm: Float? = null,
    val restingHeartRate: Int? = null,
    val hrvMs: Float? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val sleepMinutes: Int? = null,
    val spo2Percent: Float? = null,
    val photoUri: String? = null,
    val note: String? = null,
)

/** Derived body composition figures (spec §5.7). */
data class BodyMetrics(
    val bmi: Float? = null,
    val bmiCategory: BmiCategory? = null,
    val bodyFatPercent: Float? = null,
    /** True when [bodyFatPercent] came from the Deurenberg BMI estimate rather than a measurement. */
    val isBodyFatEstimated: Boolean = false,
    val leanBodyMassKg: Float? = null,
    val fatMassKg: Float? = null,
    val waistToHeightRatio: Float? = null,
    val waistToHipRatio: Float? = null,
    val idealWeightKg: Float? = null,
) {
    /** WHtR below 0.5 is the widely used "keep your waist under half your height" rule. */
    val isWaistHealthy: Boolean? get() = waistToHeightRatio?.let { it < 0.5f }

    companion object {
        val EMPTY = BodyMetrics()
    }
}

/**
 * Composite 0–100 wellbeing indicator (spec §4.4.2).
 *
 * Presented as a *trend* — "up 4 points from last week" — never as a verdict, and
 * always with the medical disclaimer. Pillars with no data are excluded and their
 * weight is redistributed rather than counted as zero.
 */
data class HealthScore(
    val total: Int,
    val pillars: List<PillarScore>,
    val previousTotal: Int? = null,
) {
    val delta: Int? get() = previousTotal?.let { total - it }

    /** Pillars the user has not supplied any data for — the actionable "add this" list. */
    val missingPillars: List<HealthPillar> get() = pillars.filter { !it.hasData }.map { it.pillar }

    companion object {
        val EMPTY = HealthScore(total = 0, pillars = emptyList())
    }
}

data class PillarScore(
    val pillar: HealthPillar,
    /** 0–100 within the pillar, or null when there is nothing to score. */
    val score: Int?,
    val hasData: Boolean,
) {
    val weightedContribution: Double get() = (score ?: 0) * pillar.weight
}

enum class HealthPillar(val weight: Double) {
    BODY_COMPOSITION(0.25),
    CARDIO_FITNESS(0.25),
    ACTIVITY(0.20),
    NUTRITION(0.20),
    RECOVERY(0.10),
}

/** A single weight reading paired with its smoothed trend value, for the progress chart. */
data class WeightPoint(
    val date: LocalDate,
    val weightKg: Float,
    /** Exponentially smoothed weight — daily scale noise hides real progress. */
    val trendKg: Float,
)

/** Heart rate training zones as % of HRmax (spec §5.8). */
enum class HeartRateZone(val lowerFraction: Double, val upperFraction: Double) {
    Z1_RECOVERY(0.50, 0.60),
    Z2_AEROBIC(0.60, 0.70),
    Z3_TEMPO(0.70, 0.80),
    Z4_THRESHOLD(0.80, 0.90),
    Z5_VO2MAX(0.90, 1.00);

    companion object {
        fun forHeartRate(hr: Int, hrMax: Int): HeartRateZone? {
            if (hrMax <= 0) return null
            val fraction = hr.toDouble() / hrMax
            return entries.firstOrNull { fraction >= it.lowerFraction && fraction < it.upperFraction }
                ?: if (fraction >= Z5_VO2MAX.lowerFraction) Z5_VO2MAX else null
        }
    }
}
