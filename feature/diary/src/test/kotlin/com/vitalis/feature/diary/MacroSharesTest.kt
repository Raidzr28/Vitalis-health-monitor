package com.vitalis.feature.diary

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MacroSharesTest {
    @Test
    fun `empty day and single-macro days give no zero-width segment to draw`() {
        // An empty day crashed Diary: Compose rejects weight(0f).
        assertThat(macroShares(0, 0, 0)).containsExactly(0f, 0f, 0f).inOrder()
        assertThat(macroShares(0, 25, 0)).containsExactly(0f, 1f, 0f).inOrder()

        val mixed = macroShares(96, 150, 52) // 384 + 600 + 468 kcal
        assertThat(mixed.sum()).isWithin(1e-5f).of(1f)
        assertThat(mixed[2]).isWithin(1e-3f).of(468f / 1452f)
    }
}
