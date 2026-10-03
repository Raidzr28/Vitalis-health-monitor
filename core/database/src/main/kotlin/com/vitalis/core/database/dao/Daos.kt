package com.vitalis.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.vitalis.core.database.entity.AchievementEntity
import com.vitalis.core.database.entity.ActivitySessionEntity
import com.vitalis.core.database.entity.ActivityTotals
import com.vitalis.core.database.entity.DayNutritionRow
import com.vitalis.core.database.entity.DaySteps
import com.vitalis.core.database.entity.BodyMeasurementEntity
import com.vitalis.core.database.entity.DailyStatsEntity
import com.vitalis.core.database.entity.FoodItemEntity
import com.vitalis.core.database.entity.FoodLogEntity
import com.vitalis.core.database.entity.GamificationStateEntity
import com.vitalis.core.database.entity.LapEntity
import com.vitalis.core.database.entity.LocationPointEntity
import com.vitalis.core.database.entity.PersonalRecordEntity
import com.vitalis.core.database.entity.QuestEntity
import com.vitalis.core.database.entity.SportLastUsed
import com.vitalis.core.database.entity.StepHourEntity
import com.vitalis.core.database.entity.XpHistoryRow
import com.vitalis.core.database.entity.XpLedgerEntity
import com.vitalis.core.database.entity.UserProfileEntity
import com.vitalis.core.database.entity.WaterLogEntity
import com.vitalis.core.model.FoodSource
import com.vitalis.core.model.MealType
import com.vitalis.core.model.SportType
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    /** Single-user app: the one row, or null before onboarding. */
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun profile(): UserProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    fun observeDailyStats(date: LocalDate): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun dailyStats(date: LocalDate): DailyStatsEntity?

    @Upsert
    suspend fun upsertDailyStats(stats: DailyStatsEntity)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_log WHERE date = :date ORDER BY loggedAt")
    fun observeLogs(date: LocalDate): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_log WHERE date = :date AND mealType = :meal ORDER BY loggedAt")
    suspend fun logs(date: LocalDate, meal: MealType): List<FoodLogEntity>

    @Query("SELECT DISTINCT mealType FROM food_log WHERE date = :date")
    suspend fun loggedMeals(date: LocalDate): List<MealType>

    @Query("SELECT DISTINCT date FROM food_log WHERE date BETWEEN :from AND :to")
    fun observeLoggedDates(from: LocalDate, to: LocalDate): Flow<List<LocalDate>>

    @Insert
    suspend fun insertLogs(logs: List<FoodLogEntity>)

    @Query("SELECT * FROM food_log WHERE id = :id")
    suspend fun logById(id: String): FoodLogEntity?

    @Query("DELETE FROM food_log WHERE id = :id")
    suspend fun deleteLog(id: String)

    /** [pattern] is a LIKE pattern with `%`, `_` and `!` escaped by `!` (see NutritionRepository.likePattern). */
    @Query("SELECT * FROM food_item WHERE name LIKE :pattern ESCAPE '!' ORDER BY usageCount DESC, name LIMIT 50")
    fun searchItems(pattern: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_item WHERE usageCount > 0 AND name LIKE :pattern ESCAPE '!' ORDER BY usageCount DESC, name LIMIT 50")
    fun frequentItems(pattern: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_item WHERE source = :source AND name LIKE :pattern ESCAPE '!' ORDER BY name LIMIT 50")
    fun itemsBySource(source: FoodSource, pattern: String): Flow<List<FoodItemEntity>>

    @Query(
        """
        SELECT f.* FROM food_item f
        JOIN (SELECT foodItemId, MAX(loggedAt) AS lastLoggedAt FROM food_log GROUP BY foodItemId) l ON l.foodItemId = f.id
        WHERE f.name LIKE :pattern ESCAPE '!'
        ORDER BY l.lastLoggedAt DESC LIMIT 50
        """,
    )
    fun recentItems(pattern: String): Flow<List<FoodItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemsIfAbsent(items: List<FoodItemEntity>)

    @Upsert
    suspend fun upsertItem(item: FoodItemEntity)

    /** Keeps a user's edits to a catalogue row: only inserts when the item is new. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemIfAbsent(item: FoodItemEntity)

    @Query("UPDATE food_item SET usageCount = usageCount + 1 WHERE id = :id")
    suspend fun bumpUsage(id: String)
}

@Dao
interface WaterDao {
    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_log WHERE date = :date")
    fun observeTotalMl(date: LocalDate): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_log WHERE date = :date")
    suspend fun totalMl(date: LocalDate): Int

    @Insert
    suspend fun insert(entry: WaterLogEntity)
}

@Dao
interface BodyDao {
    @Query("SELECT * FROM body_measurement WHERE weightKg IS NOT NULL ORDER BY date DESC LIMIT 1")
    fun observeLatestWeight(): Flow<BodyMeasurementEntity?>

    @Query("SELECT * FROM body_measurement WHERE weightKg IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun latestWeight(): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement WHERE date = :date")
    suspend fun byDate(date: LocalDate): BodyMeasurementEntity?

    @Query("SELECT * FROM body_measurement WHERE weightKg IS NOT NULL ORDER BY date")
    suspend fun weighIns(): List<BodyMeasurementEntity>

    @Query("SELECT waistCm FROM body_measurement WHERE waistCm IS NOT NULL ORDER BY date DESC LIMIT 1")
    suspend fun latestWaistCm(): Float?

    @Upsert
    suspend fun upsert(entry: BodyMeasurementEntity)
}

@Dao
interface ActivityDao {
    /** Sessions that *started* in `[from, to)` — a run past midnight counts for the day it began. */
    @Query("SELECT * FROM activity_session WHERE startTime >= :from AND startTime < :to ORDER BY startTime")
    fun observeStartedBetween(from: Instant, to: Instant): Flow<List<ActivitySessionEntity>>

    @Query("SELECT * FROM activity_session WHERE startTime >= :from ORDER BY startTime")
    suspend fun startedSince(from: Instant): List<ActivitySessionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM activity_session)")
    suspend fun hasAny(): Boolean

    @Query("SELECT sportType, MAX(startTime) AS lastStart FROM activity_session GROUP BY sportType")
    fun observeLastStartBySport(): Flow<List<SportLastUsed>>

    @Insert
    suspend fun insertSession(session: ActivitySessionEntity)

    @Insert
    suspend fun insertPoints(points: List<LocationPointEntity>)

    @Insert
    suspend fun insertLaps(laps: List<LapEntity>)
}

