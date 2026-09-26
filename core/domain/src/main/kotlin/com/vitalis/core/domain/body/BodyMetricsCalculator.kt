package com.vitalis.core.domain.body

import com.vitalis.core.model.BmiCategory
import com.vitalis.core.model.BmiStandard
import com.vitalis.core.model.BodyMeasurement
import com.vitalis.core.model.BodyMetrics
import com.vitalis.core.model.Sex
import kotlin.math.roundToInt

/**
 * Body composition maths (spec §5.7).
 *
 * Estimates are always marked as estimates: [BodyMetrics.isBodyFatEstimated] tells
 * the UI to caveat a Deurenberg-derived figure, which can be several points off for
 * an individual even though it tracks well across a population.
 */
object BodyMetricsCalculator {

    fun bmi(weightKg: Double, heightCm: Double): Double {
        if (weightKg <= 0 || heightCm <= 0) return 0.0
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    /**
     * Deurenberg BMI-based body fat estimate.
     * `BF% = 1.20 × BMI + 0.23 × age − 10.8 × sex − 5.4`, sex = 1 male / 0 female.
     */
    fun estimateBodyFatPercent(bmi: Double, ageYears: Int, sex: Sex): Double {
        if (bmi <= 0) return 0.0
        val sexTerm = if (sex == Sex.MALE) 1.0 else 0.0
        return ((1.20 * bmi) + (0.23 * ageYears) - (10.8 * sexTerm) - 5.4).coerceIn(2.0, 70.0)
    }

    fun leanBodyMassKg(weightKg: Double, bodyFatPercent: Double): Double =
        weightKg * (1.0 - bodyFatPercent / 100.0)

    fun fatMassKg(weightKg: Double, bodyFatPercent: Double): Double =
        weightKg * (bodyFatPercent / 100.0)

    /** Waist-to-height; under 0.5 is the healthy range and it beats BMI for visceral risk. */
    fun waistToHeightRatio(waistCm: Double, heightCm: Double): Double =
        if (heightCm <= 0) 0.0 else waistCm / heightCm

    fun waistToHipRatio(waistCm: Double, hipCm: Double): Double =
        if (hipCm <= 0) 0.0 else waistCm / hipCm

    /** Devine ideal body weight — a reference point, not a goal the app pushes. */
    fun idealWeightKg(heightCm: Double, sex: Sex): Double {
        val heightInches = heightCm / 2.54
        val base = if (sex == Sex.MALE) 50.0 else 45.5
        return (base + 2.3 * (heightInches - 60.0)).coerceAtLeast(30.0)
    }

    fun classifyBmi(bmi: Double, standard: BmiStandard): BmiCategory = when (standard) {
        BmiStandard.WHO -> when {
            bmi < 18.5 -> BmiCategory.UNDERWEIGHT
            bmi < 25.0 -> BmiCategory.NORMAL
            bmi < 30.0 -> BmiCategory.OVERWEIGHT
            bmi < 35.0 -> BmiCategory.OBESE_I
            else -> BmiCategory.OBESE_II
        }
        // Asia-Pacific cut-offs run lower; cardiometabolic risk rises earlier in
        // these populations, so the WHO bands under-flag Indonesian users (spec §5.7).
        BmiStandard.ASIA_PACIFIC -> when {
            bmi < 18.5 -> BmiCategory.UNDERWEIGHT
            bmi < 23.0 -> BmiCategory.NORMAL
            bmi < 25.0 -> BmiCategory.OVERWEIGHT
            bmi < 30.0 -> BmiCategory.OBESE_I
            else -> BmiCategory.OBESE_II
        }
    }

    /** Assembles everything derivable from one measurement plus the profile. */
    fun derive(
        measurement: BodyMeasurement,
        heightCm: Double,
        ageYears: Int,
        sex: Sex,
        standard: BmiStandard,
    ): BodyMetrics {
        val weight = measurement.weightKg?.toDouble() ?: return BodyMetrics.EMPTY
        val bmi = bmi(weight, heightCm)

        val measuredBf = measurement.bodyFatPercent?.toDouble()
        val bodyFat = measuredBf ?: estimateBodyFatPercent(bmi, ageYears, sex)

        return BodyMetrics(
            bmi = bmi.toFloat(),
            bmiCategory = classifyBmi(bmi, standard),
            bodyFatPercent = bodyFat.toFloat(),
            isBodyFatEstimated = measuredBf == null,
            leanBodyMassKg = leanBodyMassKg(weight, bodyFat).toFloat(),
            fatMassKg = fatMassKg(weight, bodyFat).toFloat(),
            waistToHeightRatio = measurement.waistCm
                ?.let { waistToHeightRatio(it.toDouble(), heightCm).toFloat() },
            waistToHipRatio = measurement.waistCm
                ?.let { waist -> measurement.hipCm?.let { hip -> waistToHipRatio(waist.toDouble(), hip.toDouble()).toFloat() } },
            idealWeightKg = idealWeightKg(heightCm, sex).toFloat(),
        )
    }

    /**
     * Exponentially smoothed weight trend.
     *
     * Daily scale readings swing a kilo or more on water alone, which is enough to
     * make real progress invisible and to make a good week feel like a bad one.
     * [alpha] of 0.1 gives roughly a 10-day effective window.
     */
    fun smoothTrend(weights: List<Float>, alpha: Float = 0.1f): List<Float> {
        if (weights.isEmpty()) return emptyList()
        val out = ArrayList<Float>(weights.size)
        var trend = weights.first()
        weights.forEach { w ->
            trend += alpha * (w - trend)
            out += trend
        }
        return out
    }

    /** Rounds a display weight to one decimal, the precision a bathroom scale actually has. */
    fun displayWeight(weightKg: Float): Float = (weightKg * 10f).roundToInt() / 10f
}
