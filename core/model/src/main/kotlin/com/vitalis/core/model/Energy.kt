package com.vitalis.core.model

/**
 * The single most important widget in the app (spec §1.3):
 * `Intake − (BMR + NEAT + Exercise) = Net Balance`.
 *
 * [burnedNetKcal] is deliberately *net* of resting metabolism, because the BMR
 * covered by the 24-hour target already accounts for the calories the user would
 * have burnt sitting still during their workout. Adding gross exercise calories
 * to the budget is the classic double-count (spec §4.3.3).
 */
data class EnergyBudget(
    val targetKcal: Int = 0,
    val consumedKcal: Int = 0,
    val burnedNetKcal: Int = 0,
    val bmrKcal: Int = 0,
    val neatKcal: Int = 0,
) {
    /** What the user may still eat today. Can go negative — that is the point. */
    val remainingKcal: Int get() = targetKcal - consumedKcal + burnedNetKcal

    /** Fraction of the budget eaten. Allowed above 1.0 so the ring can show overshoot. */
    val progress: Float
        get() = if (targetKcal <= 0) 0f else (consumedKcal.toFloat() / targetKcal).coerceIn(0f, 1.5f)

    val isOverBudget: Boolean get() = remainingKcal < 0

    companion object {
        val EMPTY = EnergyBudget()
    }
}

/**
 * `TDEE = BMR + TEF + NEAT + EAT` (spec §4.3), kept split so the UI can explain
 * where the number came from rather than presenting one opaque figure.
 */
data class EnergyExpenditure(
    val bmrKcal: Int = 0,
    /** Thermic effect of food — roughly 10% of intake. */
    val tefKcal: Int = 0,
    /** Non-exercise activity, derived from the step counter. */
    val neatKcal: Int = 0,
    /** Exercise activity, gross. */
    val eatGrossKcal: Int = 0,
    /** Exercise activity with the overlapping resting burn removed. */
    val eatNetKcal: Int = 0,
) {
    val totalKcal: Int get() = bmrKcal + tefKcal + neatKcal + eatNetKcal

    companion object {
        val EMPTY = EnergyExpenditure()
    }
}

/** Result of a MET or biomechanical calorie computation. */
data class CalorieEstimate(
    val grossKcal: Int,
    val netKcal: Int,
    val model: CalorieModel,
    val metValue: Double? = null,
)

/** Reference constants used across the energy calculators. */
object EnergyConstants {
    /** Energy in a kilogram of body fat (spec §5.3). */
    const val KCAL_PER_KG_FAT = 7700.0

    /** Deficits beyond this fraction of TDEE trigger a guardrail (spec §4.4.3). */
    const val MAX_DEFICIT_FRACTION = 0.25

    /** Absolute floors below which the app stops recommending a deficit (spec §4.1). */
    const val CALORIE_FLOOR_MALE = 1500
    const val CALORIE_FLOOR_FEMALE = 1200

    /** 1 MET ≈ 3.5 ml O₂/kg/min (spec §5.4). */
    const val MET_ML_O2_PER_KG_MIN = 3.5

    /** Resting oxygen uptake used by the ACSM equations. */
    const val RESTING_VO2 = 3.5

    const val TEF_FRACTION = 0.10

    /** Minutes in a day, for prorating BMR across a workout. */
    const val MINUTES_PER_DAY = 1440.0
}
