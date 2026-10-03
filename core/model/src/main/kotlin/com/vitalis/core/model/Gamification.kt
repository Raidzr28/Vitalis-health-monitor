package com.vitalis.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * Actions that grant XP (spec §9.2).
 *
 * Note what is *absent*: nothing rewards eating less, and there is no "fasting
 * streak". Rewarding deficit depth would make the app actively harmful for the
 * users most at risk (spec §9.1, §4.4.3).
 */
enum class XpAction(val baseXp: Int) {
    LOG_MEAL(10),
    COMPLETE_DAY_LOG(30),
    LOG_WEIGHT(15),
    HIT_WATER_TARGET(20),
    HIT_STEP_TARGET(25),
    COMPLETE_GPS_ACTIVITY(50),
    PER_KM_DISTANCE(10),
    PER_100M_ELEVATION(15),
    BREAK_PERSONAL_RECORD(150),
    COMPLETE_WEEKLY_QUEST(200),
    WITHIN_CALORIE_TARGET(40),
}

/** One granted award, kept as a ledger so totals can be recomputed and audited. */
data class XpAward(
    val action: XpAction,
    val amount: Int,
    val multiplier: Int = 1,
    val reason: String? = null,
)

data class GamificationState(
    val userId: String = UserProfile.DEFAULT_ID,
    val totalXp: Long = 0,
    val level: Int = 1,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val lastActiveDate: LocalDate? = null,
    /** Earned protection against a single missed day (spec §9.3). */
    val streakFreezesRemaining: Int = 0,
) {
    val tier: LevelTier get() = LevelTier.forLevel(level)
}

/** Where the user sits inside their current level, for the XP bar. */
data class LevelProgress(
    val level: Int,
    val tier: LevelTier,
    val xpIntoLevel: Long,
    val xpNeededForLevel: Long,
) {
    val fraction: Float
        get() = if (xpNeededForLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpNeededForLevel).coerceIn(0f, 1f)

    companion object {
        val EMPTY = LevelProgress(level = 1, tier = LevelTier.BRONZE, xpIntoLevel = 0, xpNeededForLevel = 283)
    }
}

/**
 * A badge definition plus the user's progress toward it. Hidden achievements
 * (`isHidden`) show as silhouettes until unlocked (spec §9.4).
 */
data class Achievement(
    val id: String,
    val category: AchievementCategory,
    val tier: Tier,
    val titleKey: String,
    val descriptionKey: String,
    /** Value the user must reach — km, metres, days, depending on category. */
    val threshold: Double,
    val progress: Float = 0f,
    val unlockedAt: Instant? = null,
    val isHidden: Boolean = false,
    val sportType: SportType? = null,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}

data class PersonalRecord(
    val id: String,
    val sportType: SportType,
    val recordType: RecordType,
    val value: Double,
    val sessionId: String,
    val achievedAt: Instant,
    /** Previous best, so the summary can say "38 seconds faster". */
    val previousValue: Double? = null,
) {
    /** Time-based records improve by going down; distance and elevation by going up. */
    val isLowerBetter: Boolean
        get() = recordType in setOf(
            RecordType.FASTEST_1K,
            RecordType.FASTEST_5K,
            RecordType.FASTEST_10K,
            RecordType.FASTEST_HALF_MARATHON,
            RecordType.FASTEST_MARATHON,
            RecordType.FASTEST_AVG_PACE,
        )
}

/** A weekly goal generated on Monday, sized from the user's own history (spec §9.5). */
data class Quest(
    val id: String,
    val type: QuestType,
    val target: Double,
    val current: Double = 0.0,
    val xpReward: Int,
    val weekStart: LocalDate,
    val completedAt: Instant? = null,
) {
    val fraction: Float get() = if (target <= 0) 0f else (current / target).coerceIn(0.0, 1.0).toFloat()
    val isComplete: Boolean get() = completedAt != null || current >= target
}

/** The lifetime number a badge is measured against (spec §9.4). */
enum class BadgeMetric {
    DISTANCE_KM, ELEVATION_M, LONGEST_STREAK_DAYS, MACRO_DAYS, WATER_DAYS, PROTEIN_DAYS,
    NIGHT_ACTIVITIES, SUNRISE_ACTIVITIES, ACTIVE_DAY_RUN, MAX_ALTITUDE_M,
}

enum class QuestType { DISTANCE_KM, ELEVATION_M, LOG_DAYS, PROTEIN_DAYS, ACTIVITY_COUNT, STEP_DAYS }
