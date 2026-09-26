package com.vitalis.core.model

/**
 * Every enum the app reasons about. Deliberately free of Android types and of
 * display strings — UI labels are resolved from `strings.xml` in the UI layer so
 * the app stays localisable (spec Lampiran A).
 */

enum class Sex { MALE, FEMALE }

/** Static activity multipliers (spec §5.2). Prefer [TdeeMode.DYNAMIC] where step data exists. */
enum class ActivityLevel(val factor: Double) {
    SEDENTARY(1.200),
    LIGHTLY_ACTIVE(1.375),
    MODERATELY_ACTIVE(1.550),
    VERY_ACTIVE(1.725),
    EXTRA_ACTIVE(1.900),
}

/**
 * DYNAMIC recomputes expenditure from what actually happened today
 * (`BMR × 1.2 + NEAT + EAT`), which avoids double-counting logged workouts
 * that a static activity factor already assumes (spec §5.2).
 */
enum class TdeeMode { STATIC, DYNAMIC }

enum class Goal { LOSE, MAINTAIN, GAIN }

enum class UnitSystem { METRIC, IMPERIAL }

/** WHO cut-offs run high for Indonesian body composition; §5.7 asks for a toggle. */
enum class BmiStandard { WHO, ASIA_PACIFIC }

enum class BmiCategory { UNDERWEIGHT, NORMAL, OVERWEIGHT, OBESE_I, OBESE_II }

enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK, PRE_WORKOUT, POST_WORKOUT }

/** Layered food sources (spec §4.2.5). Always surfaced in the UI — crowdsourced data is not truth. */
enum class FoodSource { USDA, OPEN_FOOD_FACTS, LOCAL_ID, USER }

enum class ActivitySource { GPS, MANUAL, HEALTH_CONNECT, IMPORT }

enum class BmrFormula { MIFFLIN_ST_JEOR, KATCH_MCARDLE, SCHOFIELD }

/** How a session's calories were derived, best-first (spec §5.5). */
enum class CalorieModel { HEART_RATE, GPS_BIOMECHANIC, MET_STATIC }

enum class RecordType {
    FASTEST_1K,
    FASTEST_5K,
    FASTEST_10K,
    FASTEST_HALF_MARATHON,
    FASTEST_MARATHON,
    LONGEST_DISTANCE,
    LONGEST_DURATION,
    MOST_ELEVATION_GAIN,
    FASTEST_AVG_PACE,
    HIGHEST_ALTITUDE,
}

enum class AchievementCategory { DISTANCE, ELEVATION, CONSISTENCY, NUTRITION, HIDDEN }

enum class Tier { BRONZE, SILVER, GOLD, PLATINUM }

/** Level bands (spec §9.2). */
enum class LevelTier(val minLevel: Int) {
    BRONZE(1),
    SILVER(10),
    GOLD(25),
    PLATINUM(50),
    DIAMOND(75),
    LEGEND(100);

    companion object {
        fun forLevel(level: Int): LevelTier = entries.last { level >= it.minLevel }
    }
}

/**
 * Blur is expensive (spec §10.6). FULL is real backdrop blur, LIGHT is the
 * gradient + border "fake glass" fallback, OFF is opaque surfaces for
 * high-contrast accessibility mode.
 */
enum class VisualEffectMode { FULL, LIGHT, OFF }

enum class ThemeMode { SYSTEM, DARK, LIGHT }

/**
 * Sport catalogue (spec §4.5.1).
 *
 * @param usesGps outdoor sports draw distance from location; indoor ones use MET × duration
 * @param tracksElevation whether ascent/descent is meaningful and worth showing
 * @param tracksCadence whether step/pedal cadence is collected
 * @param baseMet fallback MET used before a speed-specific entry is matched
 */
enum class SportType(
    val usesGps: Boolean,
    val tracksElevation: Boolean,
    val tracksCadence: Boolean,
    val baseMet: Double,
) {
    RUNNING(usesGps = true, tracksElevation = true, tracksCadence = true, baseMet = 9.8),
    TRAIL_RUNNING(usesGps = true, tracksElevation = true, tracksCadence = true, baseMet = 10.5),
    WALKING(usesGps = true, tracksElevation = true, tracksCadence = true, baseMet = 3.5),
    HIKING(usesGps = true, tracksElevation = true, tracksCadence = true, baseMet = 6.0),
    TREKKING(usesGps = true, tracksElevation = true, tracksCadence = false, baseMet = 7.3),
    MOUNTAIN_CLIMBING(usesGps = true, tracksElevation = true, tracksCadence = false, baseMet = 8.0),
    CYCLING_ROAD(usesGps = true, tracksElevation = true, tracksCadence = false, baseMet = 8.0),
    MOUNTAIN_BIKING(usesGps = true, tracksElevation = true, tracksCadence = false, baseMet = 8.5),
    HORSE_RIDING(usesGps = true, tracksElevation = true, tracksCadence = false, baseMet = 5.8),
    OPEN_WATER_SWIMMING(usesGps = true, tracksElevation = false, tracksCadence = false, baseMet = 5.8),
    ROWING(usesGps = true, tracksElevation = false, tracksCadence = false, baseMet = 7.0),
    KAYAKING(usesGps = true, tracksElevation = false, tracksCadence = false, baseMet = 5.0),
    SKATEBOARDING(usesGps = true, tracksElevation = false, tracksCadence = false, baseMet = 5.0),
    INLINE_SKATING(usesGps = true, tracksElevation = false, tracksCadence = false, baseMet = 7.5),
    TREADMILL(usesGps = false, tracksElevation = false, tracksCadence = true, baseMet = 8.3),
    GYM_WORKOUT(usesGps = false, tracksElevation = false, tracksCadence = false, baseMet = 6.0);

    /** Which biomechanical model best describes this sport's energy cost (spec §5.5). */
    val isFootSport: Boolean
        get() = this in setOf(RUNNING, TRAIL_RUNNING, WALKING, HIKING, TREKKING, MOUNTAIN_CLIMBING, TREADMILL)

    /** Cycling reports speed; foot sports report pace. Drives which metric is the hero number. */
    val prefersSpeedOverPace: Boolean
        get() = this in setOf(CYCLING_ROAD, MOUNTAIN_BIKING, HORSE_RIDING, ROWING, KAYAKING, SKATEBOARDING, INLINE_SKATING)
}

/** Lifecycle of a recording session (spec §4.5.3). */
enum class TrackingStatus { IDLE, PREPARING, ACTIVE, PAUSED_MANUAL, PAUSED_AUTO, STOPPED }

/** Quality of the current GPS fix, shown on the pre-start screen. */
enum class GpsSignalQuality { NONE, POOR, FAIR, GOOD, EXCELLENT }
