package com.vitalis.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vitalis.core.model.FoodItem
import com.vitalis.core.model.FoodLog
import com.vitalis.core.model.FoodSource
import com.vitalis.core.model.Macros
import com.vitalis.core.model.MealType
import java.time.Instant
import java.time.LocalDate

/**
 * The food catalogue: USDA and Open Food Facts rows cached locally, plus the
 * curated Indonesian seed and anything the user created (spec §4.2.5).
 *
 * [barcode] is indexed because scan-to-log has to feel instant, and [name] because
 * search-as-you-type runs on every keystroke.
 */
@Entity(
    tableName = "food_item",
    indices = [Index("barcode"), Index("name"), Index("usageCount")],
)
data class FoodItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val brand: String?,
    val barcode: String?,
    val source: FoodSource,
    val servingSizeG: Float,
    val servingLabel: String,
    val kcalPer100g: Float,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val fiberPer100g: Float?,
    val sugarPer100g: Float?,
    val satFatPer100g: Float?,
    val sodiumMgPer100g: Float?,
    val cholesterolMgPer100g: Float?,
    val potassiumMgPer100g: Float?,
    val isVerified: Boolean,
    /** Drives the "frequent" list — the cheapest way to cut logging to one tap. */
    val usageCount: Int,
    val updatedAt: Instant,
)

fun FoodItemEntity.toDomain() = FoodItem(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    source = source,
    servingSizeG = servingSizeG,
    servingLabel = servingLabel,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    sugarPer100g = sugarPer100g,
    satFatPer100g = satFatPer100g,
    sodiumMgPer100g = sodiumMgPer100g,
    cholesterolMgPer100g = cholesterolMgPer100g,
    potassiumMgPer100g = potassiumMgPer100g,
    isVerified = isVerified,
    usageCount = usageCount,
    updatedAt = updatedAt,
)

fun FoodItem.toEntity() = FoodItemEntity(
    id = id,
    name = name,
    brand = brand,
    barcode = barcode,
    source = source,
    servingSizeG = servingSizeG,
    servingLabel = servingLabel,
    kcalPer100g = kcalPer100g,
    proteinPer100g = proteinPer100g,
    carbsPer100g = carbsPer100g,
    fatPer100g = fatPer100g,
    fiberPer100g = fiberPer100g,
    sugarPer100g = sugarPer100g,
    satFatPer100g = satFatPer100g,
    sodiumMgPer100g = sodiumMgPer100g,
    cholesterolMgPer100g = cholesterolMgPer100g,
    potassiumMgPer100g = potassiumMgPer100g,
    isVerified = isVerified,
    usageCount = usageCount,
    updatedAt = updatedAt,
)

/**
 * A diary entry.
 *
 * Nutrition is denormalised on purpose: the numbers are snapshotted at log time so
 * that correcting a catalogue row — which happens constantly with crowdsourced
 * data — never retroactively changes what the user's history says they ate.
 * [foodName] is copied for the same reason, and so the diary still renders if the
 * source row is deleted.
 */
@Entity(
    tableName = "food_log",
    indices = [Index("date"), Index("foodItemId"), Index(value = ["date", "mealType"])],
)
data class FoodLogEntity(
    @PrimaryKey val id: String,
    val foodItemId: String,
    val foodName: String,
    val brand: String?,
    val date: LocalDate,
    val mealType: MealType,
    val quantity: Float,
    val unit: String,
    val kcal: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val loggedAt: Instant,
    val isSynced: Boolean = false,
)

fun FoodLogEntity.toDomain() = FoodLog(
    id = id,
    foodItemId = foodItemId,
    foodName = foodName,
    brand = brand,
    date = date,
    mealType = mealType,
    quantity = quantity,
    unit = unit,
    macros = Macros(kcal = kcal, proteinG = proteinG, carbsG = carbsG, fatG = fatG),
    loggedAt = loggedAt,
)

fun FoodLog.toEntity() = FoodLogEntity(
    id = id,
    foodItemId = foodItemId,
    foodName = foodName,
    brand = brand,
    date = date,
    mealType = mealType,
    quantity = quantity,
    unit = unit,
    kcal = macros.kcal,
    proteinG = macros.proteinG,
    carbsG = macros.carbsG,
    fatG = macros.fatG,
    loggedAt = loggedAt,
)

/** Individual sips, kept as rows so a mistaken tap can be undone (spec §11.2). */
@Entity(tableName = "water_log", indices = [Index("date")])
data class WaterLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val amountMl: Int,
    val loggedAt: Instant,
)
