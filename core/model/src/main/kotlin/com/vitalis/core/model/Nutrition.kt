package com.vitalis.core.model

import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/** Atlas-value energy densities (spec §5.3). */
object AtwaterFactors {
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_CARBS = 4.0
    const val KCAL_PER_G_FAT = 9.0
    const val KCAL_PER_G_ALCOHOL = 7.0
}

/**
 * A food in the catalogue. Nutrition is stored per 100 g so any serving size can
 * be derived without re-fetching, and [source] is always shown to the user
 * because crowdsourced entries vary wildly in quality (spec §4.2.5).
 */
data class FoodItem(
    val id: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val source: FoodSource,
    val servingSizeG: Float,
    val servingLabel: String,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float? = null,
    val sugarPer100g: Float? = null,
    val satFatPer100g: Float? = null,
    val sodiumMgPer100g: Float? = null,
    val cholesterolMgPer100g: Float? = null,
    val potassiumMgPer100g: Float? = null,
    /** True only for lab-verified rows (USDA) or entries the user corrected themselves. */
    val isVerified: Boolean = false,
    val usageCount: Int = 0,
    val updatedAt: Instant = Instant.EPOCH,
) {
    /** Nutrition for [grams] of this food. */
    fun nutritionFor(grams: Float): Macros = Macros(
        kcal = kcalPer100g * grams / 100f,
        proteinG = proteinPer100g * grams / 100f,
        carbsG = carbsPer100g * grams / 100f,
        fatG = fatPer100g * grams / 100f,
    )

    /** Nutrition for [servings] of the food's own serving unit ("1 piring", "1 botol"). */
    fun nutritionForServings(servings: Float): Macros = nutritionFor(servingSizeG * servings)
}

/** The four numbers every screen in the app ultimately shows. */
data class Macros(
    val kcal: Float = 0f,
    val proteinG: Float = 0f,
    val carbsG: Float = 0f,
    val fatG: Float = 0f,
) {
    operator fun plus(other: Macros) = Macros(
        kcal = kcal + other.kcal,
        proteinG = proteinG + other.proteinG,
        carbsG = carbsG + other.carbsG,
        fatG = fatG + other.fatG,
    )

    operator fun times(factor: Float) = Macros(
        kcal = kcal * factor,
        proteinG = proteinG * factor,
        carbsG = carbsG * factor,
        fatG = fatG * factor,
    )

    /**
     * Energy implied by the macros. Diverges from [kcal] on crowdsourced entries;
     * a large gap is a useful signal that a row is untrustworthy.
     */
    val kcalFromMacros: Float
        get() = (
            proteinG * AtwaterFactors.KCAL_PER_G_PROTEIN +
                carbsG * AtwaterFactors.KCAL_PER_G_CARBS +
                fatG * AtwaterFactors.KCAL_PER_G_FAT
            ).toFloat()

    companion object {
        val EMPTY = Macros()
    }
}

/**
 * One diary line. Macros are snapshotted at log time so that a later correction
 * to the master food row never silently rewrites the user's history (spec §7.1).
 */
data class FoodLog(
    val id: String,
    val foodItemId: String,
    val foodName: String,
    val brand: String? = null,
    val date: LocalDate,
    val mealType: MealType,
    val quantity: Float,
    val unit: String,
    val macros: Macros,
    val loggedAt: Instant = Instant.EPOCH,
)

/** Per-meal rollup used by the dashboard's meal cards. */
data class MealSummary(
    val mealType: MealType,
    val entries: List<FoodLog> = emptyList(),
) {
    val macros: Macros = entries.fold(Macros.EMPTY) { acc, e -> acc + e.macros }
    val isEmpty: Boolean get() = entries.isEmpty()
}

/** Grams of each macro to aim for today (spec §5.3). */
data class MacroTargets(
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
) {
    val kcal: Int
        get() = (
            proteinG * AtwaterFactors.KCAL_PER_G_PROTEIN +
                carbsG * AtwaterFactors.KCAL_PER_G_CARBS +
                fatG * AtwaterFactors.KCAL_PER_G_FAT
            ).roundToInt()

    companion object {
        val EMPTY = MacroTargets(0, 0, 0)
    }
}

/** Consumed vs. target, for the macro bar. */
data class MacroProgress(
    val consumed: Macros = Macros.EMPTY,
    val targets: MacroTargets = MacroTargets.EMPTY,
) {
    val proteinFraction: Float get() = fraction(consumed.proteinG, targets.proteinG)
    val carbsFraction: Float get() = fraction(consumed.carbsG, targets.carbsG)
    val fatFraction: Float get() = fraction(consumed.fatG, targets.fatG)

    private fun fraction(value: Float, target: Int): Float =
        if (target <= 0) 0f else (value / target).coerceIn(0f, 1.5f)

    companion object {
        val EMPTY = MacroProgress()
    }
}

/** Water intake for a day (spec §4.2.4). */
data class WaterLog(
    val date: LocalDate,
    val amountMl: Int,
    val targetMl: Int,
) {
    val fraction: Float get() = if (targetMl <= 0) 0f else (amountMl.toFloat() / targetMl).coerceIn(0f, 1f)

    companion object {
        val QUICK_ADD_ML = listOf(250, 500)
    }
}
