package com.vitalis.feature.profile

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.domain.gamification.BadgeCatalog
import com.vitalis.data.BadgeProgress
import java.time.Instant
import java.util.Locale
import org.junit.Test

class ProfileBadgesTest {
    private fun badge(id: String) = BadgeCatalog.ALL.first { it.id == id }

    @Test
    fun `newest unlocks lead, then the closest visible badges, never a locked secret`() {
        val all = listOf(
            BadgeProgress(badge("first_steps"), 1f, Instant.parse("2026-09-01T00:00:00Z")),
            BadgeProgress(badge("hill_starter"), 1f, Instant.parse("2026-09-20T00:00:00Z")),
            BadgeProgress(badge("climber"), .41f, null),
            BadgeProgress(badge("road_warrior"), .05f, null),
            BadgeProgress(badge("night_owl"), 0f, null), // secret and locked: stays off Profile
            BadgeProgress(badge("hydrated"), .6f, null),
        )

        val shown = profileBadges(all, limit = 4).map { it.badge.id }

        assertThat(shown).containsExactly("hill_starter", "first_steps", "hydrated", "climber").inOrder()
    }

    @Test
    fun `progress labels round down in the badge's own unit`() {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("id")) // labels follow the phone's language, like Formatters
        try {
            assertThat(BadgeProgress(badge("getting_started"), .524f, null).progressLabel()).isEqualTo("5,2 / 10 km")
            assertThat(BadgeProgress(badge("climber"), .4129f, null).progressLabel()).isEqualTo("412 / 1.000 m")
            assertThat(BadgeProgress(badge("week_one"), 3 / 7f, null).progressLabel()).isEqualTo("3 / 7 hari")
        } finally {
            Locale.setDefault(original)
        }
    }
}
