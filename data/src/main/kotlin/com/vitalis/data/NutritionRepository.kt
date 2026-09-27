package com.vitalis.data

import androidx.room.withTransaction
import com.vitalis.core.database.VitalisDatabase
import com.vitalis.core.database.entity.FoodLogEntity
import com.vitalis.core.database.entity.WaterLogEntity
import com.vitalis.core.database.entity.toDomain
import com.vitalis.core.database.entity.toEntity
import com.vitalis.core.model.FoodItem
import com.vitalis.core.model.FoodLog
import com.vitalis.core.model.FoodSource
import com.vitalis.core.model.MealType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class FoodFilter { ALL, RECENT, FREQUENT, MINE }

/** Substring match with the user's `%`, `_` and `!` taken literally (the DAO uses ESCAPE '!'). */
fun likePattern(query: String): String {
    val escaped = query.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_")
    return "%$escaped%"
}

@Singleton
class NutritionRepository @Inject constructor(
    private val db: VitalisDatabase,
    private val gamification: GamificationRepository,
) {
    private val foodDao = db.foodDao()
    private val waterDao = db.waterDao()

    fun observeLogs(date: LocalDate): Flow<List<FoodLog>> = foodDao.observeLogs(date).map { rows -> rows.map { it.toDomain() } }

    fun observeLoggedDates(from: LocalDate, to: LocalDate): Flow<Set<LocalDate>> = foodDao.observeLoggedDates(from, to).map { it.toSet() }

    /** Snapshots the food's nutrition into the log row (see [FoodLogEntity]). */
    suspend fun log(food: FoodItem, servings: Float, meal: MealType, date: LocalDate) {
        require(servings > 0f) { "servings must be positive" }
        val macros = food.nutritionForServings(servings)
        db.withTransaction {
            foodDao.insertItemIfAbsent(food.toEntity())
            foodDao.bumpUsage(food.id)
            foodDao.insertLogs(
                listOf(
                    FoodLog(
                        id = UUID.randomUUID().toString(), foodItemId = food.id, foodName = food.name, brand = food.brand,
                        date = date, mealType = meal, quantity = servings, unit = food.servingLabel,
                        macros = macros, loggedAt = Instant.now(),
                    ).toEntity(),
                ),
            )
            gamification.onMealLogged(date, meal, foodDao.loggedMeals(date))
        }
    }

    /** "Ulangi menu kemarin" (spec US-12). Returns how many entries were copied. */
    suspend fun copyMeal(from: LocalDate, to: LocalDate, meal: MealType): Int {
        val source = foodDao.logs(from, meal)
        val now = Instant.now()
        db.withTransaction {
            foodDao.insertLogs(source.map { it.copy(id = UUID.randomUUID().toString(), date = to, loggedAt = now, isSynced = false) })
            if (source.isNotEmpty()) gamification.onMealLogged(to, meal, foodDao.loggedMeals(to))
        }
        return source.size
    }

    /** Deletes and returns the entry so the UI can offer Undo instead of a confirm dialog (spec §11.2). */
    suspend fun deleteLog(id: String): FoodLog? = db.withTransaction {
        foodDao.logById(id)?.also { foodDao.deleteLog(id) }?.toDomain()
    }

    suspend fun restoreLog(log: FoodLog) = foodDao.insertLogs(listOf(log.toEntity()))

    // ---- Catalogue -----------------------------------------------------------

    /** Insert-if-absent, so it is cheap to call on every search screen open and never overwrites edits. */
    suspend fun seedLocalCatalogue() = foodDao.insertItemsIfAbsent(LocalFoodSeed.items.map { it.toEntity() })

    fun search(query: String, filter: FoodFilter): Flow<List<FoodItem>> {
        val pattern = likePattern(query)
        val rows = when (filter) {
            FoodFilter.ALL -> foodDao.searchItems(pattern)
            FoodFilter.RECENT -> foodDao.recentItems(pattern)
            FoodFilter.FREQUENT -> foodDao.frequentItems(pattern)
            FoodFilter.MINE -> foodDao.itemsBySource(FoodSource.USER, pattern)
        }
        return rows.map { list -> list.map { it.toDomain() } }
    }

    /** Custom food from a nutrition label (spec §4.2.1). Private to this device, source USER. */
    suspend fun saveCustomFood(item: FoodItem) {
        require(item.source == FoodSource.USER) { "custom foods must be USER-sourced" }
        foodDao.upsertItem(item.copy(updatedAt = Instant.now()).toEntity())
    }

    fun observeWaterMl(date: LocalDate): Flow<Int> = waterDao.observeTotalMl(date)

    suspend fun addWater(date: LocalDate, ml: Int) {
        require(ml in 1..5000) { "implausible water amount: $ml" }
        db.withTransaction {
            waterDao.insert(WaterLogEntity(date = date, amountMl = ml, loggedAt = Instant.now()))
            val target = db.userDao().dailyStats(date)?.waterTargetMl ?: 0
            if (target > 0 && waterDao.totalMl(date) >= target) gamification.onWaterTargetHit(date)
        }
    }
}
