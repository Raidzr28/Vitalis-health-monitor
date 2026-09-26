package com.vitalis.core.domain.energy

import com.vitalis.core.domain.body.BodyMetricsCalculator
import com.vitalis.core.model.DailyTargets
import com.vitalis.core.model.EnergyConstants
import com.vitalis.core.model.Goal
import com.vitalis.core.model.Sex
import com.vitalis.core.model.TargetWarning
import com.vitalis.core.model.UserProfile
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Turns a profile into the day's numbers — the payload of onboarding (spec §4.1).
 *
 * This is also where the health guardrails live (spec §4.4.3). They are applied
 * here rather than in the UI so that no screen, present or future, can route
 * around them: the returned target is *already* clamped, and the warnings only
 * explain what happened.
 */
object DailyTargetCalculator {

    fun calculate(
        profile: UserProfile,
        weightKg: Double,
        bodyFatPercent: Double? = null,
        today: LocalDate = LocalDate.now(),
    ): DailyTargets {
        val age = profile.ageOn(today)
        val warnings = mutableListOf<TargetWarning>()

        val bmr = BmrCalculator.calculate(
            sex = profile.sex,
            weightKg = weightKg,
            heightCm = profile.heightCm.toDouble(),
            ageYears = age,
            bodyFatPercent = bodyFatPercent,
        )
        if (bmr.mayOverestimate) warnings += TargetWarning.BMI_OVER_35_ESTIMATE_MAY_BE_HIGH

        val tdee = TdeeCalculator.static(bmr.bmrKcal, profile.activityLevel)

        // Minors get information, never a deficit (spec §4.1).
        if (profile.isMinorOn(today)) {
            warnings += TargetWarning.MINOR_INFORMATIONAL_ONLY
            return build(profile, weightKg, bodyFatPercent, bmr.bmrKcal, tdee, tdee, null, warnings)
        }

        val requestedDelta = dailyDeltaKcal(profile.goalRateKgPerWeek.toDouble())
        val signedDelta = when (profile.goal) {
            Goal.LOSE -> -requestedDelta
            Goal.GAIN -> +requestedDelta
            Goal.MAINTAIN -> 0.0
        }

        // Guardrail 1 — cap the deficit at 25% of TDEE.
        val maxDeficit = tdee * EnergyConstants.MAX_DEFICIT_FRACTION
        var appliedDelta = signedDelta
        if (profile.goal == Goal.LOSE && abs(signedDelta) > maxDeficit) {
            warnings += TargetWarning.DEFICIT_EXCEEDS_25_PERCENT
            appliedDelta = -maxDeficit
        }

        // Guardrail 2 — never recommend below the absolute calorie floor.
        val floor = when (profile.sex) {
            Sex.MALE -> EnergyConstants.CALORIE_FLOOR_MALE
            Sex.FEMALE -> EnergyConstants.CALORIE_FLOOR_FEMALE
        }
        var targetKcal = (tdee + appliedDelta).roundToInt()
        if (targetKcal < floor) {
            warnings += TargetWarning.BELOW_CALORIE_FLOOR
            targetKcal = floor
        }

        // Guardrail 3 — refuse goal weights that land in an unhealthy BMI band.
        profile.targetWeightKg?.let { goalWeight ->
            val targetBmi = BodyMetricsCalculator.bmi(goalWeight.toDouble(), profile.heightCm.toDouble())
            when {
                targetBmi < 17.5 -> warnings += TargetWarning.TARGET_BMI_UNSAFE
                targetBmi < 18.5 -> warnings += TargetWarning.TARGET_BMI_UNDERWEIGHT
            }
        }

        val projectedDate = projectGoalDate(
            currentWeightKg = weightKg,
            targetWeightKg = profile.targetWeightKg?.toDouble(),
            // Re-derive the achievable rate from the *clamped* target, so the
            // projection reflects reality rather than what the user asked for.
            actualDailyDeltaKcal = (targetKcal - tdee).toDouble(),
            today = today,
        )

        return build(profile, weightKg, bodyFatPercent, bmr.bmrKcal, tdee, targetKcal, projectedDate, warnings)
    }

    private fun build(
        profile: UserProfile,
        weightKg: Double,
        bodyFatPercent: Double?,
        bmrKcal: Int,
        tdeeKcal: Int,
        targetKcal: Int,
        projectedGoalDate: LocalDate?,
        warnings: List<TargetWarning>,
    ): DailyTargets {
        val leanMass = bodyFatPercent?.let { weightKg * (1 - it / 100.0) }
        return DailyTargets(
            bmrKcal = bmrKcal,
            tdeeKcal = tdeeKcal,
            targetKcal = targetKcal,
            macros = MacroCalculator.calculate(targetKcal, weightKg, profile.goal, leanMass),
            stepsTarget = MacroCalculator.stepTarget(profile.goal),
            waterTargetMl = MacroCalculator.waterTargetMl(weightKg),
            projectedGoalDate = projectedGoalDate,
            warnings = warnings.distinct(),
        )
    }

    /** `kg/week × 7700 / 7` — the daily energy gap implied by a weekly rate. */
    fun dailyDeltaKcal(kgPerWeek: Double): Double =
        abs(kgPerWeek) * EnergyConstants.KCAL_PER_KG_FAT / 7.0

    /**
     * Straight-line projection to the goal weight. Deliberately simple: it is a
     * motivational estimate, and dressing it up with metabolic adaptation curves
     * would imply a precision the inputs do not support.
     */
    fun projectGoalDate(
        currentWeightKg: Double,
        targetWeightKg: Double?,
        actualDailyDeltaKcal: Double,
        today: LocalDate,
    ): LocalDate? {
        if (targetWeightKg == null) return null
        val kgToChange = targetWeightKg - currentWeightKg
        if (abs(kgToChange) < 0.1) return today
        // No progress is possible if the target has no energy gap, or the gap
        // points the wrong way (e.g. a surplus while trying to lose).
        if (actualDailyDeltaKcal == 0.0) return null
        if (kgToChange > 0 != actualDailyDeltaKcal > 0) return null

        val kgPerDay = abs(actualDailyDeltaKcal) / EnergyConstants.KCAL_PER_KG_FAT
        val days = ceil(abs(kgToChange) / kgPerDay).toLong()
        return if (days > 3650) null else today.plusDays(days)
    }
}
