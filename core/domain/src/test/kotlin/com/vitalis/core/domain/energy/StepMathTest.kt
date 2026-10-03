package com.vitalis.core.domain.energy

import com.google.common.truth.Truth.assertThat
import java.time.LocalDateTime
import org.junit.jupiter.api.Test

class StepMathTest {
    private val t = LocalDateTime.of(2026, 9, 27, 7, 45)

    @Test
    fun `reboot restarts the counter`() {
        assertThat(StepMath.delta(10_000, 10_450)).isEqualTo(450)
        assertThat(StepMath.delta(10_000, 320)).isEqualTo(320) // rebooted since the last reading
    }

    @Test
    fun `steps are spread over hours in proportion and always add up`() {
        // 07:45 → 08:15, 600 steps: half before 08:00, half after.
        val split = StepMath.distribute(600, t, t.plusMinutes(30))
        assertThat(split).containsExactly(t.withMinute(0), 300, t.plusHours(1).withMinute(0), 300)

        // Same hour: everything lands there.
        assertThat(StepMath.distribute(120, t, t.plusMinutes(10))).containsExactly(t.withMinute(0), 120)

        // Across midnight, uneven numbers: the total is exact.
        val night = StepMath.distribute(1_001, LocalDateTime.of(2026, 9, 26, 23, 20), LocalDateTime.of(2026, 9, 27, 0, 50))
        assertThat(night.values.sum()).isEqualTo(1_001)
        assertThat(night.keys.map { it.toLocalDate().dayOfMonth }.toSet()).containsExactly(26, 27)
    }
}
