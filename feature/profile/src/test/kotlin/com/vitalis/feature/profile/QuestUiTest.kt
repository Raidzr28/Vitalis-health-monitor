package com.vitalis.feature.profile

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.Quest
import com.vitalis.core.model.QuestType
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import org.junit.Test

class QuestUiTest {
    private val week = LocalDate.of(2026, 9, 28) // Monday

    @Test
    fun `quest rows read like the design`() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("id"))
        try {
            val running = questUi(Quest("a", QuestType.DISTANCE_KM, target = 15.0, current = 8.46, xpReward = 200, weekStart = week))
            assertThat(running.name).isEqualTo("Tempuh 15 km")
            assertThat(running.progressLabel).isEqualTo("8,4 / 15 km")
            assertThat(running.done).isFalse()

            val done = questUi(Quest("b", QuestType.LOG_DAYS, 5.0, 5.0, 150, week, completedAt = Instant.EPOCH))
            assertThat(done.progressLabel).isEqualTo("Selesai")
            assertThat(done.fraction).isEqualTo(1f)
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `countdown to Monday`() {
        assertThat(questsEndLabel(week, week)).isEqualTo("Sisa 7 hari")
        assertThat(questsEndLabel(week.plusDays(4), week)).isEqualTo("Sisa 3 hari")
        assertThat(questsEndLabel(week.plusDays(6), week)).isEqualTo("Berakhir hari ini")
    }
}
