package com.vitalis.core.designsystem

import com.vitalis.core.model.MealType
import com.vitalis.core.model.SportType

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
