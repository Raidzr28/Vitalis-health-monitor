package com.vitalis.data

import androidx.room.withTransaction
import com.vitalis.core.database.VitalisDatabase
import com.vitalis.core.database.entity.AchievementEntity
import com.vitalis.core.database.entity.XpLedgerEntity
import com.vitalis.core.database.entity.toDomain
import com.vitalis.core.database.entity.toEntity
import com.vitalis.core.domain.gamification.BadgeCatalog
import com.vitalis.core.domain.gamification.BadgeDefinition
import com.vitalis.core.domain.gamification.BadgeEvaluator
import com.vitalis.core.domain.gamification.DayNutrition
import com.vitalis.core.domain.gamification.PersonalRecordDetector
import com.vitalis.core.domain.gamification.QuestHistory
import com.vitalis.core.domain.gamification.QuestSession
import com.vitalis.core.domain.gamification.WeeklyQuests
import com.vitalis.core.domain.gamification.StreakCalculator
import com.vitalis.core.domain.gamification.XpCalculator
import com.vitalis.core.domain.gamification.XpGranter
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.MealType
import com.vitalis.core.model.PersonalRecord
import com.vitalis.core.model.Quest
import com.vitalis.core.model.QuestType
import com.vitalis.core.model.SportType
import com.vitalis.core.model.XpAction
import com.vitalis.core.model.XpAward
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** A moment worth a full-screen celebration, queued when XP is granted (spec §9.2–9.3). */
sealed interface Celebration {
    /** [totalXp] is after the award, so the screen can show both the old and the new level's progress. */
    data class LevelUp(val fromLevel: Int, val toLevel: Int, val xpGained: Int, val awards: List<XpAward>, val totalXp: Long) : Celebration
    data class StreakMilestone(val days: Int, val bonusXp: Int, val lastActiveDate: LocalDate, val freezes: Int) : Celebration
    data class BadgeUnlocked(val badge: BadgeDefinition) : Celebration
}

/** A badge with where the user stands on it. [value] is progress in the badge's own unit (km, days). */
data class BadgeProgress(val badge: BadgeDefinition, val progress: Float, val unlockedAt: Instant?) {
    val unlocked: Boolean get() = unlockedAt != null
    val value: Double get() = progress * badge.threshold
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

    /** This week's quests in generation order (distance, logging, activity or elevation, protein). */
    fun observeQuests(weekStart: LocalDate): Flow<List<Quest>> = dao.observeQuests(weekStart).map { rows ->
        rows.map { it.toDomain() }.sortedBy { QUEST_ORDER.indexOf(it.type) }
    }

    /**
     * Creates this week's quests the first time they are needed, updates their progress, and pays
     * each finished quest once, plus the all-four bonus (spec §9.5). A finished quest stays finished
     * even if an entry behind it is deleted later. Runs after meals and activities, at onboarding,
     * and when Profile opens.
     */
    suspend fun refreshQuests(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()) = db.withTransaction {
        if (dao.state() == null) return@withTransaction // before onboarding
        val week = WeeklyQuests.weekStart(today)
        val history = questHistory(from = week.minusWeeks(4), to = week.plusDays(6), zone)
        val quests = dao.quests(week).map { it.toDomain() }.ifEmpty { WeeklyQuests.generate(week, history) }
        val now = Instant.now()
        val updated = quests.map { q ->
            val current = WeeklyQuests.progress(q.type, week, history)
            q.copy(current = current, completedAt = q.completedAt ?: now.takeIf { current >= q.target })
        }
        dao.upsertQuests(updated.map { it.toEntity() })
        updated.filter { it.completedAt != null }.forEach { q ->
            grant("quest:$week:${q.type}", listOf(XpAward(XpAction.COMPLETE_WEEKLY_QUEST, q.xpReward, reason = "quest")), streakDay = null)
        }
        if (updated.all { it.completedAt != null }) {
            grant("quest-chest:$week", listOf(XpAward(XpAction.COMPLETE_WEEKLY_QUEST, WeeklyQuests.ALL_DONE_BONUS_XP, reason = "quest_chest")), streakDay = null)
        }
    }

    private suspend fun questHistory(from: LocalDate, to: LocalDate, zone: ZoneId): QuestHistory {
        val sessions = db.activityDao().startedSince(from.atStartOfDay(zone).toInstant()).map {
            QuestSession(LocalDateTime.ofInstant(it.startTime, zone).toLocalDate(), it.distanceMeters ?: 0.0, it.elevationGainM ?: 0.0)
        }
        return QuestHistory(
            sessions = sessions,
            loggedDates = db.foodDao().observeLoggedDates(from, to).first().toSet(),
            proteinHitDates = dao.dayNutrition()
                .filter { !it.date.isBefore(from) && it.targetProteinG > 0 && it.proteinG >= it.targetProteinG }
                .map { it.date }.toSet(),
            joined = db.bodyDao().weighIns().firstOrNull()?.date,
        )
    }

