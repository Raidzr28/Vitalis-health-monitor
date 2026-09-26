package com.vitalis.core.domain.energy

import com.vitalis.core.model.EnergyConstants
import kotlin.math.abs

/**
 * ACSM metabolic equations (spec §5.5).
 *
 * A static MET value assumes flat ground, so it badly under-reads a hill climb and
 * over-reads a descent. Once GPS gives speed *and* grade, these equations produce a
 * far better estimate — which is why hiking and trail running lean on them.
 */
object AcsmCalculator {

    /** Below this speed the walking equation applies; above it, the running one. */
    private const val RUN_WALK_THRESHOLD_MPS = 2.2 // ≈ 8 km/h

    /**
     * Walking: `VO2 = (0.1 × S) + (1.8 × S × G) + 3.5`
     *
     * @param speedMetersPerMinute horizontal speed, m/min
     * @param grade incline as a decimal (0.05 = 5%)
     */
    fun walkingVo2(speedMetersPerMinute: Double, grade: Double): Double =
        (0.1 * speedMetersPerMinute) +
            (1.8 * speedMetersPerMinute * grade) +
            EnergyConstants.RESTING_VO2

    /** Running: `VO2 = (0.2 × S) + (0.9 × S × G) + 3.5` */
    fun runningVo2(speedMetersPerMinute: Double, grade: Double): Double =
        (0.2 * speedMetersPerMinute) +
            (0.9 * speedMetersPerMinute * grade) +
            EnergyConstants.RESTING_VO2

    /** `kcal/min = (VO2 × kg) / 200` */
    fun kcalPerMinute(vo2MlPerKgPerMin: Double, weightKg: Double): Double =
        (vo2MlPerKgPerMin * weightKg) / 200.0

    /**
     * Gross energy cost of a GPS segment.
     *
     * Downhill grades are clamped: the equations are only validated for positive
     * grades and, extrapolated far enough downhill, they return a *negative* VO₂,
     * which would mean running downhill generates energy. Steep descents still
     * cost real effort (eccentric braking), so the floor is the flat-ground value
     * scaled down rather than zero.
     */
    fun segmentKcal(
        distanceMeters: Double,
        durationSeconds: Double,
        elevationChangeMeters: Double,
        weightKg: Double,
    ): Double {
        if (distanceMeters <= 0 || durationSeconds <= 0 || weightKg <= 0) return 0.0

        val speedMps = distanceMeters / durationSeconds
        val speedMPerMin = speedMps * 60.0
        val grade = (elevationChangeMeters / distanceMeters).coerceIn(-0.40, 0.40)

        val vo2 = if (speedMps >= RUN_WALK_THRESHOLD_MPS) {
            runningVo2(speedMPerMin, grade.coerceAtLeast(0.0))
        } else {
            walkingVo2(speedMPerMin, grade.coerceAtLeast(0.0))
        }

        // Descending is cheaper than flat but not free — apply a modest discount
        // proportional to steepness instead of the (invalid) negative-grade result.
        val descentFactor = if (grade < 0) (1.0 - abs(grade) * 0.5).coerceAtLeast(0.65) else 1.0
        val adjustedVo2 = (vo2 * descentFactor).coerceAtLeast(EnergyConstants.RESTING_VO2)

        return kcalPerMinute(adjustedVo2, weightKg) * (durationSeconds / 60.0)
    }

    /** Back-converts a VO₂ figure into an equivalent MET, for display alongside the estimate. */
    fun vo2ToMet(vo2MlPerKgPerMin: Double): Double =
        vo2MlPerKgPerMin / EnergyConstants.MET_ML_O2_PER_KG_MIN
}

/**
 * Cycling power model (spec §5.5). Riding energy depends on aerodynamics far more
 * than on distance, so the foot-sport equations do not transfer at all.
 */
object CyclingPowerCalculator {

    const val GRAVITY = 9.80665

    /** Air density at sea level, 15 °C. */
    const val AIR_DENSITY = 1.225

    /** Drag area for a rider on the hoods. */
    const val DEFAULT_CDA = 0.32

    /** Rolling resistance for road tyres on asphalt. */
    const val DEFAULT_CRR = 0.005

    /** Human gross mechanical efficiency — roughly a quarter of intake becomes pedal work. */
    const val EFFICIENCY = 0.24

    /**
     * Mechanical power required to hold [speedMps].
     *
     * @param totalMassKg rider plus bike
     * @param grade incline as a decimal
     */
    fun powerWatts(
        speedMps: Double,
        totalMassKg: Double,
        grade: Double,
        cda: Double = DEFAULT_CDA,
        crr: Double = DEFAULT_CRR,
    ): Double {
        if (speedMps <= 0) return 0.0
        // Small-angle approximation: at cycling gradients sin(θ)≈grade and cos(θ)≈1.
        val gravityPower = totalMassKg * GRAVITY * grade * speedMps
        val rollingPower = crr * totalMassKg * GRAVITY * speedMps
        val aeroPower = 0.5 * AIR_DENSITY * cda * speedMps * speedMps * speedMps
        return (gravityPower + rollingPower + aeroPower).coerceAtLeast(0.0)
    }

    /** `kcal/h = (W × 3.6) / efficiency` */
    fun kcalPerHour(powerWatts: Double): Double = (powerWatts * 3.6) / EFFICIENCY

    fun segmentKcal(
        distanceMeters: Double,
        durationSeconds: Double,
        elevationChangeMeters: Double,
        riderWeightKg: Double,
        bikeWeightKg: Double = 9.0,
    ): Double {
        if (distanceMeters <= 0 || durationSeconds <= 0) return 0.0
        val speedMps = distanceMeters / durationSeconds
        val grade = (elevationChangeMeters / distanceMeters).coerceIn(-0.25, 0.25)
        val watts = powerWatts(speedMps, riderWeightKg + bikeWeightKg, grade)
        return kcalPerHour(watts) * (durationSeconds / 3600.0)
    }
}

/**
 * Heart-rate derived energy (spec §5.6). Preferred over every other model when a
 * chest strap is present, because it measures the body's actual response rather
 * than inferring it from movement.
 */
object HeartRateCalorieCalculator {

    /** Returns kcal/min; may be negative at resting heart rates, so callers clamp. */
    fun kcalPerMinute(
        heartRate: Int,
        weightKg: Double,
        ageYears: Int,
        isMale: Boolean,
    ): Double {
        val raw = if (isMale) {
            -55.0969 + (0.6309 * heartRate) + (0.1988 * weightKg) + (0.2017 * ageYears)
        } else {
            -20.4022 + (0.4472 * heartRate) - (0.1263 * weightKg) + (0.0740 * ageYears)
        }
        return (raw / 4.184).coerceAtLeast(0.0)
    }

    fun grossKcal(
        avgHeartRate: Int,
        durationMinutes: Double,
        weightKg: Double,
        ageYears: Int,
        isMale: Boolean,
    ): Double = kcalPerMinute(avgHeartRate, weightKg, ageYears, isMale) * durationMinutes
}
