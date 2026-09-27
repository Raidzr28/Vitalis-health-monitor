package com.vitalis.data

import androidx.room.withTransaction
import com.vitalis.core.database.VitalisDatabase
import com.vitalis.core.database.entity.XpLedgerEntity
import com.vitalis.core.database.entity.toDomain
import com.vitalis.core.database.entity.toEntity
import com.vitalis.core.domain.gamification.PersonalRecordDetector
import com.vitalis.core.domain.gamification.StreakCalculator
import com.vitalis.core.domain.gamification.XpCalculator
import com.vitalis.core.domain.gamification.XpGranter
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.MealType
import com.vitalis.core.model.PersonalRecord
import com.vitalis.core.model.SportType
import com.vitalis.core.model.XpAction
import com.vitalis.core.model.XpAward
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** A moment worth a full-screen celebration, queued when XP is granted (spec §9.2–9.3). */
sealed interface Celebration {
    /** [totalXp] is after the award, so the screen can show both the old and the new level's progress. */
    data class LevelUp(val fromLevel: Int, val toLevel: Int, val xpGained: Int, val awards: List<XpAward>, val totalXp: Long) : Celebration
    data class StreakMilestone(val days: Int, val bonusXp: Int, val lastActiveDate: LocalDate, val freezes: Int) : Celebration
}

/** One paid award, with the session it came from when it came from one. */
data class XpHistoryEntry(val award: XpAward, val referenceId: String, val awardedAt: Instant, val sport: SportType?, val distanceMeters: Double?)

/** What a finished session is worth, shown on the summary before save and paid out on save. */
data class ActivityRewards(val records: List<PersonalRecord>, val awards: List<XpAward>) {
    /** First-ever values are stored as records but are not "broken" — nothing was beaten. */
    val beaten: List<PersonalRecord> get() = records.filter { it.previousValue != null }
}

/**
 * The only writer of XP, streak and personal records (spec §9). Every award carries a
 * reference id and pays out at most once, so deleting and re-logging a meal or re-saving
 * a session cannot farm XP.
 */
@Singleton
class GamificationRepository @Inject constructor(private val db: VitalisDatabase) {
    private val dao = db.gamificationDao()

    // ponytail: in-memory queue — a celebration pending when the process dies is simply skipped.
    private val _celebrations = MutableStateFlow<List<Celebration>>(emptyList())
    val celebrations: StateFlow<List<Celebration>> = _celebrations.asStateFlow()

    fun dismissCelebration() = _celebrations.update { it.drop(1) }

    // ponytail: newest 500 awards (months of normal use); page it if anyone scrolls that far.
    fun observeHistory(limit: Int = 500): Flow<List<XpHistoryEntry>> = dao.observeHistory(limit).map { rows ->
        rows.mapNotNull { r ->
            val action = XpAction.entries.find { it.name == r.action } ?: return@mapNotNull null
            XpHistoryEntry(XpAward(action, r.amount, r.multiplier, r.reason), r.referenceId.orEmpty(), r.awardedAt, r.sportType, r.distanceMeters)
        }
    }

    fun observeXpSince(from: Instant): Flow<Int> = dao.observeXpSince(from)

    /** +10 per meal slot per day, +30 once breakfast, lunch and dinner are all in. */
    suspend fun onMealLogged(date: LocalDate, meal: MealType, loggedMeals: Collection<MealType>) {
        grant("meal:$date:$meal", listOf(XpAward(XpAction.LOG_MEAL, XpAction.LOG_MEAL.baseXp)), streakDay())
        if (loggedMeals.containsAll(FULL_DAY)) {
            grant("daylog:$date", listOf(XpAward(XpAction.COMPLETE_DAY_LOG, XpAction.COMPLETE_DAY_LOG.baseXp)), streakDay = null)
        }
    }

    suspend fun onWaterTargetHit(date: LocalDate) =
        grant("water:$date", listOf(XpAward(XpAction.HIT_WATER_TARGET, XpAction.HIT_WATER_TARGET.baseXp)), streakDay = null)

    /** Read-only preview for the summary screen. */
    suspend fun activityRewards(session: ActivitySession): ActivityRewards {
        val existing = dao.records(session.sportType).map { it.toDomain() }
        val records = PersonalRecordDetector.detect(session, existing)
        val beaten = records.any { it.previousValue != null }
        return ActivityRewards(records, XpCalculator.forActivity(session.distanceMeters ?: 0.0, session.elevationGainM ?: 0.0, beaten))
    }

    /** Call inside the transaction that saves [session]. */
    suspend fun onActivitySaved(session: ActivitySession) = db.withTransaction {
        val rewards = activityRewards(session)
        dao.upsertRecords(rewards.records.map { it.toEntity() })
        // A session too short for XP still keeps the streak alive — the user did move.
        grant("activity:${session.id}", rewards.awards, streakDay())
    }

    private suspend fun grant(referenceId: String, awards: List<XpAward>, streakDay: LocalDate?) = db.withTransaction {
        if (awards.isEmpty() && streakDay == null) return@withTransaction
        if (dao.hasAward(referenceId)) return@withTransaction
        val state = dao.state()?.toDomain() ?: return@withTransaction // before onboarding: nothing to credit
        val result = XpGranter.grant(state, awards, streakDay)
        val now = Instant.now()
        dao.insertLedger(
            result.awards.map { XpLedgerEntity(action = it.action.name, amount = it.amount, multiplier = it.multiplier, reason = it.reason, referenceId = referenceId, awardedAt = now) },
        )
        dao.upsertState(result.state.toEntity())

        val next = result.state
        val queued = buildList {
            result.milestone?.let { days ->
                val bonus = XpCalculator.forStreakMilestone(days)?.amount ?: 0
                add(Celebration.StreakMilestone(days, bonus, next.lastActiveDate ?: LocalDate.now(), next.streakFreezesRemaining))
            }
            if (next.level > state.level) add(Celebration.LevelUp(state.level, next.level, result.xpGained, result.awards, next.totalXp))
        }
        if (queued.isNotEmpty()) _celebrations.update { it + queued }
    }

    /** Streaks follow when the user acted, not the diary date, so back-filling cannot extend them. */
    private fun streakDay() = StreakCalculator.effectiveDate(LocalDateTime.now())

    private companion object {
        val FULL_DAY = setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)
    }
}
