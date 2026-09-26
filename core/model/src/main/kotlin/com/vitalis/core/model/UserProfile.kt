package com.vitalis.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.Period

/**
 * The user's identity and goal settings. Everything the energy calculators need
 * to produce a target lives here or in the latest [BodyMeasurement].
 */
data class UserProfile(
    val id: String,
    val displayName: String,
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Float,
    val activityLevel: ActivityLevel,
    val tdeeMode: TdeeMode = TdeeMode.DYNAMIC,
    val goal: Goal,
    /** Absolute rate, e.g. 0.5 means half a kilo per week in the [goal] direction. */
    val goalRateKgPerWeek: Float,
    val targetWeightKg: Float? = null,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val bmiStandard: BmiStandard = BmiStandard.ASIA_PACIFIC,
    val dietPreference: String? = null,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
) {
    fun ageOn(date: LocalDate): Int = Period.between(birthDate, date).years

    /**
     * Under-18 users get information only, never a deficit target (spec §4.1).
     * The same gate switches BMR to Schofield in [BmrFormula].
     */
    fun isMinorOn(date: LocalDate): Boolean = ageOn(date) < 18

    companion object {
        const val DEFAULT_ID: String = "local-user"
    }
}

/** Daily targets derived from the profile — the output of onboarding (spec §4.1). */
data class DailyTargets(
    val bmrKcal: Int,
    val tdeeKcal: Int,
    val targetKcal: Int,
    val macros: MacroTargets,
    val stepsTarget: Int,
    val waterTargetMl: Int,
    /** Null when the goal is maintenance or the user is a minor. */
    val projectedGoalDate: LocalDate? = null,
    val warnings: List<TargetWarning> = emptyList(),
) {
    companion object {
        val EMPTY = DailyTargets(
            bmrKcal = 0,
            tdeeKcal = 0,
            targetKcal = 0,
            macros = MacroTargets.EMPTY,
            stepsTarget = 0,
            waterTargetMl = 0,
        )
    }
}

/**
 * Guardrails from spec §4.4.3. These are surfaced as supportive copy, never as
 * a red scare screen, and [BLOCKING] ones prevent the target from being saved.
 */
enum class TargetWarning(val isBlocking: Boolean) {
    DEFICIT_EXCEEDS_25_PERCENT(false),
    BELOW_CALORIE_FLOOR(false),
    TARGET_BMI_UNDERWEIGHT(false),
    TARGET_BMI_UNSAFE(true),
    MINOR_INFORMATIONAL_ONLY(false),
    BMI_OVER_35_ESTIMATE_MAY_BE_HIGH(false),
}
