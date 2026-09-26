package com.vitalis.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vitalis.core.database.dao.ActivityDao
import com.vitalis.core.database.dao.BodyDao
import com.vitalis.core.database.dao.FoodDao
import com.vitalis.core.database.dao.GamificationDao
import com.vitalis.core.database.dao.UserDao
import com.vitalis.core.database.dao.WaterDao
import com.vitalis.core.database.entity.AchievementEntity
import com.vitalis.core.database.entity.ActivitySessionEntity
import com.vitalis.core.database.entity.BodyMeasurementEntity
import com.vitalis.core.database.entity.DailyStatsEntity
import com.vitalis.core.database.entity.FoodItemEntity
import com.vitalis.core.database.entity.FoodLogEntity
import com.vitalis.core.database.entity.GamificationStateEntity
import com.vitalis.core.database.entity.LapEntity
import com.vitalis.core.database.entity.LocationPointEntity
import com.vitalis.core.database.entity.PersonalRecordEntity
import com.vitalis.core.database.entity.QuestEntity
import com.vitalis.core.database.entity.UserProfileEntity
import com.vitalis.core.database.entity.WaterLogEntity
import com.vitalis.core.database.entity.XpLedgerEntity

/**
 * Single source of truth (spec §6.3). Every table exists from v1 so later sprints
 * add DAOs, not migrations. Bump [version] and add a Migration for any schema change —
 * never fallbackToDestructiveMigration, it would wipe the user's diary.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        DailyStatsEntity::class,
        FoodItemEntity::class,
        FoodLogEntity::class,
        WaterLogEntity::class,
        BodyMeasurementEntity::class,
        ActivitySessionEntity::class,
        LocationPointEntity::class,
        LapEntity::class,
        GamificationStateEntity::class,
        XpLedgerEntity::class,
        AchievementEntity::class,
        PersonalRecordEntity::class,
        QuestEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class VitalisDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun foodDao(): FoodDao
    abstract fun waterDao(): WaterDao
    abstract fun bodyDao(): BodyDao
    abstract fun activityDao(): ActivityDao
    abstract fun gamificationDao(): GamificationDao
}
