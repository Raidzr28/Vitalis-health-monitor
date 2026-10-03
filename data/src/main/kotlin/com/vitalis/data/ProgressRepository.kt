package com.vitalis.data

import com.vitalis.core.database.VitalisDatabase
import com.vitalis.core.database.entity.toDomain
import com.vitalis.core.domain.gamification.DayNutrition
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.PersonalRecord
import com.vitalis.core.model.UserProfile
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class WeighIn(val date: LocalDate, val kg: Float)

/** Everything the Progress tab reads, fetched in one go when the tab opens. */
data class ProgressData(
    val profile: UserProfile,
    /** Every weigh-in, oldest first. The onboarding weight is the first one. */
    val weighIns: List<WeighIn>,
    val latestWaistCm: Float?,
    /** Sessions since the start of this month or two weeks ago, whichever is earlier. */
    val sessions: List<ActivitySession>,
    /** Whether the user has ever recorded an activity. Without that, "0 minutes" is missing data, not a score. */
    val hasAnyActivity: Boolean,
    /** The last 14 days' intake against targets, for the nutrition pillar. */
    val days: List<DayNutrition>,
    /** Days with at least one food entry in the last 14 days. */
    val loggedDates: Set<LocalDate>,
    val records: List<PersonalRecord>,
    /** Steps per day over the last 14 days. */
    val stepsByDate: Map<LocalDate, Int> = emptyMap(),
    /** First day the step counter ran; null = steps never tracked. */
    val stepsSince: LocalDate? = null,
)

@Singleton
class ProgressRepository @Inject constructor(private val db: VitalisDatabase) {

    /** Null before onboarding. */
    suspend fun load(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): ProgressData? {
        val profile = db.userDao().profile()?.toDomain() ?: return null
        val twoWeeksAgo = today.minusDays(13)
        val sessionsFrom = minOf(today.withDayOfMonth(1), twoWeeksAgo)
        val g = db.gamificationDao()
        return ProgressData(
            profile = profile,
            weighIns = db.bodyDao().weighIns().mapNotNull { e -> e.weightKg?.let { WeighIn(e.date, it) } },
            latestWaistCm = db.bodyDao().latestWaistCm(),
            sessions = db.activityDao().startedSince(sessionsFrom.atStartOfDay(zone).toInstant()).map { it.toDomain() },
            hasAnyActivity = db.activityDao().hasAny(),
            days = g.dayNutrition().filter { !it.date.isBefore(twoWeeksAgo) }.map {
                DayNutrition(it.date, it.proteinG, it.carbsG, it.fatG, it.waterMl, it.targetProteinG, it.targetCarbsG, it.targetFatG, it.targetWaterMl)
            },
            loggedDates = db.foodDao().observeLoggedDates(twoWeeksAgo, today).first().toSet(),
            records = g.allRecords().map { it.toDomain() },
            stepsByDate = db.stepDao().dailyTotals(twoWeeksAgo).associate { it.date to it.steps },
            stepsSince = db.stepDao().firstDate(),
        )
    }
}
