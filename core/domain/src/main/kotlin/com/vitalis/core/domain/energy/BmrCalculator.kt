package com.vitalis.core.domain.energy

import com.vitalis.core.model.BmrFormula
import com.vitalis.core.model.Sex
import kotlin.math.roundToInt

/**
 * Basal metabolic rate (spec §5.1).
 *
 * Formula selection matters more than people expect:
 * - body fat known → Katch-McArdle, which scales off lean mass and is far better
 *   for muscular users that Mifflin systematically under-reads;
 * - BMI > 35 → Mifflin still, but flagged, because it over-reads at high adiposity
 *   (it cannot tell fat mass from lean mass);
 * - under 19 → Schofield/FAO-WHO-UNU, and no deficit advice at all.
 */
object BmrCalculator {

    data class Result(
        val bmrKcal: Int,
        val formula: BmrFormula,
        /** True when the estimate is likely high because BMI exceeds 35. */
        val mayOverestimate: Boolean = false,
    )

    fun calculate(
        sex: Sex,
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        bodyFatPercent: Double? = null,
    ): Result {
        require(weightKg > 0) { "weightKg must be positive" }
        require(heightCm > 0) { "heightCm must be positive" }
        require(ageYears >= 0) { "ageYears must not be negative" }

        val bmi = weightKg / (heightCm / 100.0).let { it * it }

        return when {
            ageYears < 19 -> Result(
                bmrKcal = schofield(sex, weightKg, heightCm, ageYears).roundToInt(),
                formula = BmrFormula.SCHOFIELD,
            )

            bodyFatPercent != null && bodyFatPercent in 3.0..70.0 -> Result(
                bmrKcal = katchMcArdle(weightKg, bodyFatPercent).roundToInt(),
                formula = BmrFormula.KATCH_MCARDLE,
            )

            else -> Result(
                bmrKcal = mifflinStJeor(sex, weightKg, heightCm, ageYears).roundToInt(),
                formula = BmrFormula.MIFFLIN_ST_JEOR,
                mayOverestimate = bmi > 35.0,
            )
        }
    }

    /** `(10 × kg) + (6.25 × cm) − (5 × age) + s`, s = +5 male / −161 female. */
    fun mifflinStJeor(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int): Double {
        val base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * ageYears)
        return base + if (sex == Sex.MALE) 5.0 else -161.0
    }

    /** `370 + 21.6 × LBM` — sex-independent, because lean mass already carries that signal. */
    fun katchMcArdle(weightKg: Double, bodyFatPercent: Double): Double {
        val leanBodyMass = weightKg * (1.0 - bodyFatPercent / 100.0)
        return 370.0 + (21.6 * leanBodyMass)
    }

    /**
     * Schofield (FAO/WHO/UNU) weight-and-height equations for under-19s.
     * Children's metabolism does not follow the adult equations, and the app must
     * never hand a growing body a deficit target.
     */
    fun schofield(sex: Sex, weightKg: Double, heightCm: Double, ageYears: Int): Double {
        val heightM = heightCm / 100.0
        return when {
            ageYears < 3 -> if (sex == Sex.MALE) 0.167 * weightKg + 1517.4 * heightM - 617.6
            else 16.252 * weightKg + 1023.2 * heightM - 413.5

            ageYears < 10 -> if (sex == Sex.MALE) 19.59 * weightKg + 130.3 * heightM + 414.9
            else 16.969 * weightKg + 161.8 * heightM + 371.2

            else -> if (sex == Sex.MALE) 16.25 * weightKg + 137.2 * heightM + 515.5
            else 8.365 * weightKg + 465.0 * heightM + 200.0
        }
    }
}
