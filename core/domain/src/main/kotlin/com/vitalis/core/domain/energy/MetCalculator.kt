package com.vitalis.core.domain.energy

import com.vitalis.core.model.CalorieEstimate
import com.vitalis.core.model.CalorieModel
import com.vitalis.core.model.EnergyConstants
import kotlin.math.roundToInt

/**
 * MET-based energy cost (spec §5.4) and the net-vs-gross correction (spec §4.3.3).
 *
 * The net correction is the single most important calculation in the app. A MET
 * figure is *gross* — it includes the calories the body would have burnt lying on
 * the sofa for the same hour. Because the daily target already budgets 24 hours of
 * BMR, adding gross workout calories to the budget grants the user the same
 * resting energy twice, which is how people "eat back" a deficit that never
 * existed. Everything user-facing shows gross (it is the familiar number), but
 * only [CalorieEstimate.netKcal] is ever added to the budget.
 */
object MetCalculator {

    /** `kcal/min = (MET × 3.5 × kg) / 200` — the Compendium's own formulation. */
    fun kcalPerMinute(met: Double, weightKg: Double): Double =
        (met * EnergyConstants.MET_ML_O2_PER_KG_MIN * weightKg) / 200.0

    fun grossKcal(met: Double, weightKg: Double, durationMinutes: Double): Double =
        kcalPerMinute(met, weightKg) * durationMinutes

    /**
     * Resting energy over the same window, prorated from daily BMR. This is what
     * gets subtracted so the workout is not double-counted.
     */
    fun restingKcalDuring(bmrKcal: Int, durationMinutes: Double): Double =
        (bmrKcal / EnergyConstants.MINUTES_PER_DAY) * durationMinutes

    /**
     * @param met activity MET value
     * @param weightKg body weight
     * @param durationMinutes exercise duration
     * @param bmrKcal the user's daily BMR, used for the net correction
     */
    fun estimate(
        met: Double,
        weightKg: Double,
        durationMinutes: Double,
        bmrKcal: Int,
    ): CalorieEstimate {
        val gross = grossKcal(met, weightKg, durationMinutes)
        val resting = restingKcalDuring(bmrKcal, durationMinutes)
        return CalorieEstimate(
            grossKcal = gross.roundToInt(),
            // Never negative: a very light activity can score below resting, and a
            // negative "burn" would silently shrink the day's budget.
            netKcal = (gross - resting).coerceAtLeast(0.0).roundToInt(),
            model = CalorieModel.MET_STATIC,
            metValue = met,
        )
    }

    /** Converts an already-computed gross figure (e.g. from ACSM or HR) into an estimate. */
    fun toNetEstimate(
        grossKcal: Double,
        durationMinutes: Double,
        bmrKcal: Int,
        model: CalorieModel,
        met: Double? = null,
    ): CalorieEstimate {
        val resting = restingKcalDuring(bmrKcal, durationMinutes)
        return CalorieEstimate(
            grossKcal = grossKcal.roundToInt(),
            netKcal = (grossKcal - resting).coerceAtLeast(0.0).roundToInt(),
            model = model,
            metValue = met,
        )
    }
}
