package com.vitalis.core.domain.gamification

import com.vitalis.core.model.GamificationState
import com.vitalis.core.model.LevelProgress
import com.vitalis.core.model.LevelTier
import com.vitalis.core.model.XpAction
import com.vitalis.core.model.XpAward
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.pow

/**
 * Level curve `XP(n) = 100 × n^1.5` (spec §9.2).
 *
 * The exponent is what keeps long-term play meaningful: early levels arrive in
 * days so a new user sees momentum immediately, while later ones take weeks, so
 * a level-40 badge still means something.
 */
object LevelCurve {

    private const val BASE = 100.0
    private const val EXPONENT = 1.5

    /** XP required to advance *from* [level] to the next one. */
    fun xpForLevel(level: Int): Long {
        if (level < 1) return 0
        return (BASE * level.toDouble().pow(EXPONENT)).toLong()
    }

    /** Cumulative XP needed to have reached [level]. */
    fun cumulativeXpForLevel(level: Int): Long {
        if (level <= 1) return 0
        return (1 until level).sumOf { xpForLevel(it) }
    }

    /** The level a given lifetime XP total corresponds to. */
    fun levelForXp(totalXp: Long): Int {
        if (totalXp <= 0) return 1
        var level = 1
        var remaining = totalXp
        while (remaining >= xpForLevel(level) && level < MAX_LEVEL) {
            remaining -= xpForLevel(level)
            level++
        }
        return level
    }

    fun progressFor(totalXp: Long): LevelProgress {
        val level = levelForXp(totalXp)
        val intoLevel = totalXp - cumulativeXpForLevel(level)
        return LevelProgress(
            level = level,
            tier = LevelTier.forLevel(level),
            xpIntoLevel = intoLevel.coerceAtLeast(0),
            xpNeededForLevel = xpForLevel(level),
        )
    }

    const val MAX_LEVEL = 200
}

/**
 * Awards XP for completed actions (spec §9.2).
 *
 * Deliberately has no notion of calorie deficit. Rewarding "ate least" would
 * turn the app into a disordered-eating accelerator, which §9.1 rules out.
 */
object XpCalculator {

    fun forActivity(
        distanceMeters: Double,
        elevationGainM: Double,
        brokeRecord: Boolean,
    ): List<XpAward> = buildList {
        add(XpAward(XpAction.COMPLETE_GPS_ACTIVITY, XpAction.COMPLETE_GPS_ACTIVITY.baseXp))

        val fullKm = floor(distanceMeters / 1000.0).toInt()
        if (fullKm > 0) {
            add(
                XpAward(
                    action = XpAction.PER_KM_DISTANCE,
                    amount = XpAction.PER_KM_DISTANCE.baseXp * fullKm,
                    multiplier = fullKm,
                ),
            )
        }

        val elevationBlocks = floor(elevationGainM / 100.0).toInt()
        if (elevationBlocks > 0) {
            add(
                XpAward(
                    action = XpAction.PER_100M_ELEVATION,
                    amount = XpAction.PER_100M_ELEVATION.baseXp * elevationBlocks,
                    multiplier = elevationBlocks,
                ),
            )
        }

        if (brokeRecord) {
            add(XpAward(XpAction.BREAK_PERSONAL_RECORD, XpAction.BREAK_PERSONAL_RECORD.baseXp))
        }
    }

    fun forDiaryDay(
        mealsLogged: Int,
        hitWaterTarget: Boolean,
        hitStepTarget: Boolean,
        withinCalorieTarget: Boolean,
    ): List<XpAward> = buildList {
        if (mealsLogged > 0) {
            add(
                XpAward(
                    action = XpAction.LOG_MEAL,
                    amount = XpAction.LOG_MEAL.baseXp * mealsLogged,
                    multiplier = mealsLogged,
                ),
            )
        }
        if (mealsLogged >= 3) {
            add(XpAward(XpAction.COMPLETE_DAY_LOG, XpAction.COMPLETE_DAY_LOG.baseXp))
        }
        if (hitWaterTarget) add(XpAward(XpAction.HIT_WATER_TARGET, XpAction.HIT_WATER_TARGET.baseXp))
        if (hitStepTarget) add(XpAward(XpAction.HIT_STEP_TARGET, XpAction.HIT_STEP_TARGET.baseXp))
        if (withinCalorieTarget) {
            add(XpAward(XpAction.WITHIN_CALORIE_TARGET, XpAction.WITHIN_CALORIE_TARGET.baseXp))
        }
    }