    /** Every badge in catalogue order, with stored progress (none stored yet reads as 0). */
    val badges: Flow<List<BadgeProgress>> = dao.observeAchievements().map { rows ->
        val byId = rows.associateBy { it.id }
        BadgeCatalog.ALL.map { def -> byId[def.id].let { BadgeProgress(def, it?.progress ?: 0f, it?.unlockedAt) } }
    }

    /**
     * Recomputes every badge from lifetime stats and queues a celebration for each new unlock.
     * Runs after meals, water and activities, and when Profile opens. That last one also
     * back-fills badges for history recorded before badges existed and counts finished days
     * toward Macro Master. An unlock is permanent even if a later delete lowers the numbers.
     *
     * The very first refresh (no rows yet) is a silent back-fill: an existing install should not
     * get six celebrations in a row for history. New users get their rows at onboarding, so their
     * first real unlock is still celebrated.
     */
    suspend fun refreshBadges(zone: ZoneId = ZoneId.systemDefault()) = db.withTransaction {
        val totals = dao.activityTotals()
        val stats = BadgeEvaluator.stats(
            totalDistanceM = totals.distanceM,
            totalElevationM = totals.elevationM,
            maxAltitudeM = totals.maxAltitudeM,
            longestStreakDays = dao.state()?.longestStreakDays ?: 0,
            activityStarts = dao.activityStarts().map { LocalDateTime.ofInstant(it, zone) },
            days = dao.dayNutrition().map {
                DayNutrition(it.date, it.proteinG, it.carbsG, it.fatG, it.waterMl, it.targetProteinG, it.targetCarbsG, it.targetFatG, it.targetWaterMl)
            },
            today = LocalDate.now(zone),
        )
        val stored = dao.achievements().associateBy { it.id }
        val announce = stored.isNotEmpty()
        val now = Instant.now()
        val unlocked = mutableListOf<BadgeDefinition>()
        val rows = BadgeCatalog.ALL.map { def ->
            val progress = BadgeEvaluator.progress(def, stats)
            val unlockedAt = stored[def.id]?.unlockedAt ?: now.takeIf { progress >= 1f }?.also { unlocked += def }
            AchievementEntity(
                id = def.id, category = def.category, tier = def.tier, titleKey = def.title, descriptionKey = def.metric.name,
                threshold = def.threshold, progress = progress, unlockedAt = unlockedAt, isHidden = def.hidden, sportType = null,
            )
        }
        dao.upsertAchievements(rows)
        if (announce && unlocked.isNotEmpty()) _celebrations.update { it + unlocked.map(Celebration::BadgeUnlocked) }
    }

    /** +10 per meal slot per day, +30 once breakfast, lunch and dinner are all in. */
    suspend fun onMealLogged(date: LocalDate, meal: MealType, loggedMeals: Collection<MealType>) {
        grant("meal:$date:$meal", listOf(XpAward(XpAction.LOG_MEAL, XpAction.LOG_MEAL.baseXp)), streakDay())
        if (loggedMeals.containsAll(FULL_DAY)) {
            grant("daylog:$date", listOf(XpAward(XpAction.COMPLETE_DAY_LOG, XpAction.COMPLETE_DAY_LOG.baseXp)), streakDay = null)
        }
        refreshQuests()
        refreshBadges()
    }

    suspend fun onWaterTargetHit(date: LocalDate) {
        grant("water:$date", listOf(XpAward(XpAction.HIT_WATER_TARGET, XpAction.HIT_WATER_TARGET.baseXp)), streakDay = null)
        refreshBadges()
    }

    /** +25 once per day when the step target is reached (spec §9.2). */
    suspend fun onStepTargetHit(date: LocalDate) =
        grant("steps:$date", listOf(XpAward(XpAction.HIT_STEP_TARGET, XpAction.HIT_STEP_TARGET.baseXp)), streakDay = null)

    /** +15 once per day, however many times the scale is corrected (spec §9.2). */
    suspend fun onWeightLogged(date: LocalDate) =
        grant("weight:$date", listOf(XpAward(XpAction.LOG_WEIGHT, XpAction.LOG_WEIGHT.baseXp)), streakDay = null)

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
        refreshQuests()
        refreshBadges()
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
        val QUEST_ORDER = listOf(QuestType.DISTANCE_KM, QuestType.LOG_DAYS, QuestType.ACTIVITY_COUNT, QuestType.ELEVATION_M, QuestType.PROTEIN_DAYS, QuestType.STEP_DAYS)
    }
}
