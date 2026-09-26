package com.vitalis.core.domain.health

import com.vitalis.core.model.HealthPillar
import com.vitalis.core.model.HealthScore
import com.vitalis.core.model.PillarScore
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Composite health score (spec §4.4.2).
 *
 * Two design rules shape the maths. First, **absent data is not a zero**: a user
 * who has never worn a heart-rate monitor should not be told their cardio fitness
 * is terrible, so pillars without data are excluded and the remaining weights are
 * renormalised. Second, the output is meant to be read as a *trend*, so the score
 * is smooth rather than banded — a single day's slip should nudge it, not cliff it.
 *
 * This is not a diagnostic. The UI must always carry the medical disclaimer.
 */
object HealthScoreCalculator {

    data class Inputs(
        // Body composition
        val bmi: Double? = null,
        val waistToHeightRatio: Double? = null,
        val bodyFatPercent: Double? = null,
        val isMale: Boolean = true,
        // Cardio
        val restingHeartRate: Int? = null,
        val vo2Max: Double? = null,
        val ageYears: Int = 30,
        // Activity
        val weeklyActiveMinutes: Int? = null,
        val avgDailySteps: Int? = null,
        val stepTarget: Int = 8_000,
        // Nutrition
        val daysLoggedThisWeek: Int? = null,
        val proteinTargetHitDays: Int? = null,
        val fiberGramsPerDay: Double? = null,
        val waterTargetHitDays: Int? = null,
        // Recovery
        val avgSleepMinutes: Int? = null,
        val sleepConsistencyMinutes: Int? = null,
        val restDaysThisWeek: Int? = null,
    )

    fun calculate(inputs: Inputs, previousTotal: Int? = null): HealthScore {
        val pillars = listOf(
            scoreBodyComposition(inputs),
            scoreCardioFitness(inputs),
            scoreActivity(inputs),
            scoreNutrition(inputs),
            scoreRecovery(inputs),
        )

        // Renormalise over the pillars that actually have data.
        val availableWeight = pillars.filter { it.hasData }.sumOf { it.pillar.weight }
        val total = if (availableWeight <= 0.0) {
            0
        } else {
            (pillars.filter { it.hasData }.sumOf { it.weightedContribution } / availableWeight).roundToInt()
        }

        return HealthScore(
            total = total.coerceIn(0, 100),
            pillars = pillars,
            previousTotal = previousTotal,
        )
    }

    private fun scoreBodyComposition(inputs: Inputs): PillarScore {
        val parts = mutableListOf<Double>()

        // BMI: full marks across the healthy band, tapering either side.
        inputs.bmi?.let { bmi ->
            parts += bandScore(bmi, ideal = 18.5..24.9, tolerance = 6.0)
        }
        // WHtR: 0.5 is the accepted ceiling; below 0.4 is not better, so it is a band too.
        inputs.waistToHeightRatio?.let { whtr ->
            parts += bandScore(whtr, ideal = 0.40..0.50, tolerance = 0.12)
        }
        inputs.bodyFatPercent?.let { bf ->
            val ideal = if (inputs.isMale) 10.0..20.0 else 18.0..28.0
            parts += bandScore(bf, ideal, tolerance = 10.0)
        }

        return PillarScore(
            pillar = HealthPillar.BODY_COMPOSITION,
            score = parts.averageOrNull()?.roundToInt(),
            hasData = parts.isNotEmpty(),
        )
    }

    private fun scoreCardioFitness(inputs: Inputs): PillarScore {
        val parts = mutableListOf<Double>()

        // Lower resting heart rate is better, down to a trained-athlete floor.
        inputs.restingHeartRate?.let { rhr ->
            parts += when {
                rhr <= 50 -> 100.0
                rhr >= 90 -> 20.0
                else -> 100.0 - ((rhr - 50) / 40.0) * 80.0
            }
        }
        // VO2max thresholds shift with age, so score against an age-adjusted target.
        inputs.vo2Max?.let { vo2 ->
            val target = if (inputs.isMale) 48.0 - (inputs.ageYears - 30) * 0.35
            else 42.0 - (inputs.ageYears - 30) * 0.35
            parts += ((vo2 / target.coerceAtLeast(25.0)) * 80.0).coerceIn(10.0, 100.0)
        }

        return PillarScore(
            pillar = HealthPillar.CARDIO_FITNESS,
            score = parts.averageOrNull()?.roundToInt(),
            hasData = parts.isNotEmpty(),
        )
    }

    private fun scoreActivity(inputs: Inputs): PillarScore {
        val parts = mutableListOf<Double>()

        // WHO recommends 150 minutes of moderate activity per week.
        inputs.weeklyActiveMinutes?.let { minutes ->
            parts += ((minutes / 150.0) * 100.0).coerceIn(0.0, 100.0)
        }
        inputs.avgDailySteps?.let { steps ->
            parts += ((steps.toDouble() / inputs.stepTarget) * 100.0).coerceIn(0.0, 100.0)
        }

        return PillarScore(
            pillar = HealthPillar.ACTIVITY,
            score = parts.averageOrNull()?.roundToInt(),
            hasData = parts.isNotEmpty(),
        )
    }

    private fun scoreNutrition(inputs: Inputs): PillarScore {
        val parts = mutableListOf<Double>()

        inputs.daysLoggedThisWeek?.let { parts += ((it / 7.0) * 100.0).coerceIn(0.0, 100.0) }
        inputs.proteinTargetHitDays?.let { parts += ((it / 7.0) * 100.0).coerceIn(0.0, 100.0) }
        inputs.waterTargetHitDays?.let { parts += ((it / 7.0) * 100.0).coerceIn(0.0, 100.0) }
        // 30 g of fibre a day is the commonly cited adequate intake.
        inputs.fiberGramsPerDay?.let { parts += ((it / 30.0) * 100.0).coerceIn(0.0, 100.0) }

        return PillarScore(
            pillar = HealthPillar.NUTRITION,
            score = parts.averageOrNull()?.roundToInt(),
            hasData = parts.isNotEmpty(),
        )
    }

    private fun scoreRecovery(inputs: Inputs): PillarScore {
        val parts = mutableListOf<Double>()

        // 7–9 hours. Oversleeping is not scored as better than the band.
        inputs.avgSleepMinutes?.let { minutes ->
            parts += bandScore(minutes.toDouble(), ideal = 420.0..540.0, tolerance = 120.0)
        }
        // Consistency matters nearly as much as duration.
        inputs.sleepConsistencyMinutes?.let { variance ->
            parts += (100.0 - (variance / 90.0) * 100.0).coerceIn(0.0, 100.0)
        }
        // One or two rest days a week; zero is under-recovery, five is inactivity.
        inputs.restDaysThisWeek?.let { days ->
            parts += bandScore(days.toDouble(), ideal = 1.0..2.0, tolerance = 3.0)
        }

        return PillarScore(
            pillar = HealthPillar.RECOVERY,
            score = parts.averageOrNull()?.roundToInt(),
            hasData = parts.isNotEmpty(),
        )
    }

    /**
     * 100 inside [ideal], falling linearly to 0 once [tolerance] past either edge.
     * Used wherever both too little and too much are worse than the middle.
     */
    private fun bandScore(value: Double, ideal: ClosedFloatingPointRange<Double>, tolerance: Double): Double {
        if (value in ideal) return 100.0
        val distance = if (value < ideal.start) ideal.start - value else value - ideal.endInclusive
        return (100.0 * (1.0 - abs(distance) / tolerance)).coerceIn(0.0, 100.0)
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
}
