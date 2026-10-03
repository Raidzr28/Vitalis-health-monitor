package com.vitalis.core.domain.gamification

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.QuestType
import java.time.LocalDate
import org.junit.jupiter.api.Test

class WeeklyQuestsTest {
    private val week = LocalDate.of(2026, 9, 28) // a Monday

    @Test
    fun `a newcomer gets gentle floors`() {
        val quests = WeeklyQuests.generate(week, QuestHistory(emptyList(), emptySet(), emptySet(), joined = week))
        assertThat(quests.map { it.type to it.target }).containsExactly(
            QuestType.DISTANCE_KM to 5.0,
            QuestType.LOG_DAYS to 3.0,
            QuestType.ACTIVITY_COUNT to 2.0,
            QuestType.PROTEIN_DAYS to 2.0,
        ).inOrder()
        assertThat(quests.sumOf { it.xpReward }).isEqualTo(200 + 150 + 180 + 150)
    }

    @Test
    fun `targets stretch a little past the last weeks, counting only weeks since joining`() {
        // Joined 2 weeks ago; ran 10 km and climbed 200 m each of those weeks, logged 5 days, hit protein 3.
        val joined = week.minusWeeks(2)
        val sessions = (1..2).flatMap { w ->
            val start = week.minusWeeks(w.toLong())
            listOf(QuestSession(start, 6_000.0, 120.0), QuestSession(start.plusDays(3), 4_000.0, 80.0))
        }
        val logged = (1..2).flatMap { w -> (0..4).map { week.minusWeeks(w.toLong()).plusDays(it.toLong()) } }.toSet()
        val protein = (1..2).flatMap { w -> (0..2).map { week.minusWeeks(w.toLong()).plusDays(it.toLong()) } }.toSet()

        val q = WeeklyQuests.generate(week, QuestHistory(sessions, logged, protein, joined)).associate { it.type to it.target }

        assertThat(q[QuestType.DISTANCE_KM]).isEqualTo(11.0) // 10 km/week (not 5 over 4 weeks) × 1.1
        assertThat(q[QuestType.ELEVATION_M]).isEqualTo(250.0) // 200 m × 1.15 → next 50
        assertThat(q[QuestType.LOG_DAYS]).isEqualTo(6.0)
        assertThat(q[QuestType.PROTEIN_DAYS]).isEqualTo(4.0)
    }

    @Test
    fun `progress counts only this week and ignores sessions too short for XP`() {
        val history = QuestHistory(
            sessions = listOf(
                QuestSession(week, 5_240.0, 48.0),
                QuestSession(week.plusDays(2), 150.0, 0.0), // too short to count as an activity
                QuestSession(week.minusDays(1), 9_000.0, 300.0), // last Sunday: previous week
            ),
            loggedDates = setOf(week, week.plusDays(1), week.minusDays(1)),
            proteinHitDates = setOf(week.plusDays(1)),
            joined = null,
        )
        assertThat(WeeklyQuests.progress(QuestType.DISTANCE_KM, week, history)).isWithin(1e-9).of(5.39)
        assertThat(WeeklyQuests.progress(QuestType.ACTIVITY_COUNT, week, history)).isEqualTo(1.0)
        assertThat(WeeklyQuests.progress(QuestType.LOG_DAYS, week, history)).isEqualTo(2.0)
        assertThat(WeeklyQuests.progress(QuestType.PROTEIN_DAYS, week, history)).isEqualTo(1.0)
        assertThat(WeeklyQuests.weekStart(week.plusDays(6))).isEqualTo(week)
    }
}
