package com.vitalis.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vitalis.core.model.AchievementCategory
import com.vitalis.core.model.GamificationState
import com.vitalis.core.model.PersonalRecord
import com.vitalis.core.model.Quest
import com.vitalis.core.model.QuestType
import com.vitalis.core.model.RecordType
import com.vitalis.core.model.SportType
import com.vitalis.core.model.Tier
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "gamification_state")
data class GamificationStateEntity(
    @PrimaryKey val userId: String,
    val totalXp: Long,
    val level: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val lastActiveDate: LocalDate?,
    val streakFreezesRemaining: Int,
)

fun GamificationStateEntity.toDomain() = GamificationState(
    userId = userId,
    totalXp = totalXp,
    level = level,
    currentStreakDays = currentStreakDays,
    longestStreakDays = longestStreakDays,
    lastActiveDate = lastActiveDate,
    streakFreezesRemaining = streakFreezesRemaining,
)

fun GamificationState.toEntity() = GamificationStateEntity(
    userId = userId,
    totalXp = totalXp,
    level = level,
    currentStreakDays = currentStreakDays,
    longestStreakDays = longestStreakDays,
    lastActiveDate = lastActiveDate,
    streakFreezesRemaining = streakFreezesRemaining,
)

/**
 * Awarded XP, kept as an append-only ledger.
 *
 * Storing the events rather than just the total means the level can always be
 * recomputed — which matters because gamification is server-authoritative once
 * sync exists (spec §14.2) and the client's optimistic total will sometimes drift.
 */
@Entity(tableName = "xp_ledger", indices = [Index("awardedAt"), Index("isSynced")])
data class XpLedgerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val amount: Int,
    val multiplier: Int,
    val reason: String?,
    val referenceId: String?,
    val awardedAt: Instant,
    val isSynced: Boolean = false,
)

/** A ledger row plus the session it paid for (null unless [referenceId] is `activity:<id>`). */
data class XpHistoryRow(
    val action: String,
    val amount: Int,
    val multiplier: Int,
    val reason: String?,
    val referenceId: String?,
    val awardedAt: Instant,
    val sportType: SportType?,
    val distanceMeters: Double?,
)

/** Lifetime activity sums for badges. */
data class ActivityTotals(val distanceM: Double, val elevationM: Double, val maxAltitudeM: Double)

/** One day's intake and targets, for the nutrition badges. */
data class DayNutritionRow(
    val date: LocalDate,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val waterMl: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int,
    val targetWaterMl: Int,
)

/**
 * A badge's progress and unlock time. Definitions (title, tier, threshold) live in code,
 * in BadgeCatalog, so [titleKey] and [descriptionKey] only mirror them for debugging.
 */
@Entity(tableName = "achievement", indices = [Index("category")])
data class AchievementEntity(
    @PrimaryKey val id: String,
    val category: AchievementCategory,
    val tier: Tier,
    val titleKey: String,
    val descriptionKey: String,
    val threshold: Double,
    val progress: Float,
    val unlockedAt: Instant?,
    val isHidden: Boolean,
    val sportType: SportType?,
)

@Entity(
    tableName = "personal_record",
    indices = [Index(value = ["sportType", "recordType"], unique = true)],
)
data class PersonalRecordEntity(
    @PrimaryKey val id: String,
    val sportType: SportType,
    val recordType: RecordType,
    val value: Double,
    val sessionId: String,
    val achievedAt: Instant,
    val previousValue: Double?,
)

fun PersonalRecordEntity.toDomain() = PersonalRecord(
    id = id,
    sportType = sportType,
    recordType = recordType,
    value = value,
    sessionId = sessionId,
    achievedAt = achievedAt,
    previousValue = previousValue,
)

fun PersonalRecord.toEntity() = PersonalRecordEntity(
    id = id,
    sportType = sportType,
    recordType = recordType,
    value = value,
    sessionId = sessionId,
    achievedAt = achievedAt,
    previousValue = previousValue,
)

@Entity(tableName = "quest", indices = [Index("weekStart")])
data class QuestEntity(
    @PrimaryKey val id: String,
    val type: QuestType,
    val target: Double,
    val current: Double,
    val xpReward: Int,
    val weekStart: LocalDate,
    val completedAt: Instant?,
)

fun QuestEntity.toDomain() = Quest(
    id = id,
    type = type,
    target = target,
    current = current,
    xpReward = xpReward,
    weekStart = weekStart,
    completedAt = completedAt,
)

fun Quest.toEntity() = QuestEntity(
    id = id,
    type = type,
    target = target,
    current = current,
    xpReward = xpReward,
    weekStart = weekStart,
    completedAt = completedAt,
)