    /** Streak milestones at 7/30/100/365 days (spec §9.2). */
    fun forStreakMilestone(streakDays: Int): XpAward? = when (streakDays) {
        7 -> XpAward(XpAction.COMPLETE_WEEKLY_QUEST, 100, reason = "streak_7")
        30 -> XpAward(XpAction.COMPLETE_WEEKLY_QUEST, 500, reason = "streak_30")
        100 -> XpAward(XpAction.COMPLETE_WEEKLY_QUEST, 2_000, reason = "streak_100")
        365 -> XpAward(XpAction.COMPLETE_WEEKLY_QUEST, 10_000, reason = "streak_365")
        else -> null
    }

    fun total(awards: List<XpAward>): Int = awards.sumOf { it.amount }
}

/**
 * Streak bookkeeping (spec §9.3).
 *
 * Two humane details, both deliberate. The grace period counts activity up to
 * 04:00 toward the previous day, because someone logging dinner at 1 a.m. has not
 * actually missed a day. Streak freezes absorb one genuine miss, so a single bad
 * day does not erase months of effort — losing everything at once is exactly the
 * loss-aversion pattern §9.1 rules out.
 */
object StreakCalculator {

    /** Activity before this hour still counts as the previous calendar day. */
    const val GRACE_HOUR = 4

    /** One free freeze earned per this many streak days, capped at [MAX_FREEZES]. */
    const val DAYS_PER_FREEZE = 14
    const val MAX_FREEZES = 3

    /** The day a timestamp belongs to, once the grace period is applied. */
    fun effectiveDate(dateTime: LocalDateTime): LocalDate =
        if (dateTime.hour < GRACE_HOUR) dateTime.toLocalDate().minusDays(1) else dateTime.toLocalDate()

    data class Result(
        val state: GamificationState,
        val milestoneReached: Int? = null,
        val freezeUsed: Boolean = false,
        val streakBroken: Boolean = false,
    )

    /**
     * Records qualifying activity (at least one meal or one workout) on [activityDate].
     */
    fun recordActivity(current: GamificationState, activityDate: LocalDate): Result {
        val last = current.lastActiveDate

        // Already counted today — logging a second meal must not inflate the streak.
        if (last == activityDate) return Result(current)

        if (last == null) {
            return Result(current.copy(currentStreakDays = 1, longestStreakDays = maxOf(1, current.longestStreakDays), lastActiveDate = activityDate))
        }

        val gap = ChronoUnit.DAYS.between(last, activityDate)

        return when {
            gap <= 0 -> Result(current) // Out-of-order entry; leave the streak alone.

            gap == 1L -> extend(current, activityDate)

            // Exactly one missed day and a freeze available: spend it and continue.
            gap == 2L && current.streakFreezesRemaining > 0 ->
                extend(current.copy(streakFreezesRemaining = current.streakFreezesRemaining - 1), activityDate)
                    .copy(freezeUsed = true)

            else -> Result(
                state = current.copy(currentStreakDays = 1, lastActiveDate = activityDate),
                streakBroken = true,
            )
        }
    }

    private fun extend(current: GamificationState, activityDate: LocalDate): Result {
        val newStreak = current.currentStreakDays + 1
        val earnedFreeze = newStreak % DAYS_PER_FREEZE == 0
        val freezes = (current.streakFreezesRemaining + if (earnedFreeze) 1 else 0).coerceAtMost(MAX_FREEZES)

        return Result(
            state = current.copy(
                currentStreakDays = newStreak,
                longestStreakDays = maxOf(newStreak, current.longestStreakDays),
                lastActiveDate = activityDate,
                streakFreezesRemaining = freezes,
            ),
            milestoneReached = newStreak.takeIf { it in MILESTONES },
        )
    }

    /**
     * Recomputes the streak as of [today] without recording anything — used on app
     * open so a streak that has already lapsed is not displayed as still alive.
     */
    fun currentStreak(state: GamificationState, today: LocalDate): Int {
        val last = state.lastActiveDate ?: return 0
        val gap = ChronoUnit.DAYS.between(last, today)
        return when {
            gap <= 1 -> state.currentStreakDays
            gap == 2L && state.streakFreezesRemaining > 0 -> state.currentStreakDays
            else -> 0
        }
    }

    val MILESTONES = setOf(7, 30, 100, 365)
}
