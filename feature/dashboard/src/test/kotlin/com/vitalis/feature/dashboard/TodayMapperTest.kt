package com.vitalis.feature.dashboard

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.ActivitySource
import com.vitalis.core.model.CalorieModel
import com.vitalis.core.model.DailyTargets
import com.vitalis.core.model.FoodLog
import com.vitalis.core.model.GamificationState
import com.vitalis.core.model.Goal
import com.vitalis.core.model.MacroTargets
import com.vitalis.core.model.Macros
import com.vitalis.core.model.MealType
import com.vitalis.core.model.Sex
import com.vitalis.core.model.SportType
import com.vitalis.core.model.UserProfile
import com.vitalis.data.DayStats
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class TodayMapperTest {
    private val date = LocalDate.of(2026, 9, 25)
    private val profile = UserProfile(
        id = "u", displayName = "Rangga", sex = Sex.MALE, birthDate = LocalDate.of(1998, 3, 14), heightCm = 175f,
        activityLevel = ActivityLevel.MODERATELY_ACTIVE, goal = Goal.LOSE, goalRateKgPerWeek = 0.5f, targetWeightKg = 75f,
    )
    private val day = DayStats(date, DailyTargets(1839, 2850, 2300, MacroTargets(158, 259, 70), 10000, 2750), steps = 0)

    private fun log(meal: MealType, name: String, kcal: Float, p: Float = 0f) =
        FoodLog(name, name, name, date = date, mealType = meal, quantity = 1f, unit = "1 porsi", macros = Macros(kcal, p, 0f, 0f))

    @Test fun `budget uses net exercise calories and groups meals in order`() {
        val run = ActivitySession(
            id = "r", sportType = SportType.RUNNING, source = ActivitySource.GPS, startTime = Instant.EPOCH, endTime = Instant.EPOCH,
            elapsedSeconds = 1938, movingSeconds = 1938, kcalGross = 412, kcalNet = 371, calorieModel = CalorieModel.GPS_BIOMECHANIC,
        )
        val logs = listOf(log(MealType.LUNCH, "Ayam bakar", 290f, 27f), log(MealType.BREAKFAST, "Nasi uduk", 390f), log(MealType.LUNCH, "Nasi putih", 260f))

        val s = todayUiState(date, profile, day, logs, waterMl = 750, sessions = listOf(run), game = GamificationState())

        assertThat(s.budget.consumedKcal).isEqualTo(940)
        assertThat(s.budget.burnedNetKcal).isEqualTo(371) // net, never gross (spec §4.3.3)
        assertThat(s.budget.remainingKcal).isEqualTo(2300 - 940 + 371)
        assertThat(s.meals.map { it.type }).containsExactly(MealType.BREAKFAST, MealType.LUNCH).inOrder()
        assertThat(s.meals[1].kcal).isEqualTo(550)
        assertThat(s.meals[1].items).isEqualTo("Ayam bakar, Nasi putih")
        assertThat(s.activeMinutes).isEqualTo(32)
        assertThat(s.macros[0].eatenG).isEqualTo(27)
        assertThat(s.dateLabel).isEqualTo("Jumat, 25 September")
    }

    @Test fun `fresh user starts at level 1 with no meals`() {
        val s = todayUiState(date, profile, day, emptyList(), 0, emptyList(), GamificationState())
        assertThat(s.level).isEqualTo(1)
        assertThat(s.meals).isEmpty()
        assertThat(s.budget.remainingKcal).isEqualTo(2300)
    }

    @Test
    fun `step bars pair up hours and scale to the busiest pair`() {
        val hours = List(24) { if (it == 7) 1_200 else if (it == 8) 400 else if (it == 18) 800 else 0 }
        val bars = stepBars(hours)
        assertThat(bars).hasSize(12)
        assertThat(bars[3]).isEqualTo(1f) // 06-08: 1.200
        assertThat(bars[4]).isEqualTo(400f / 1_200f)
        assertThat(bars[9]).isEqualTo(800f / 1_200f)
        assertThat(stepBars(List(24) { 0 })).containsExactlyElementsIn(List(12) { 0f })
    }
}
