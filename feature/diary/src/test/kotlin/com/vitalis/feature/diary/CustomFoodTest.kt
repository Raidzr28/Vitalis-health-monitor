package com.vitalis.feature.diary

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CustomFoodTest {
    @Test fun `label values per serving become per 100 g and round-trip`() {
        val r = CustomFoodInput("Keripik kentang", "1 bungkus", "68", "340", "4", "40", "18,5").toFoodItem("x")
        val item = (r as CustomFoodResult.Ok).item
        assertThat(item.kcalPer100g).isWithin(0.01f).of(500f)
        assertThat(item.nutritionForServings(1f).kcal).isWithin(0.01f).of(340f)
        assertThat(item.nutritionForServings(1f).fatG).isWithin(0.01f).of(18.5f)
    }

    @Test fun `blank macros default to zero, bad input is reported per field`() {
        assertThat(CustomFoodInput("Teh", "1 gelas", "250", "32").toFoodItem()).isInstanceOf(CustomFoodResult.Ok::class.java)
        val bad = CustomFoodInput("", "1 porsi", "0", "abc", protein = "-1").toFoodItem() as CustomFoodResult.Invalid
        assertThat(bad.errors.keys).containsExactly("name", "grams", "kcal", "protein")
    }

    @Test fun `macros heavier than the serving are rejected`() {
        val bad = CustomFoodInput("X", "1 porsi", "10", "100", "5", "5", "5").toFoodItem() as CustomFoodResult.Invalid
        assertThat(bad.errors).containsKey("grams")
    }
}
