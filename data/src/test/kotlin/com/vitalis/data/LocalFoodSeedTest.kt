package com.vitalis.data

import com.google.common.truth.Truth.assertWithMessage
import com.vitalis.core.model.AtwaterFactors
import org.junit.Test
import kotlin.math.abs

class LocalFoodSeedTest {
    @Test fun `every seed row is internally consistent`() {
        val items = LocalFoodSeed.items
        assertWithMessage("ids must be unique").that(items.map { it.id }.toSet()).hasSize(items.size)
        items.forEach { f ->
            val fromMacros = f.proteinPer100g * AtwaterFactors.KCAL_PER_G_PROTEIN +
                f.carbsPer100g * AtwaterFactors.KCAL_PER_G_CARBS +
                f.fatPer100g * AtwaterFactors.KCAL_PER_G_FAT
            // Fibre and rounding explain small gaps; a big one means a typo in the table.
            val gap = abs(fromMacros - f.kcalPer100g)
            assertWithMessage("${f.name}: ${f.kcalPer100g} kcal vs $fromMacros from macros").that(gap <= maxOf(10.0, f.kcalPer100g * 0.15)).isTrue()
            assertWithMessage("${f.name}: macros exceed 100 g").that(f.proteinPer100g + f.carbsPer100g + f.fatPer100g <= 100f).isTrue()
            assertWithMessage("${f.name}: serving").that(f.servingSizeG in 5f..600f).isTrue()
        }
    }
}

class LikePatternTest {
    @Test fun `wildcards typed by the user are literal`() {
        com.google.common.truth.Truth.assertThat(likePattern(" 50%_off! ")).isEqualTo("%50!%!_off!!%")
        com.google.common.truth.Truth.assertThat(likePattern("")).isEqualTo("%%")
    }
}