@Dao
interface GamificationDao {
    @Query("SELECT * FROM gamification_state LIMIT 1")
    fun observe(): Flow<GamificationStateEntity?>

    /** IGNORE so re-running onboarding never resets earned XP or streak. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(state: GamificationStateEntity)

    @Query("SELECT * FROM gamification_state LIMIT 1")
    suspend fun state(): GamificationStateEntity?

    @Upsert
    suspend fun upsertState(state: GamificationStateEntity)

    @Insert
    suspend fun insertLedger(rows: List<XpLedgerEntity>)

    /** Idempotency key for awards: one event (a meal slot, a session, a water day) pays out once. */
    @Query(
        """
        SELECT l.action, l.amount, l.multiplier, l.reason, l.referenceId, l.awardedAt, s.sportType, s.distanceMeters
        FROM xp_ledger l LEFT JOIN activity_session s ON l.referenceId = 'activity:' || s.id
        ORDER BY l.awardedAt DESC, l.id DESC LIMIT :limit
        """,
    )
    fun observeHistory(limit: Int): Flow<List<XpHistoryRow>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_ledger WHERE awardedAt >= :from")
    fun observeXpSince(from: Instant): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM xp_ledger WHERE referenceId = :referenceId)")
    suspend fun hasAward(referenceId: String): Boolean

    @Query("SELECT * FROM personal_record WHERE sportType = :sport")
    suspend fun records(sport: SportType): List<PersonalRecordEntity>

    @Query("SELECT * FROM personal_record")
    suspend fun allRecords(): List<PersonalRecordEntity>

    // Weekly quests

    @Query("SELECT * FROM quest WHERE weekStart = :weekStart")
    suspend fun quests(weekStart: LocalDate): List<QuestEntity>

    @Query("SELECT * FROM quest WHERE weekStart = :weekStart")
    fun observeQuests(weekStart: LocalDate): Flow<List<QuestEntity>>

    @Upsert
    suspend fun upsertQuests(quests: List<QuestEntity>)

    // Badges

    @Query("SELECT * FROM achievement")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievement")
    suspend fun achievements(): List<AchievementEntity>

    @Upsert
    suspend fun upsertAchievements(rows: List<AchievementEntity>)

    @Query(
        "SELECT COALESCE(SUM(distanceMeters), 0) AS distanceM, COALESCE(SUM(elevationGainM), 0) AS elevationM, " +
            "COALESCE(MAX(maxAltitudeM), 0) AS maxAltitudeM FROM activity_session",
    )
    suspend fun activityTotals(): ActivityTotals

    @Query("SELECT startTime FROM activity_session")
    suspend fun activityStarts(): List<Instant>

    @Query(
        """
        SELECT d.date AS date,
            COALESCE((SELECT SUM(f.proteinG) FROM food_log f WHERE f.date = d.date), 0) AS proteinG,
            COALESCE((SELECT SUM(f.carbsG) FROM food_log f WHERE f.date = d.date), 0) AS carbsG,
            COALESCE((SELECT SUM(f.fatG) FROM food_log f WHERE f.date = d.date), 0) AS fatG,
            COALESCE((SELECT SUM(w.amountMl) FROM water_log w WHERE w.date = d.date), 0) AS waterMl,
            d.targetProteinG AS targetProteinG, d.targetCarbsG AS targetCarbsG, d.targetFatG AS targetFatG,
            d.waterTargetMl AS targetWaterMl
        FROM daily_stats d
        """,
    )
    suspend fun dayNutrition(): List<DayNutritionRow>

    /** REPLACE so the (sportType, recordType) unique index swaps the old best for the new one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecords(records: List<PersonalRecordEntity>)
}

@Dao
interface StepDao {
    @Query("SELECT * FROM step_hour WHERE date = :date ORDER BY hour")
    fun observeDay(date: LocalDate): Flow<List<StepHourEntity>>

    @Query("SELECT steps FROM step_hour WHERE date = :date AND hour = :hour")
    suspend fun steps(date: LocalDate, hour: Int): Int?

    @Upsert
    suspend fun upsert(row: StepHourEntity)

    @Query("SELECT COALESCE(SUM(steps), 0) FROM step_hour WHERE date = :date")
    suspend fun dayTotal(date: LocalDate): Int

    @Query("SELECT date, SUM(steps) AS steps FROM step_hour WHERE date >= :from GROUP BY date")
    suspend fun dailyTotals(from: LocalDate): List<DaySteps>

    /** First day the counter was on; days before it are "not tracked", not "0 steps". */
    @Query("SELECT MIN(date) FROM step_hour")
    suspend fun firstDate(): LocalDate?
}
