package com.vitalis.core.designsystem

import com.vitalis.core.model.MealType
import com.vitalis.core.model.SportType
import com.vitalis.core.model.XpAction
import com.vitalis.core.model.XpAward

// ponytail: Indonesian literals; move to strings.xml when a second language ships.

fun MealType.label(): String = when (this) {
    MealType.BREAKFAST -> "Sarapan"
    MealType.LUNCH -> "Makan siang"
    MealType.DINNER -> "Makan malam"
    MealType.SNACK -> "Snack"
    MealType.PRE_WORKOUT -> "Pra-latihan"
    MealType.POST_WORKOUT -> "Pasca-latihan"
}

fun SportType.label(): String = when (this) {
    SportType.RUNNING -> "Lari"
    SportType.TRAIL_RUNNING -> "Trail run"
    SportType.WALKING -> "Jalan"
    SportType.HIKING -> "Hiking"
    SportType.TREKKING -> "Trekking"
    SportType.MOUNTAIN_CLIMBING -> "Mendaki"
    SportType.CYCLING_ROAD -> "Sepeda"
    SportType.MOUNTAIN_BIKING -> "MTB"
    SportType.HORSE_RIDING -> "Berkuda"
    SportType.OPEN_WATER_SWIMMING -> "Renang"
    SportType.ROWING -> "Dayung"
    SportType.KAYAKING -> "Kayak"
    SportType.SKATEBOARDING -> "Skateboard"
    SportType.INLINE_SKATING -> "Inline skate"
    SportType.TREADMILL -> "Treadmill"
    SportType.GYM_WORKOUT -> "Gym"
}

fun XpAward.label(): String = when (action) {
    XpAction.LOG_MEAL -> "Makan dicatat"
    XpAction.COMPLETE_DAY_LOG -> "Hari lengkap"
    XpAction.LOG_WEIGHT -> "Berat dicatat"
    XpAction.HIT_WATER_TARGET -> "Target air"
    XpAction.HIT_STEP_TARGET -> "Target langkah"
    XpAction.COMPLETE_GPS_ACTIVITY -> "Aktivitas"
    XpAction.PER_KM_DISTANCE -> "Jarak"
    XpAction.PER_100M_ELEVATION -> "Elevasi"
    XpAction.BREAK_PERSONAL_RECORD -> "Rekor"
    XpAction.WITHIN_CALORIE_TARGET -> "Dalam target kalori"
    // Streak milestones reuse the quest action; the reason carries the length ("streak_7").
    XpAction.COMPLETE_WEEKLY_QUEST -> reason?.takeIf { it.startsWith("streak_") }?.let { "Streak ${it.removePrefix("streak_")} hari" } ?: "Quest mingguan"
}
