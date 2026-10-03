package com.vitalis.feature.progress

import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.label
import com.vitalis.core.domain.body.BodyMetricsCalculator
import com.vitalis.core.domain.health.HealthScoreCalculator
import com.vitalis.core.model.HealthPillar
import com.vitalis.core.model.HealthScore
import com.vitalis.core.model.RecordType
import com.vitalis.core.model.Sex
import com.vitalis.data.ProgressData
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Id = Locale.forLanguageTag("id")

/** Records in the order people care about: race distances first, then totals. */
private val RecordOrder = listOf(
    RecordType.FASTEST_5K, RecordType.FASTEST_10K, RecordType.FASTEST_HALF_MARATHON, RecordType.FASTEST_MARATHON,
    RecordType.FASTEST_1K, RecordType.LONGEST_DISTANCE, RecordType.LONGEST_DURATION, RecordType.MOST_ELEVATION_GAIN,
    RecordType.HIGHEST_ALTITUDE, RecordType.FASTEST_AVG_PACE,
)

fun progressUiState(data: ProgressData, range: ProgressRange, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): ProgressUiState {
    val height = data.profile.heightCm.toDouble()
    val current = data.weighIns.lastOrNull()?.kg
    val health = HealthScoreCalculator.calculate(healthInputs(data, today, zone))
    val previous = HealthScoreCalculator.calculate(healthInputs(data, today.minusDays(7), zone)).takeIf { it.hasAnyData() }
    val month = today.withDayOfMonth(1)

    return ProgressUiState(
        range = range,
        healthScore = health.total.takeIf { health.hasAnyData() },
        healthDelta = previous?.let { health.total - it.total }.takeIf { health.hasAnyData() },
        pillars = health.pillars.map { Pillar(it.pillar.label(), it.score.takeIf { _ -> it.hasData }) },
        tip = tip(data, health, today, zone),
        weights = data.weighIns.filter { !it.date.isBefore(today.minusDays(range.days - 1L)) }.map { WeightPoint(it.date, it.kg) },
        currentWeightKg = current,
        targetWeightKg = data.profile.targetWeightKg,
        bmi = current?.let { BodyMetricsCalculator.bmi(it.toDouble(), height).toFloat() },
        waistToHeight = data.latestWaistCm?.let { BodyMetricsCalculator.waistToHeightRatio(it.toDouble(), height).toFloat() },
        month = month,
        today = today,
        intensityByDay = (1..today.dayOfMonth).associateWith { day -> intensity(activeMinutesOn(data, month.withDayOfMonth(day), zone)) },
        records = data.records
            .sortedWith(compareBy({ RecordOrder.indexOf(it.recordType) }, { it.sportType.ordinal }))
            .take(6)
            .map { r ->
                RecordRow(
                    name = r.recordType.label().replaceFirstChar { it.uppercase() },
                    detail = "${r.achievedAt.atZone(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy", Id))} · ${r.sportType.label()}",
                    value = Formatters.record(r.recordType, r.value),
                )
            },
    )
}

/**
 * Health Score inputs for the 7 days ending [end]. Anything the user has never tracked stays
 * null, so the calculator drops that pillar instead of scoring it as zero (spec §4.4.2).
 * Heart rate, sleep and steps are not collected yet, so cardio and most of recovery stay empty.
 */
