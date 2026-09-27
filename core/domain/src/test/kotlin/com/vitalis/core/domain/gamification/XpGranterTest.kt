package com.vitalis.core.domain.gamification

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.GamificationState
import com.vitalis.core.model.XpAction
import com.vitalis.core.model.XpAward
import java.time.LocalDate
import org.junit.jupiter.api.Test

class XpGranterTest {
    private val day = LocalDate.of(2026, 9, 26)
    private val meal = listOf(XpAward(XpAction.LOG_MEAL, 10))

    @Test
    fun `awards add up and cross into level 2 at 283 XP`() {
        val r = XpGranter.grant(GamificationState(totalXp = 280), meal, streakDay = null)
        assertThat(r.state.totalXp).isEqualTo(290)
        assertThat(r.state.level).isEqualTo(2)
        assertThat(r.state.currentStreakDays).isEqualTo(0)
    }

    @Test
    fun `seventh consecutive day adds the 100 XP milestone bonus once`() {
        val six = GamificationState(currentStreakDays = 6, longestStreakDays = 6, lastActiveDate = day.minusDays(1))
        val r = XpGranter.grant(six, meal, streakDay = day)
        assertThat(r.state.currentStreakDays).isEqualTo(7)
        assertThat(r.xpGained).isEqualTo(110)
        assertThat(r.milestone).isEqualTo(7)

        // A second meal the same day neither extends the streak nor repeats the bonus.
        val again = XpGranter.grant(r.state, meal, streakDay = day)
        assertThat(again.state.currentStreakDays).isEqualTo(7)
        assertThat(again.xpGained).isEqualTo(10)
        assertThat(again.milestone).isNull()
    }

    @Test
    fun `tiny GPS sessions earn nothing`() {
        assertThat(XpCalculator.forActivity(150.0, 0.0, brokeRecord = false)).isEmpty()
        assertThat(XpCalculator.total(XpCalculator.forActivity(5_240.0, 120.0, brokeRecord = true))).isEqualTo(50 + 50 + 15 + 150)
    }
}
