package com.vitalis.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.BmiStandard
import com.vitalis.core.model.Goal
import com.vitalis.core.model.Sex
import com.vitalis.core.model.TdeeMode
import com.vitalis.core.model.UnitSystem
import com.vitalis.core.model.UserProfile
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Float,
    val activityLevel: ActivityLevel,
    val tdeeMode: TdeeMode,
    val goal: Goal,
    val goalRateKgPerWeek: Float,
    val targetWeightKg: Float?,
    val unitSystem: UnitSystem,
    val bmiStandard: BmiStandard,
    val dietPreference: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun UserProfileEntity.toDomain() = UserProfile(
    id = id,
    displayName = displayName,
    sex = sex,
    birthDate = birthDate,
    heightCm = heightCm,
    activityLevel = activityLevel,
    tdeeMode = tdeeMode,
    goal = goal,
    goalRateKgPerWeek = goalRateKgPerWeek,
    targetWeightKg = targetWeightKg,
    unitSystem = unitSystem,
    bmiStandard = bmiStandard,
    dietPreference = dietPreference,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun UserProfile.toEntity() = UserProfileEntity(
    id = id,
    displayName = displayName,
    sex = sex,
    birthDate = birthDate,
    heightCm = heightCm,
    activityLevel = activityLevel,
    tdeeMode = tdeeMode,
    goal = goal,
    goalRateKgPerWeek = goalRateKgPerWeek,
    targetWeightKg = targetWeightKg,
    unitSystem = unitSystem,
    bmiStandard = bmiStandard,
    dietPreference = dietPreference,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/**
 * Per-day rollup of things that are counted rather than logged: steps, water,
 * and the day's computed targets.
 *
 * Targets are snapshotted daily rather than recomputed on read, so that looking
 * back at last month shows the budget the user was actually working to, not one
 * recalculated from today's weight.
 */
@Entity(tableName = "daily_stats", indices = [Index("date", unique = true)])
data class DailyStatsEntity(
    @PrimaryKey val date: LocalDate,
    val steps: Int = 0,
    val waterMl: Int = 0,
    val targetKcal: Int = 0,
    val targetProteinG: Int = 0,
    val targetCarbsG: Int = 0,
    val targetFatG: Int = 0,
    val stepsTarget: Int = 0,
    val waterTargetMl: Int = 0,
    val bmrKcal: Int = 0,
    val tdeeKcal: Int = 0,
)
