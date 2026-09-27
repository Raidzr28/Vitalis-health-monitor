package com.vitalis.data

import androidx.room.withTransaction
import com.vitalis.core.database.VitalisDatabase
import com.vitalis.core.database.entity.BodyMeasurementEntity
import com.vitalis.core.database.entity.DailyStatsEntity
import com.vitalis.core.database.entity.toDomain
import com.vitalis.core.database.entity.toEntity
import com.vitalis.core.domain.energy.DailyTargetCalculator
import com.vitalis.core.domain.gamification.StreakCalculator
import com.vitalis.core.model.DailyTargets
import com.vitalis.core.model.GamificationState
import com.vitalis.core.model.MacroTargets
import com.vitalis.core.model.UserProfile
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** What a day is being measured against, plus the counters stored alongside it. */
data class DayStats(val date: LocalDate, val targets: DailyTargets, val steps: Int)

@Singleton
class UserRepository @Inject constructor(private val db: VitalisDatabase) {
    private val userDao = db.userDao()
    private val bodyDao = db.bodyDao()
    private val gamificationDao = db.gamificationDao()

    val profile: Flow<UserProfile?> = userDao.observeProfile().map { it?.toDomain() }

    val latestWeightKg: Flow<Float?> = bodyDao.observeLatestWeight().map { it?.weightKg }

    /** For display: a streak that has already lapsed shows as 0, not as its last stored value. */
    val gamification: Flow<GamificationState> = gamificationDao.observe().map { row ->
        val state = row?.toDomain() ?: GamificationState()
        state.copy(currentStreakDays = StreakCalculator.currentStreak(state, StreakCalculator.effectiveDate(LocalDateTime.now())))
    }

    /** Onboarding's last step (spec §4.1): profile, first weigh-in, today's targets, XP row — all or nothing. */
    suspend fun completeOnboarding(profile: UserProfile, weightKg: Float, today: LocalDate = LocalDate.now()) {
        val now = Instant.now()
        val saved = profile.copy(createdAt = if (profile.createdAt == Instant.EPOCH) now else profile.createdAt, updatedAt = now)
        db.withTransaction {
            userDao.upsertProfile(saved.toEntity())
            upsertWeight(today, weightKg)
            gamificationDao.insertIfAbsent(GamificationState(userId = saved.id).toEntity())
            snapshotTargets(saved, weightKg, today)
        }
    }

    /**
     * Targets for [date], snapshotted on first read of the day so history keeps the budget
     * the user actually had (see [DailyStatsEntity]). Emits null only before onboarding.
     */
    fun observeDay(date: LocalDate): Flow<DayStats?> =
        userDao.observeDailyStats(date)
            .onStart { ensureSnapshot(date) }
            .map { it?.toDayStats() }

    private suspend fun ensureSnapshot(date: LocalDate) {
        if ((userDao.dailyStats(date)?.targetKcal ?: 0) > 0) return
        val profile = userDao.profile()?.toDomain() ?: return
        val weight = bodyDao.latestWeight()?.weightKg ?: return
        // Past days without a snapshot get today's targets — the best estimate we have.
        snapshotTargets(profile, weight, date)
    }

    private suspend fun snapshotTargets(profile: UserProfile, weightKg: Float, date: LocalDate) {
        val t = DailyTargetCalculator.calculate(profile, weightKg.toDouble(), today = date)
        val existing = userDao.dailyStats(date) ?: DailyStatsEntity(date = date)
        userDao.upsertDailyStats(
            existing.copy(
                targetKcal = t.targetKcal,
                targetProteinG = t.macros.proteinG,
                targetCarbsG = t.macros.carbsG,
                targetFatG = t.macros.fatG,
                stepsTarget = t.stepsTarget,
                waterTargetMl = t.waterTargetMl,
                bmrKcal = t.bmrKcal,
                tdeeKcal = t.tdeeKcal,
            ),
        )
    }

    private suspend fun upsertWeight(date: LocalDate, weightKg: Float) {
        val existing = bodyDao.byDate(date)
        bodyDao.upsert(
            existing?.copy(weightKg = weightKg) ?: BodyMeasurementEntity(
                id = UUID.randomUUID().toString(), date = date, weightKg = weightKg,
                bodyFatPercent = null, waistCm = null, hipCm = null, chestCm = null, armCm = null, thighCm = null,
                restingHeartRate = null, hrvMs = null, systolic = null, diastolic = null, sleepMinutes = null,
                spo2Percent = null, photoUri = null, note = null,
            ),
        )
    }
}

private fun DailyStatsEntity.toDayStats() = DayStats(
    date = date,
    targets = DailyTargets(
        bmrKcal = bmrKcal,
        tdeeKcal = tdeeKcal,
        targetKcal = targetKcal,
        macros = MacroTargets(proteinG = targetProteinG, carbsG = targetCarbsG, fatG = targetFatG),
        stepsTarget = stepsTarget,
        waterTargetMl = waterTargetMl,
    ),
    steps = steps,
)
