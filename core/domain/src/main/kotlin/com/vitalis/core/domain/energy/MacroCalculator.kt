package com.vitalis.core.domain.energy

import com.vitalis.core.model.AtwaterFactors
import com.vitalis.core.model.Goal
import com.vitalis.core.model.MacroTargets
import kotlin.math.roundToInt

/**
 * Macro split (spec §5.3).
 *
 * Protein and fat are anchored to body weight first, and carbohydrate takes
 * whatever energy is left. That ordering matters in a deficit: protein protects
 * lean mass and fat has a hormonal floor, so neither should be the variable that
 * absorbs a calorie cut.
 */
object MacroCalculator {

    /** Grams of protein per kg of body weight, by goal. */
    private fun proteinPerKg(goal: Goal): Double = when (goal) {
        Goal.LOSE -> 2.0
        Goal.MAINTAIN -> 1.6
        Goal.GAIN -> 1.8
    }

    /** Fat floor, as g/kg for fat loss or as a fraction of energy otherwise. */
    private const val FAT_G_PER_KG_LOSS = 0.8
    private const val FAT_FRACTION_MAINTAIN = 0.275
    private const val FAT_FRACTION_GAIN = 0.25

    fun calculate(
        targetKcal: Int,
        weightKg: Double,
        goal: Goal,
        /** Lean mass is the better anchor when it is known — fat mass needs no feeding. */
        leanBodyMassKg: Double? = null,
    ): MacroTargets {
        if (targetKcal <= 0 || weightKg <= 0) return MacroTargets.EMPTY

        val proteinAnchorKg = leanBodyMassKg?.takeIf { it > 0 } ?: weightKg
        val proteinG = (proteinAnchorKg * proteinPerKg(goal)).roundToInt()

        val fatG = when (goal) {
            Goal.LOSE -> (weightKg * FAT_G_PER_KG_LOSS)
            Goal.MAINTAIN -> (targetKcal * FAT_FRACTION_MAINTAIN) / AtwaterFactors.KCAL_PER_G_FAT
            Goal.GAIN -> (targetKcal * FAT_FRACTION_GAIN) / AtwaterFactors.KCAL_PER_G_FAT
        }.roundToInt()

        val proteinKcal = proteinG * AtwaterFactors.KCAL_PER_G_PROTEIN
        val fatKcal = fatG * AtwaterFactors.KCAL_PER_G_FAT
        val carbsG = ((targetKcal - proteinKcal - fatKcal) / AtwaterFactors.KCAL_PER_G_CARBS)
            .coerceAtLeast(0.0)
            .roundToInt()

        return MacroTargets(proteinG = proteinG, carbsG = carbsG, fatG = fatG)
    }

    /** Water target: `kg × 30 ml`, rounded to the nearest 250 ml glass (spec §4.2.4). */
    fun waterTargetMl(weightKg: Double): Int {
        if (weightKg <= 0) return 0
        val raw = weightKg * 30.0
        return ((raw / 250.0).roundToInt() * 250).coerceAtLeast(1000)
    }

    /** Step target, nudged by goal. 8 000 is a defensible floor; 10 000 is folklore, not science. */
    fun stepTarget(goal: Goal): Int = when (goal) {
        Goal.LOSE -> 10_000
        Goal.MAINTAIN -> 8_000
        Goal.GAIN -> 7_000
    }
}
