package com.vitalis.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.domain.gamification.BadgeDefinition
import com.vitalis.core.model.AchievementCategory
import com.vitalis.core.model.BadgeMetric
import com.vitalis.core.model.Tier
import com.vitalis.data.BadgeProgress
import java.util.Locale
import kotlin.math.floor

internal fun Tier.color(): Color = when (this) {
    Tier.BRONZE -> VitalisColors.TierBronze
    Tier.SILVER -> VitalisColors.TierSilver
    Tier.GOLD -> VitalisColors.TierGold
    Tier.PLATINUM -> Color(0xFF7FB8C9)
}

internal fun Tier.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

internal fun BadgeDefinition.icon(): ImageVector = when (metric) {
    BadgeMetric.DISTANCE_KM -> Icons.Rounded.Route
    BadgeMetric.ELEVATION_M -> Icons.Rounded.Terrain
    BadgeMetric.LONGEST_STREAK_DAYS -> Icons.Rounded.LocalFireDepartment
    BadgeMetric.MACRO_DAYS -> Icons.Rounded.PieChart
    BadgeMetric.PROTEIN_DAYS -> Icons.Rounded.FitnessCenter
    BadgeMetric.WATER_DAYS -> Icons.Rounded.WaterDrop
    BadgeMetric.NIGHT_ACTIVITIES -> Icons.Rounded.DarkMode
    BadgeMetric.SUNRISE_ACTIVITIES -> Icons.Rounded.WbTwilight
    BadgeMetric.ACTIVE_DAY_RUN -> Icons.Rounded.Umbrella
    BadgeMetric.MAX_ALTITUDE_M -> Icons.Rounded.Landscape
}

private fun n(v: Double) = Formatters.kcal(v.toInt())

internal fun BadgeDefinition.description(): String = when (metric) {
    BadgeMetric.DISTANCE_KM -> "Tempuh total ${n(threshold)} km"
    BadgeMetric.ELEVATION_M -> "Naik total ${n(threshold)} m"
    BadgeMetric.LONGEST_STREAK_DAYS -> "Streak ${n(threshold)} hari"
    BadgeMetric.MACRO_DAYS -> "Protein, karbo, dan lemak dalam target selama ${n(threshold)} hari"
    BadgeMetric.PROTEIN_DAYS -> "Capai target protein ${n(threshold)} hari"
    BadgeMetric.WATER_DAYS -> "Capai target air minum ${n(threshold)} hari"
    BadgeMetric.NIGHT_ACTIVITIES -> "Aktivitas antara 00.00 dan 04.00"
    BadgeMetric.SUNRISE_ACTIVITIES -> "${n(threshold)} aktivitas mulai sebelum jam 6 pagi"
    BadgeMetric.ACTIVE_DAY_RUN -> "Aktivitas ${n(threshold)} hari berturut-turut"
    BadgeMetric.MAX_ALTITUDE_M -> "Capai titik di atas ${n(threshold)} mdpl"
}

/** "5,2 / 10 km", "412 / 1.000 m", "3 / 7 hari". Values round down so a bar never reads as done early. */
internal fun BadgeProgress.progressLabel(): String {
    val unit = when (badge.metric) {
        BadgeMetric.DISTANCE_KM -> "km"
        BadgeMetric.ELEVATION_M, BadgeMetric.MAX_ALTITUDE_M -> "m"
        BadgeMetric.NIGHT_ACTIVITIES, BadgeMetric.SUNRISE_ACTIVITIES -> "aktivitas"
        else -> "hari"
    }
    val current = if (badge.metric == BadgeMetric.DISTANCE_KM && badge.threshold < 100) {
        // Same locale as Formatters (the phone's), so "5,2" and "1.000" agree on one screen.
        String.format(Locale.getDefault(), "%.1f", floor(value * 10) / 10)
    } else {
        n(floor(value))
    }
    return "$current / ${n(badge.threshold)} $unit"
}

internal fun AchievementCategory.label(): String = when (this) {
    AchievementCategory.DISTANCE -> "Jarak"
    AchievementCategory.ELEVATION -> "Elevasi"
    AchievementCategory.CONSISTENCY -> "Konsistensi"
    AchievementCategory.NUTRITION -> "Nutrisi"
    AchievementCategory.HIDDEN -> "Rahasia"
}

/** Round badge: ink disc and tier ring when earned, a quiet outline when not. A hidden badge shows a lock until earned. */
@Composable
internal fun BadgeMedallion(badge: BadgeDefinition, unlocked: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val icon = if (!unlocked && badge.hidden) Icons.Rounded.Lock else badge.icon()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (unlocked) VitalisColors.Ink else VitalisColors.Ground)
            .border(size * .05f, if (unlocked) badge.tier.color() else VitalisColors.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(size * .4f), tint = if (unlocked) VitalisColors.Lime else VitalisColors.InkFaint) }
}

/** Profile shows 8: newest unlocks first, then the visible badges closest to unlocking. */
fun profileBadges(all: List<BadgeProgress>, limit: Int = 8): List<BadgeProgress> =
    (all.filter { it.unlocked }.sortedByDescending { it.unlockedAt } + all.filter { !it.unlocked && !it.badge.hidden }.sortedByDescending { it.progress })
        .take(limit)
