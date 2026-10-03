package com.vitalis.feature.tracking

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.Lap
import com.vitalis.core.model.SportType
import com.vitalis.core.model.TrackingState
import java.time.LocalDate
import org.junit.Test

class VoiceCuesTest {
    private val lap1 = Lap(1, 1000.0, 370)

    @Test
    fun `a new kilometre is announced with time and average pace`() {
        val before = TrackingState(distanceMeters = 990.0, movingSeconds = 366)
        val after = TrackingState(distanceMeters = 1_004.0, movingSeconds = 371, laps = listOf(lap1))
        assertThat(VoiceCues.cue(before, after))
            .isEqualTo("Kilometer 1. Waktu 6 menit 11 detik. Pace rata-rata 6 menit 10 detik per kilometer.")
        assertThat(VoiceCues.cue(after, after.copy(distanceMeters = 1_010.0))).isNull() // nothing new: stay quiet
    }

    @Test
    fun `reaching the target wins over the kilometre it lands on`() {
        val before = TrackingState(distanceMeters = 4_996.0, movingSeconds = 1_846, laps = List(4) { lap1.copy(index = it + 1) }, targetMeters = 5_000.0)
        val after = before.copy(distanceMeters = 5_003.0, movingSeconds = 1_848, laps = List(5) { lap1.copy(index = it + 1) })
        assertThat(VoiceCues.cue(before, after)).isEqualTo("Target 5 kilometer tercapai. Waktu 30 menit 48 detik.")
    }

    @Test
    fun `spoken numbers and captions`() {
        assertThat(VoiceCues.spoken(3_905)).isEqualTo("1 jam 5 menit 5 detik")
        assertThat(VoiceCues.spoken(0)).isEqualTo("0 detik")
        assertThat(VoiceCues.km(21.1)).isEqualTo("21,1")
        assertThat(VoiceCues.km(5.0)).isEqualTo("5")

        val today = LocalDate.of(2026, 9, 27)
        assertThat(lastUsedLabel(today, today)).isEqualTo("Hari ini")
        assertThat(lastUsedLabel(today.minusDays(1), today)).isEqualTo("Kemarin")
        assertThat(lastUsedLabel(today.minusDays(3), today)).isEqualTo("3 hari lalu")
        assertThat(lastUsedLabel(today.minusDays(20), today)).isEqualTo("7 Sep")
        assertThat(targetOptions(SportType.CYCLING_ROAD)).contains(40.0)
    }
}
