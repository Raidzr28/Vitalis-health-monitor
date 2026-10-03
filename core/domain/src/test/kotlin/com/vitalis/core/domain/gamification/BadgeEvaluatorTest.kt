package com.vitalis.core.domain.gamification

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.BadgeMetric
import java.time.LocalDate
import org.junit.jupiter.api.Test

class BadgeEvaluatorTest {
    private val today = LocalDate.of(2026, 9, 27)

    private fun day(offset: Long, p: Double, c: Double, f: Double, water: Int) =
        DayNutrition(today.minusDays(offset), p, c, f, water, targetProteinG = 150, targetCarbsG = 230, targetFatG = 70, targetWaterMl = 2_000)

    @Test
    fun `stats count the right activities and days`() {
        val starts = listOf(
            today.minusDays(3).atTime(2, 30), // night owl
            today.minusDays(2).atTime(5, 10), // sunrise
            today.minusDays(1).atTime(18, 0),
            today.atTime(5, 55), // sunrise
        )
        val days = listOf(
            day(2, 150.0, 225.0, 72.0, 2_000), // macros in range, water hit, protein hit
            day(1, 120.0, 230.0, 70.0, 1_500), // protein 20% short: not in range, not hit
            day(0, 152.0, 231.0, 69.0, 2_100), // in range but today is unfinished
        )

        val s = BadgeEvaluator.stats(5_240.0, 48.0, 0.0, 12, starts, days, today)

        assertThat(s[BadgeMetric.DISTANCE_KM]).isWithin(1e-9).of(5.24)
        assertThat(s[BadgeMetric.NIGHT_ACTIVITIES]).isEqualTo(1.0)
        assertThat(s[BadgeMetric.SUNRISE_ACTIVITIES]).isEqualTo(2.0)
        assertThat(s[BadgeMetric.ACTIVE_DAY_RUN]).isEqualTo(4.0)
        assertThat(s[BadgeMetric.MACRO_DAYS]).isEqualTo(1.0)
        assertThat(s[BadgeMetric.PROTEIN_DAYS]).isEqualTo(2.0)
        assertThat(s[BadgeMetric.WATER_DAYS]).isEqualTo(2.0)
    }

    @Test
    fun `progress is clamped and a run restarts after a gap`() {
        val firstSteps = BadgeCatalog.ALL.first { it.id == "first_steps" }
        assertThat(BadgeEvaluator.progress(firstSteps, mapOf(BadgeMetric.DISTANCE_KM to 5.24))).isEqualTo(1f)
        assertThat(BadgeEvaluator.progress(firstSteps, emptyMap())).isEqualTo(0f)

        val gap = listOf(today, today.minusDays(1), today.minusDays(3), today.minusDays(4), today.minusDays(5))
        assertThat(BadgeEvaluator.longestRun(gap)).isEqualTo(3)
        assertThat(BadgeCatalog.ALL.map { it.id }.toSet()).hasSize(BadgeCatalog.ALL.size) // ids are unique: they are DB keys
    }
}
