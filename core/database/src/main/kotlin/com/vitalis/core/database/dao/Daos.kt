package com.vitalis.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.vitalis.core.database.entity.ActivitySessionEntity
import com.vitalis.core.database.entity.BodyMeasurementEntity
import com.vitalis.core.database.entity.DailyStatsEntity
import com.vitalis.core.database.entity.FoodItemEntity
import com.vitalis.core.database.entity.FoodLogEntity
import com.vitalis.core.database.entity.GamificationStateEntity
import com.vitalis.core.database.entity.UserProfileEntity
import com.vitalis.core.database.entity.WaterLogEntity
import com.vitalis.core.model.FoodSource
import com.vitalis.core.model.MealType
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

    @Upsert
    suspend fun upsert(entry: BodyMeasurementEntity)
}

@Dao
interface ActivityDao {
    /** Sessions that *started* in `[from, to)` — a run past midnight counts for the day it began. */
    @Query("SELECT * FROM activity_session WHERE startTime >= :from AND startTime < :to ORDER BY startTime")
    fun observeStartedBetween(from: Instant, to: Instant): Flow<List<ActivitySessionEntity>>
}

@Dao
interface GamificationDao {
    @Query("SELECT * FROM gamification_state LIMIT 1")
    fun observe(): Flow<GamificationStateEntity?>

    /** IGNORE so re-running onboarding never resets earned XP or streak. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(state: GamificationStateEntity)
}
