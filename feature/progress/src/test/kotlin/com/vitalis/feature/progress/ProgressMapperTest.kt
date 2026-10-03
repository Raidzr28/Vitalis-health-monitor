package com.vitalis.feature.progress

import com.google.common.truth.Truth.assertThat
import com.vitalis.core.domain.gamification.DayNutrition
import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.ActivitySource
import com.vitalis.core.model.Goal
import com.vitalis.core.model.PersonalRecord
import com.vitalis.core.model.RecordType
import com.vitalis.core.model.Sex
import com.vitalis.core.model.SportType
import com.vitalis.core.model.UserProfile
import com.vitalis.data.ProgressData
import com.vitalis.data.WeighIn
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Test

class ProgressMapperTest {
    private val zone = ZoneOffset.ofHours(7)
    private val today = LocalDate.of(2026, 9, 27)
    private val profile = UserProfile(
        id = "u", displayName = "Rangga", sex = Sex.MALE, birthDate = LocalDate.of(1996, 3, 14), heightCm = 172f,
        activityLevel = ActivityLevel.MODERATELY_ACTIVE, goal = Goal.LOSE, goalRateKgPerWeek = .5f, targetWeightKg = 68f,
    )

    private fun run(daysAgo: Long, minutes: Long) = ActivitySession(
        id = "s$daysAgo", sportType = SportType.RUNNING, source = ActivitySource.GPS,
        startTime = today.minusDays(daysAgo).atTime(6, 0).atZone(zone).toInstant(),
        endTime = today.minusDays(daysAgo).atTime(7, 0).atZone(zone).toInstant(),
        elapsedSeconds = minutes * 60, movingSeconds = minutes * 60, distanceMeters = 5_000.0,
    )

    private fun data(sessions: List<ActivitySession> = emptyList(), weighIns: List<WeighIn> = listOf(WeighIn(today.minusDays(30), 75f), WeighIn(today, 74f))) = ProgressData(
        profile = profile, weighIns = weighIns, latestWaistCm = null, sessions = sessions, hasAnyActivity = sessions.isNotEmpty(),
        days = listOf(DayNutrition(today.minusDays(1), 150.0, 200.0, 60.0, 2_000, 150, 230, 70, 2_000)),
        loggedDates = setOf(today, today.minusDays(1)),
        records = emptyList(),
    )

    @Test
    fun `untracked data stays out of the score instead of counting as zero`() {
        val none = healthInputs(data(), today, zone)
        assertThat(none.weeklyActiveMinutes).isNull() // never recorded an activity: not "0 minutes"
        assertThat(none.restDaysThisWeek).isNull()
        assertThat(none.restingHeartRate).isNull()
        assertThat(none.bmi).isWithin(.01).of(74 / (1.72 * 1.72))
        assertThat(none.daysLoggedThisWeek).isEqualTo(2)
        assertThat(none.proteinTargetHitDays).isEqualTo(1)

        // Only the last 7 days count: the 10-days-ago run is outside the window.
        assertThat(none.avgDailySteps).isNull() // step counter never on

        // Counter switched on 2 days ago: average over those 3 days only (8.000 + 6.000 + 4.000).
        val counted = data().copy(stepsSince = today.minusDays(2), stepsByDate = mapOf(today to 4_000, today.minusDays(1) to 6_000, today.minusDays(2) to 8_000))
        assertThat(healthInputs(counted, today, zone).avgDailySteps).isEqualTo(6_000)

        val active = healthInputs(data(listOf(run(0, 40), run(2, 30), run(10, 90))), today, zone)
        assertThat(active.weeklyActiveMinutes).isEqualTo(70)
        assertThat(active.restDaysThisWeek).isEqualTo(5)

        // Joined today and logged today: 1 of 1 days, scaled to a full week, not "1 of 7".
        val newcomer = healthInputs(data(weighIns = listOf(WeighIn(today, 74f))), today, zone)
        assertThat(newcomer.daysLoggedThisWeek).isEqualTo(7)
        // The week before joining is outside the record: nothing there to score.
        assertThat(healthInputs(data(weighIns = listOf(WeighIn(today, 74f))), today.minusDays(7), zone)).isEqualTo(
            com.vitalis.core.domain.health.HealthScoreCalculator.Inputs(),
        )
    }

    @Test
    fun `screen state reflects real data`() {
        val weighIns = listOf(WeighIn(today.minusDays(40), 76f), WeighIn(today.minusDays(20), 75f), WeighIn(today, 74f))
        val records = listOf(
            PersonalRecord("r1", SportType.RUNNING, RecordType.LONGEST_DISTANCE, 12_400.0, "s", today.atStartOfDay(zone).toInstant()),
            PersonalRecord("r2", SportType.RUNNING, RecordType.FASTEST_5K, 1_847.0, "s", today.atStartOfDay(zone).toInstant()),
        )
        val ui = progressUiState(data(listOf(run(0, 50), run(3, 10))).copy(weighIns = weighIns, records = records), ProgressRange.Month, today, zone)

        assertThat(ui.weights.map { it.kg }).containsExactly(75f, 74f).inOrder() // the 40-days-ago reading is outside "Bulan"
        assertThat(ui.healthDelta).isNotNull() // last week had a weigh-in, so there is something to compare
        assertThat(ui.pillars.first { it.name == "Kebugaran kardio" }.score).isNull()
        assertThat(ui.intensityByDay[27]).isEqualTo(3)
        assertThat(ui.intensityByDay[24]).isEqualTo(1)
        assertThat(ui.intensityByDay[25]).isEqualTo(0)
        assertThat(ui.records.map { it.name }).containsExactly("5K tercepat", "Jarak terjauh").inOrder()

        // Brand-new user: last week predates them, so no "+63 dari minggu lalu".
        assertThat(progressUiState(data(weighIns = listOf(WeighIn(today, 74f))), ProgressRange.Month, today, zone).healthDelta).isNull()
    }

    @Test
    fun `weight input accepts both decimal marks and rejects the implausible`() {
        assertThat(parseWeightKg("72,4")).isEqualTo(72.4f)
        assertThat(parseWeightKg(" 72.4 ")).isEqualTo(72.4f)
        assertThat(parseWeightKg("7")).isNull()
        assertThat(parseWeightKg("abc")).isNull()
    }
}
