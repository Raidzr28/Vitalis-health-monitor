package com.vitalis.core.domain.gamification

import com.vitalis.core.model.AchievementCategory
import com.vitalis.core.model.AchievementCategory.CONSISTENCY
import com.vitalis.core.model.AchievementCategory.DISTANCE
import com.vitalis.core.model.AchievementCategory.ELEVATION
import com.vitalis.core.model.AchievementCategory.HIDDEN
import com.vitalis.core.model.AchievementCategory.NUTRITION
import com.vitalis.core.model.BadgeMetric
import com.vitalis.core.model.Tier
import com.vitalis.core.model.Tier.BRONZE
import com.vitalis.core.model.Tier.GOLD
import com.vitalis.core.model.Tier.PLATINUM
import com.vitalis.core.model.Tier.SILVER
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

/** One badge. The catalogue in code is the source of truth; the database only keeps progress and unlock time. */
data class BadgeDefinition(
    val id: String,
    val title: String,
    val category: AchievementCategory,
    val tier: Tier,
    val metric: BadgeMetric,
    val threshold: Double,
) {
    /** Hidden badges are easter eggs: shown as a silhouette until earned (spec §9.4). */
    val hidden: Boolean get() = category == HIDDEN
}

/**
 * The badge set from spec §9.4. Tier marks rarity, so a ladder climbs Bronze → Platinum.
 * ponytail: distance and elevation are lifetime totals across all sports; per-sport ladders
 * and "Explorer" (10 districts, needs reverse geocoding) come later.
 */
object BadgeCatalog {
    private fun b(id: String, title: String, cat: AchievementCategory, tier: Tier, metric: BadgeMetric, threshold: Double) =
        BadgeDefinition(id, title, cat, tier, metric, threshold)

    val ALL: List<BadgeDefinition> = listOf(
        b("first_steps", "First Steps", DISTANCE, BRONZE, BadgeMetric.DISTANCE_KM, 1.0),
        b("getting_started", "Getting Started", DISTANCE, BRONZE, BadgeMetric.DISTANCE_KM, 10.0),
        b("road_warrior", "Road Warrior", DISTANCE, SILVER, BadgeMetric.DISTANCE_KM, 100.0),
        b("century", "Century", DISTANCE, GOLD, BadgeMetric.DISTANCE_KM, 500.0),
        b("marathoner", "Marathoner", DISTANCE, GOLD, BadgeMetric.DISTANCE_KM, 1_000.0),
        b("ultra", "Ultra", DISTANCE, PLATINUM, BadgeMetric.DISTANCE_KM, 5_000.0),

        b("hill_starter", "Hill Starter", ELEVATION, BRONZE, BadgeMetric.ELEVATION_M, 100.0),
        b("climber", "Climber", ELEVATION, SILVER, BadgeMetric.ELEVATION_M, 1_000.0),
        b("everest", "Everest", ELEVATION, GOLD, BadgeMetric.ELEVATION_M, 8_848.0),
        b("above_the_clouds", "Above the Clouds", ELEVATION, PLATINUM, BadgeMetric.ELEVATION_M, 29_029.0),

        b("week_one", "Week One", CONSISTENCY, BRONZE, BadgeMetric.LONGEST_STREAK_DAYS, 7.0),
        b("habit_formed", "Habit Formed", CONSISTENCY, SILVER, BadgeMetric.LONGEST_STREAK_DAYS, 30.0),
        b("centurion", "Centurion", CONSISTENCY, GOLD, BadgeMetric.LONGEST_STREAK_DAYS, 100.0),
        b("year_of_you", "Year of You", CONSISTENCY, PLATINUM, BadgeMetric.LONGEST_STREAK_DAYS, 365.0),

        b("macro_master", "Macro Master", NUTRITION, SILVER, BadgeMetric.MACRO_DAYS, 7.0),
        b("protein_pro", "Protein Pro", NUTRITION, SILVER, BadgeMetric.PROTEIN_DAYS, 14.0),
        b("hydrated", "Hydrated", NUTRITION, GOLD, BadgeMetric.WATER_DAYS, 30.0),

        b("night_owl", "Night Owl", HIDDEN, BRONZE, BadgeMetric.NIGHT_ACTIVITIES, 1.0),
        b("sunrise_chaser", "Sunrise Chaser", HIDDEN, SILVER, BadgeMetric.SUNRISE_ACTIVITIES, 10.0),
        b("rain_or_shine", "Rain or Shine", HIDDEN, SILVER, BadgeMetric.ACTIVE_DAY_RUN, 7.0),
        b("summit_seeker", "Summit Seeker", HIDDEN, GOLD, BadgeMetric.MAX_ALTITUDE_M, 2_000.0),
    )
}

/** One day's intake against that day's snapshotted targets. */
data class DayNutrition(
    val date: LocalDate,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val waterMl: Int,
    val targetProteinG: Int,
    val targetCarbsG: Int,
    val targetFatG: Int,
    val targetWaterMl: Int,
)

object BadgeEvaluator {
    /** "Macros in target" means each of protein, carbs and fat within ±10% of its target. */
    const val MACRO_TOLERANCE = .10

    /**
     * Lifetime numbers every badge is measured against.
     *
     * Macro days only count finished days (before [today]): eating more later can still push
     * today out of range. Protein and water only go up during a day, so today counts once reached.
     */
    fun stats(
        totalDistanceM: Double,
        totalElevationM: Double,
        maxAltitudeM: Double,
        longestStreakDays: Int,
        activityStarts: List<LocalDateTime>,
        days: List<DayNutrition>,
        today: LocalDate,
    ): Map<BadgeMetric, Double> = mapOf(
        BadgeMetric.DISTANCE_KM to totalDistanceM / 1000.0,
        BadgeMetric.ELEVATION_M to totalElevationM,
        BadgeMetric.MAX_ALTITUDE_M to maxAltitudeM,
        BadgeMetric.LONGEST_STREAK_DAYS to longestStreakDays.toDouble(),
        BadgeMetric.NIGHT_ACTIVITIES to activityStarts.count { it.hour < 4 }.toDouble(),
        BadgeMetric.SUNRISE_ACTIVITIES to activityStarts.count { it.hour in 4..5 }.toDouble(),
        BadgeMetric.ACTIVE_DAY_RUN to longestRun(activityStarts.map { it.toLocalDate() }).toDouble(),
        BadgeMetric.PROTEIN_DAYS to days.count { it.targetProteinG > 0 && it.proteinG >= it.targetProteinG }.toDouble(),
        BadgeMetric.WATER_DAYS to days.count { it.targetWaterMl > 0 && it.waterMl >= it.targetWaterMl }.toDouble(),
        BadgeMetric.MACRO_DAYS to days.count { it.date.isBefore(today) && macrosInTarget(it) }.toDouble(),
    )

    /** 0..1 toward [def]'s threshold. */
    fun progress(def: BadgeDefinition, stats: Map<BadgeMetric, Double>): Float =
        ((stats[def.metric] ?: 0.0) / def.threshold).coerceIn(0.0, 1.0).toFloat()

    /** Longest run of consecutive calendar days in [dates]. */
    fun longestRun(dates: Collection<LocalDate>): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in dates.toSortedSet()) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }

    private fun macrosInTarget(d: DayNutrition): Boolean {
        fun near(value: Double, target: Int) = target > 0 && abs(value - target) <= target * MACRO_TOLERANCE
        return near(d.proteinG, d.targetProteinG) && near(d.carbsG, d.targetCarbsG) && near(d.fatG, d.targetFatG)
    }
}
