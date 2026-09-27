package com.vitalis.feature.profile

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.SportType
import com.vitalis.core.model.XpAction
import com.vitalis.core.model.XpAward
import com.vitalis.data.XpHistoryEntry
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Test

class XpHistoryMapperTest {
    private val zone = ZoneOffset.ofHours(7)
    private val today = LocalDate.of(2026, 9, 26)

    private fun entry(action: XpAction, amount: Int, ref: String, day: LocalDate, hour: Int, reason: String? = null, sport: SportType? = null, km: Double? = null) =
        XpHistoryEntry(XpAward(action, amount, reason = reason), ref, day.atTime(hour, 0).atZone(zone).toInstant(), sport, km?.times(1000))

    @Test
    fun `awards of one event collapse into one row, grouped by day newest first`() {
        val run = "activity:abc"
        val entries = listOf( // newest first, as the DAO returns them
            entry(XpAction.BREAK_PERSONAL_RECORD, 150, run, today, 7, sport = SportType.RUNNING, km = 5.24),
            entry(XpAction.PER_KM_DISTANCE, 50, run, today, 7, sport = SportType.RUNNING, km = 5.24),
            entry(XpAction.COMPLETE_GPS_ACTIVITY, 50, run, today, 7, sport = SportType.RUNNING, km = 5.24),
            entry(XpAction.LOG_MEAL, 10, "meal:$today:BREAKFAST", today, 6),
            entry(XpAction.COMPLETE_WEEKLY_QUEST, 100, "meal:$today:BREAKFAST", today, 6, reason = "streak_7"),
            entry(XpAction.HIT_WATER_TARGET, 20, "water:${today.minusDays(1)}", today.minusDays(1), 15),
        )

        val ui = xpHistory(entries, totalXp = 6_228, weekXp = 380, today = today, zone = zone)

        assertThat(ui.days.map { it.label }).containsExactly("Hari ini", "Kemarin").inOrder()
        val (runRow, mealRow) = ui.days[0].events
        assertThat(runRow.kind).isEqualTo(XpEventKind.RECORD)
        assertThat(runRow.title).startsWith("Lari 5")
        assertThat(runRow.xp).isEqualTo(250)
        assertThat(mealRow.title).isEqualTo("Sarapan dicatat")
        assertThat(mealRow.detail).contains("Streak 7 hari 100")
        assertThat(mealRow.xp).isEqualTo(110)
        assertThat(ui.days[1].events.single().title).isEqualTo("Target air tercapai")
    }
}
