package com.vitalis.core.domain.energy

import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.EnergyConstants
import com.vitalis.core.model.EnergyExpenditure
import com.vitalis.core.model.Sex
import kotlin.math.roundToInt

/**
 * Total daily energy expenditure (spec §4.3, §5.2).
 *
 * The static path (`BMR × activity factor`) is what onboarding uses on day one,
 * before any behavioural data exists. From then on the dynamic path is preferred:
 * it rebuilds the number each day from real steps and real workouts, which both
 * responds faster and — crucially — avoids counting a logged run twice, once
 * inside the "very active" multiplier and once as a logged activity.
 */
object TdeeCalculator {

    /** `TDEE = BMR × factor`. Used until there is enough data for [dynamic]. */
    fun static(bmrKcal: Int, activityLevel: ActivityLevel): Int =
        (bmrKcal * activityLevel.factor).roundToInt()

    /**
     * `BMR × 1.2 + TEF + NEAT + net EAT`.
     *
     * The 1.2 baseline covers unavoidable non-exercise movement that the step
     * counter cannot see (fidgeting, standing, posture). [neatKcal] adds measured
     * steps on top, and [eatNetKcal] must already have the resting overlap removed.
     */
    fun dynamic(
        bmrKcal: Int,
        neatKcal: Int = 0,
        eatNetKcal: Int = 0,
        intakeKcal: Int = 0,
        includeTef: Boolean = true,
    ): EnergyExpenditure {
        val sedentaryBase = (bmrKcal * ActivityLevel.SEDENTARY.factor).roundToInt()
        val tef = if (includeTef) (intakeKcal * EnergyConstants.TEF_FRACTION).roundToInt() else 0
        return EnergyExpenditure(
            bmrKcal = sedentaryBase,
            tefKcal = tef,
            neatKcal = neatKcal,
            eatGrossKcal = eatNetKcal,
            eatNetKcal = eatNetKcal,
        )
    }
}

/**
 * Turns step counts into calories (spec §4.3.1).
 *
 * Stride length is estimated from height with the standard 0.415/0.413 coefficients
 * rather than asked for, because almost nobody knows their stride length and a
 * wrong self-report is worse than the estimate.
 */
object NeatCalculator {

    fun strideLengthM(heightCm: Double, sex: Sex): Double =
        heightCm * (if (sex == Sex.MALE) 0.415 else 0.413) / 100.0

    fun distanceMetersFromSteps(steps: Int, heightCm: Double, sex: Sex): Double =
        steps * strideLengthM(heightCm, sex)

    /**
     * Walking energy cost, net of rest.
     *
     * Uses ~0.5 kcal per kg per km — the widely used net walking cost — instead of
     * a flat MET, since steps accumulate all day and a gross figure here would
     * inflate the budget substantially.
     */
    fun kcalFromSteps(steps: Int, weightKg: Double, heightCm: Double, sex: Sex): Int {
        if (steps <= 0 || weightKg <= 0) return 0
        val km = distanceMetersFromSteps(steps, heightCm, sex) / 1000.0
        return (km * weightKg * 0.5).roundToInt()
    }
}
