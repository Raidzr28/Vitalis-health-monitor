package com.vitalis.core.database

import androidx.room.TypeConverter
import com.vitalis.core.model.AchievementCategory
import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.ActivitySource
import com.vitalis.core.model.BmiStandard
import com.vitalis.core.model.CalorieModel
import com.vitalis.core.model.FoodSource
import com.vitalis.core.model.Goal
import com.vitalis.core.model.MealType
import com.vitalis.core.model.QuestType
import com.vitalis.core.model.RecordType
import com.vitalis.core.model.Sex
import com.vitalis.core.model.SportType
import com.vitalis.core.model.TdeeMode
import com.vitalis.core.model.Tier
import com.vitalis.core.model.UnitSystem
import java.time.Instant
import java.time.LocalDate

/**
 * Room type converters.
 *
 * Dates are stored as ISO-8601 text rather than epoch numbers: it sorts
 * chronologically under `ORDER BY` and `BETWEEN`, and it makes a database dump
 * readable when debugging a user's diary.
 *
 * Enums are stored by `name`, never by ordinal — reordering an enum constant
 * would silently rewrite every existing row if ordinals were persisted.
 */
class Converters {

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun sexToString(v: Sex?): String? = v?.name
    @TypeConverter fun stringToSex(v: String?): Sex? = v?.let(Sex::valueOf)

    @TypeConverter fun activityLevelToString(v: ActivityLevel?): String? = v?.name
    @TypeConverter fun stringToActivityLevel(v: String?): ActivityLevel? = v?.let(ActivityLevel::valueOf)

    @TypeConverter fun goalToString(v: Goal?): String? = v?.name
    @TypeConverter fun stringToGoal(v: String?): Goal? = v?.let(Goal::valueOf)

    @TypeConverter fun unitSystemToString(v: UnitSystem?): String? = v?.name
    @TypeConverter fun stringToUnitSystem(v: String?): UnitSystem? = v?.let(UnitSystem::valueOf)

    @TypeConverter fun bmiStandardToString(v: BmiStandard?): String? = v?.name
    @TypeConverter fun stringToBmiStandard(v: String?): BmiStandard? = v?.let(BmiStandard::valueOf)

    @TypeConverter fun tdeeModeToString(v: TdeeMode?): String? = v?.name
    @TypeConverter fun stringToTdeeMode(v: String?): TdeeMode? = v?.let(TdeeMode::valueOf)

    @TypeConverter fun mealTypeToString(v: MealType?): String? = v?.name
    @TypeConverter fun stringToMealType(v: String?): MealType? = v?.let(MealType::valueOf)

    @TypeConverter fun foodSourceToString(v: FoodSource?): String? = v?.name
    @TypeConverter fun stringToFoodSource(v: String?): FoodSource? = v?.let(FoodSource::valueOf)

    @TypeConverter fun sportTypeToString(v: SportType?): String? = v?.name
    @TypeConverter fun stringToSportType(v: String?): SportType? = v?.let(SportType::valueOf)

    @TypeConverter fun activitySourceToString(v: ActivitySource?): String? = v?.name
    @TypeConverter fun stringToActivitySource(v: String?): ActivitySource? = v?.let(ActivitySource::valueOf)

    @TypeConverter fun calorieModelToString(v: CalorieModel?): String? = v?.name
    @TypeConverter fun stringToCalorieModel(v: String?): CalorieModel? = v?.let(CalorieModel::valueOf)

    @TypeConverter fun recordTypeToString(v: RecordType?): String? = v?.name
    @TypeConverter fun stringToRecordType(v: String?): RecordType? = v?.let(RecordType::valueOf)

    @TypeConverter fun achievementCategoryToString(v: AchievementCategory?): String? = v?.name
    @TypeConverter fun stringToAchievementCategory(v: String?): AchievementCategory? = v?.let(AchievementCategory::valueOf)

    @TypeConverter fun tierToString(v: Tier?): String? = v?.name
    @TypeConverter fun stringToTier(v: String?): Tier? = v?.let(Tier::valueOf)

    @TypeConverter fun questTypeToString(v: QuestType?): String? = v?.name
    @TypeConverter fun stringToQuestType(v: String?): QuestType? = v?.let(QuestType::valueOf)
}
