package com.vitalis.core.domain.gamification

import com.vitalis.core.model.Quest
import com.vitalis.core.model.QuestType
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

/** One recorded activity, reduced to what quests measure. */
data class QuestSession(val date: LocalDate, val distanceM: Double, val elevationM: Double)

/** What the user did, day by day. Covers the 4 weeks before this one plus this week. */
data class QuestHistory(
    val sessions: List<QuestSession>,
    val loggedDates: Set<LocalDate>,
    val proteinHitDates: Set<LocalDate>,
    /** First day with the app (the onboarding weigh-in); weeks before it are not "zero" weeks. */
    val joined: LocalDate?,
)

/**
 * Weekly quests (spec §9.5): four goals every Monday, sized from the user's own last four
 * weeks so they stretch without being out of reach. Steps are left out until a step counter exists.
 */
object WeeklyQuests {
    /** Weeks run Monday to Sunday. */
    fun weekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    /** Paid on top when all four are done in the same week. */
    const val ALL_DONE_BONUS_XP = 500

    fun xpFor(type: QuestType): Int = when (type) {
        QuestType.DISTANCE_KM -> 200
        QuestType.ELEVATION_M, QuestType.ACTIVITY_COUNT -> 180
        QuestType.LOG_DAYS, QuestType.PROTEIN_DAYS, QuestType.STEP_DAYS -> 150
    }

    fun generate(week: LocalDate, history: QuestHistory): List<Quest> {
        val pastWeeks = (1..4).map { week.minusWeeks(it.toLong()) }
            .filter { start -> history.joined == null || !start.plusDays(6).isBefore(history.joined) }
        val weeks = pastWeeks.size.coerceAtLeast(1)
        fun avg(type: QuestType) = pastWeeks.sumOf { progress(type, it, history) } / weeks

        val km = avg(QuestType.DISTANCE_KM)
        val elevation = avg(QuestType.ELEVATION_M)
        val third = if (elevation >= 50) {
            QuestType.ELEVATION_M to maxOf(100.0, ceilClean(elevation * 1.15 / 50) * 50)
        } else {
            QuestType.ACTIVITY_COUNT to (avg(QuestType.ACTIVITY_COUNT).roundToInt() + 1).coerceIn(2, 5).toDouble()
        }
        return listOf(
            QuestType.DISTANCE_KM to maxOf(5.0, ceilClean(km * 1.1)),
            QuestType.LOG_DAYS to (avg(QuestType.LOG_DAYS).roundToInt() + 1).coerceIn(3, 7).toDouble(),
            third,
            QuestType.PROTEIN_DAYS to (avg(QuestType.PROTEIN_DAYS).roundToInt() + 1).coerceIn(2, 6).toDouble(),
        ).map { (type, target) -> Quest(id = "$week:$type", type = type, target = target, xpReward = xpFor(type), weekStart = week) }
    }

    /** ceil that ignores floating-point dust: 10 × 1.1 is 11.000000000000002, and that must stay 11. */
    private fun ceilClean(x: Double) = ceil(x - 1e-9)

    /** How far [type] got in the week starting [week]. */
    fun progress(type: QuestType, week: LocalDate, history: QuestHistory): Double {
        val days = (0..6).map { week.plusDays(it.toLong()) }.toSet()
        val sessions = history.sessions.filter { it.date in days }
        return when (type) {
            QuestType.DISTANCE_KM -> sessions.sumOf { it.distanceM } / 1000.0
            QuestType.ELEVATION_M -> sessions.sumOf { it.elevationM }
            // Same bar as XP: a session too short to earn XP does not count as an activity.
            QuestType.ACTIVITY_COUNT -> sessions.count { it.distanceM >= XpCalculator.MIN_GPS_ACTIVITY_METERS }.toDouble()
            QuestType.LOG_DAYS -> days.count { it in history.loggedDates }.toDouble()
            QuestType.PROTEIN_DAYS -> days.count { it in history.proteinHitDates }.toDouble()
            QuestType.STEP_DAYS -> 0.0
        }
    }
}