fun healthInputs(data: ProgressData, end: LocalDate, zone: ZoneId = ZoneId.systemDefault()): HealthScoreCalculator.Inputs {
    // Days before the user joined (their onboarding weigh-in) are not "0 meals logged": they are
    // outside the record. A partial first week is scaled up to 7; a week before joining has no data.
    val joined = data.weighIns.firstOrNull()?.date
    val window = (0..6).map { end.minusDays(it.toLong()) }.filter { joined == null || !it.isBefore(joined) }.toSet()
    if (window.isEmpty()) return HealthScoreCalculator.Inputs()
    fun perWeek(count: Int) = (count * 7.0 / window.size).roundToInt().coerceIn(0, 7)

    val weight = data.weighIns.lastOrNull { !it.date.isAfter(end) }?.kg
    val height = data.profile.heightCm.toDouble()
    val activeDays = window.count { activeMinutesOn(data, it, zone) > 0 }
    val nutritionDays = data.days.filter { it.date in window }
    val stepDays = data.stepsSince?.let { since -> window.filter { !it.isBefore(since) } }.orEmpty()
    return HealthScoreCalculator.Inputs(
        bmi = weight?.let { BodyMetricsCalculator.bmi(it.toDouble(), height) },
        waistToHeightRatio = data.latestWaistCm?.let { BodyMetricsCalculator.waistToHeightRatio(it.toDouble(), height) },
        isMale = data.profile.sex == Sex.MALE,
        ageYears = data.profile.ageOn(end),
        weeklyActiveMinutes = (window.sumOf { activeMinutesOn(data, it, zone) } * 7.0 / window.size).roundToInt().takeIf { data.hasAnyActivity },
        // Only days the counter was on: before that, steps are unknown, not zero.
        avgDailySteps = stepDays.takeIf { it.isNotEmpty() }?.let { d -> d.sumOf { data.stepsByDate[it] ?: 0 } / d.size },
        daysLoggedThisWeek = perWeek(window.count { it in data.loggedDates }).takeIf { data.loggedDates.isNotEmpty() },
        proteinTargetHitDays = nutritionDays.filter { it.targetProteinG > 0 }.takeIf { it.isNotEmpty() }?.let { d -> perWeek(d.count { it.proteinG >= it.targetProteinG }) },
        waterTargetHitDays = nutritionDays.filter { it.targetWaterMl > 0 }.takeIf { it.isNotEmpty() }?.let { d -> perWeek(d.count { it.waterMl >= it.targetWaterMl }) },
        restDaysThisWeek = perWeek(window.size - activeDays).takeIf { data.hasAnyActivity },
    )
}

private fun activeMinutesOn(data: ProgressData, date: LocalDate, zone: ZoneId): Int =
    data.sessions.filter { it.startTime.atZone(zone).toLocalDate() == date }.sumOf { it.movingSeconds / 60 }.toInt()

/** Heatmap shade: 0 rest, then light / moderate / hard by active minutes. */
fun intensity(activeMinutes: Int): Int = when {
    activeMinutes <= 0 -> 0
    activeMinutes < 20 -> 1
    activeMinutes < 45 -> 2
    else -> 3
}

private fun HealthScore.hasAnyData() = pillars.any { it.hasData }

/** One concrete next step, aimed at the weakest pillar the user can actually move. */
private fun tip(data: ProgressData, health: HealthScore, today: LocalDate, zone: ZoneId): String {
    if (!data.hasAnyActivity) return "Rekam aktivitas pertamamu untuk mengisi pilar aktivitas."
    val weakest = health.pillars.filter { it.hasData && (it.score ?: 0) < 80 }.minByOrNull { it.score ?: 0 }
        ?: return "Semua pilar yang tercatat dalam kondisi baik minggu ini. Pertahankan."
    val inputs = healthInputs(data, today, zone)
    return when (weakest.pillar) {
        HealthPillar.ACTIVITY -> "Tambah ${(150 - (inputs.weeklyActiveMinutes ?: 0)).coerceAtLeast(10)} menit aktif lagi untuk mencapai 150 menit minggu ini."
        HealthPillar.NUTRITION -> inputs.daysLoggedThisWeek?.takeIf { it < 7 }?.let { "Catat makan setiap hari: baru $it dari 7 hari tercatat minggu ini." }
            ?: "Capai target protein dan air minum lebih sering untuk menaikkan pilar nutrisi."
        HealthPillar.RECOVERY -> if ((inputs.restDaysThisWeek ?: 1) < 1) "Sisihkan 1–2 hari istirahat per minggu." else "Tambah satu sesi aktivitas minggu ini."
        HealthPillar.BODY_COMPOSITION -> "Catat berat rutin tiap minggu supaya tren komposisi tubuh akurat."
        HealthPillar.CARDIO_FITNESS -> "Latihan kardio rutin menurunkan detak jantung istirahat."
    }
}

private fun HealthPillar.label() = when (this) {
    HealthPillar.BODY_COMPOSITION -> "Komposisi tubuh"
    HealthPillar.CARDIO_FITNESS -> "Kebugaran kardio"
    HealthPillar.ACTIVITY -> "Aktivitas"
    HealthPillar.NUTRITION -> "Nutrisi"
    HealthPillar.RECOVERY -> "Pemulihan"
}
