package com.vitalis.core.domain.performance

import kotlin.math.exp
import kotlin.math.roundToInt

/** Pace, speed and grade-adjusted pace (spec §5.8). */
object PaceCalculator {

    /** Seconds per kilometre. Returns null rather than infinity for zero distance. */
    fun paceSecPerKm(distanceMeters: Double, durationSeconds: Double): Double? {
        if (distanceMeters < 1.0 || durationSeconds <= 0) return null
        return durationSeconds / (distanceMeters / 1000.0)
    }

    fun speedKmh(distanceMeters: Double, durationSeconds: Double): Double {
        if (durationSeconds <= 0) return 0.0
        return (distanceMeters / 1000.0) / (durationSeconds / 3600.0)
    }

    fun mpsToPaceSecPerKm(speedMps: Double): Double? =
        if (speedMps <= 0.1) null else 1000.0 / speedMps

    /**
     * Grade Adjusted Pace — what the same effort would have been on flat ground.
     *
     * Without it, hill sessions look like bad runs and downhill sessions look like
     * breakthroughs. Uphill costs roughly 3% per 1% of grade; downhill returns
     * about half that, and only up to a point (spec §5.8).
     */
    fun gradeAdjustedPaceSecPerKm(actualPaceSecPerKm: Double, gradePercent: Double): Double {
        val clamped = gradePercent.coerceIn(-20.0, 20.0)
        val factor = if (clamped >= 0) {
            1.0 + 0.03 * clamped
        } else {
            1.0 + 0.015 * clamped // clamped is negative, so this reduces the pace
        }
        return (actualPaceSecPerKm * factor).coerceAtLeast(60.0)
    }

    /** Formats seconds-per-km as `m:ss`. */
    fun formatPace(paceSecPerKm: Double?): String {
        if (paceSecPerKm == null || paceSecPerKm.isNaN() || paceSecPerKm <= 0) return "--:--"
        val total = paceSecPerKm.roundToInt()
        return "%d:%02d".format(total / 60, total % 60)
    }
}

/** Heart-rate derived figures (spec §5.8). */
object HeartRateCalculator {

    /**
     * Tanaka: `208 − 0.7 × age`. Preferred over `220 − age`, which was never
     * derived from data and is badly wrong at both ends of the age range.
     */
    fun estimateHrMax(ageYears: Int): Int = (208.0 - 0.7 * ageYears).roundToInt()

    /** Uth–Sørensen–Overgaard–Pedersen: `VO2max ≈ 15.3 × HRmax / HRrest`. */
    fun estimateVo2MaxFromRestingHr(hrMax: Int, hrRest: Int): Double {
        if (hrRest <= 0) return 0.0
        return 15.3 * (hrMax.toDouble() / hrRest)
    }

    fun heartRateReserveFraction(avgHr: Int, restingHr: Int, maxHr: Int): Double {
        val denominator = (maxHr - restingHr).toDouble()
        if (denominator <= 0) return 0.0
        return ((avgHr - restingHr) / denominator).coerceIn(0.0, 1.0)
    }
}

/**
 * TRIMP — training impulse, i.e. how much a session actually cost the body
 * (spec §5.8). Duration alone cannot distinguish an easy hour from a hard one;
 * the exponential term is what makes intensity dominate.
 */
object TrimpCalculator {

    fun calculate(
        durationMinutes: Double,
        avgHeartRate: Int,
        restingHeartRate: Int,
        maxHeartRate: Int,
        isMale: Boolean,
    ): Double {
        val ratio = HeartRateCalculator.heartRateReserveFraction(avgHeartRate, restingHeartRate, maxHeartRate)
        if (ratio <= 0) return 0.0
        // Sex-specific weighting factors from Banister's original formulation.
        val (coefficient, exponent) = if (isMale) 0.64 to 1.92 else 0.86 to 1.67
        return durationMinutes * ratio * coefficient * exp(exponent * ratio)
    }

    /**
     * Fallback when no heart rate exists: scale duration by the activity's MET.
     * Cruder, but keeps the training-load chart continuous for phone-only users.
     */
    fun estimateFromMet(durationMinutes: Double, met: Double): Double =
        durationMinutes * (met / 10.0) * 1.5
}

/** VO₂max estimated from race performance (spec §5.8). */
object VdotCalculator {

    /**
     * Daniels & Gilbert VDOT.
     *
     * @param distanceMeters distance covered at a hard, sustained effort
     * @param durationSeconds time taken
     * @return estimated VO₂max in ml/kg/min, or null if the effort is too short to model
     */
    fun estimate(distanceMeters: Double, durationSeconds: Double): Double? {
        // Below ~1.5 km or 4 minutes the model is dominated by anaerobic
        // contribution and returns implausible numbers.
        if (distanceMeters < 1500 || durationSeconds < 240) return null

        val minutes = durationSeconds / 60.0
        val velocityMPerMin = distanceMeters / minutes

        val vo2 = -4.60 + (0.182258 * velocityMPerMin) + (0.000104 * velocityMPerMin * velocityMPerMin)
        val percentMax = 0.8 +
            (0.1894393 * exp(-0.012778 * minutes)) +
            (0.2989558 * exp(-0.1932605 * minutes))

        if (percentMax <= 0) return null
        return (vo2 / percentMax).takeIf { it in 20.0..90.0 }
    }
}
