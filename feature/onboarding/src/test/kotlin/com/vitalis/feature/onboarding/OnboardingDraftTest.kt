package com.vitalis.feature.onboarding

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.Goal
import com.vitalis.core.model.Sex
import java.time.LocalDate
import org.junit.Test

class OnboardingDraftTest {
    private val today = LocalDate.of(2026, 9, 25)
    private val valid = OnboardingDraft(
        goal = Goal.LOSE, rateKgPerWeek = 0.5f, targetWeight = "75",
        name = "Rangga", sex = Sex.MALE, birthDate = LocalDate.of(1998, 3, 14),
        height = "175", weight = "88", activityLevel = ActivityLevel.MODERATELY_ACTIVE,
    )

    @Test fun `valid draft passes every step and maps to a profile`() {
        OnboardingStep.entries.forEach { assertThat(valid.errors(it, today)).isEmpty() }
        val p = valid.toProfile()
        assertThat(p.heightCm).isEqualTo(175f)
        assertThat(p.targetWeightKg).isEqualTo(75f)
    }

    @Test fun `comma decimals are accepted`() {
        assertThat(valid.copy(weight = "88,5").weightKg).isEqualTo(88.5f)
    }

    @Test fun `out of range and missing numbers are rejected`() {
        val e = valid.copy(height = "17", weight = "abc").errors(OnboardingStep.Body, today)
        assertThat(e.keys).containsAtLeast("height", "weight")
    }

    @Test fun `target must point in the goal's direction`() {
        assertThat(valid.copy(targetWeight = "90").errors(OnboardingStep.Body, today)).containsKey("weight")
        assertThat(valid.copy(goal = Goal.GAIN, targetWeight = "80").errors(OnboardingStep.Body, today)).containsKey("weight")
    }

    @Test fun `maintain needs no target and stores none`() {
        val m = valid.copy(goal = Goal.MAINTAIN, targetWeight = "")
        assertThat(m.errors(OnboardingStep.Goal, today)).isEmpty()
        assertThat(m.toProfile().targetWeightKg).isNull()
        assertThat(m.toProfile().goalRateKgPerWeek).isEqualTo(0f)
    }

    @Test fun `too young is rejected`() {
        val kid = valid.copy(birthDate = today.minusYears(MIN_AGE.toLong() - 1))
        assertThat(kid.errors(OnboardingStep.Body, today)).containsKey("birth")
    }
}
