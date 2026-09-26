package com.vitalis.feature.diary

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.FoodLog
import com.vitalis.core.model.Macros
import com.vitalis.core.model.MealType
import java.time.LocalDate
import org.junit.Test

class DiaryMapperTest {
    private val date = LocalDate.of(2026, 9, 25)

    private fun log(meal: MealType, kcal: Float, d: LocalDate = date) =
        FoodLog("$meal$kcal$d", "f", "Makanan", date = d, mealType = meal, quantity = 1.5f, unit = "1 potong", macros = Macros(kcal, 0f, 0f, 0f))

    @Test fun `always shows the four main meals and yesterday per meal`() {
        val s = diaryUiState(
            date, date,
            logs = listOf(log(MealType.LUNCH, 290f)),
            yesterday = listOf(log(MealType.DINNER, 400f, date.minusDays(1)), log(MealType.DINNER, 210f, date.minusDays(1))),
            day = null, burnedNetKcal = 0, loggedDates = setOf(date),
        )
        assertThat(s.meals.map { it.type }).containsExactly(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).inOrder()
        assertThat(s.meals[1].entries.single().portion).isEqualTo("1,5 potong")
        assertThat(s.yesterdayKcal[MealType.DINNER]).isEqualTo(610)
    }

    @Test fun `portion label falls back to multiplier for gram servings`() {
        assertThat(portionLabel(2f, "100 g")).isEqualTo("2 × 100 g")
        assertThat(portionLabel(1f, "1 mangkuk")).isEqualTo("1 mangkuk")
    }
}
