package com.vitalis.feature.tracking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.Stat
import com.vitalis.core.designsystem.component.TabBarClearance
import com.vitalis.core.designsystem.component.VCard
import com.vitalis.core.designsystem.label
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.GpsSignalQuality
import com.vitalis.core.datastore.TrackingSettings
import com.vitalis.core.model.SportType

fun SportType.icon(): ImageVector = when (this) {
    SportType.RUNNING, SportType.TREADMILL -> Icons.Rounded.DirectionsRun
    SportType.TRAIL_RUNNING -> Icons.Rounded.Terrain
    SportType.WALKING -> Icons.Rounded.DirectionsWalk
    SportType.HIKING, SportType.TREKKING -> Icons.Rounded.Hiking
    SportType.MOUNTAIN_CLIMBING -> Icons.Rounded.Landscape
    SportType.CYCLING_ROAD, SportType.MOUNTAIN_BIKING -> Icons.Rounded.DirectionsBike
    SportType.HORSE_RIDING -> Icons.Rounded.Pets
    else -> Icons.Rounded.FitnessCenter
}

/** The eight tiles on the selector, in the order the design shows them (spec §4.5.1). */
val FeaturedSports = listOf(
    SportType.RUNNING, SportType.WALKING, SportType.CYCLING_ROAD, SportType.HIKING,
    SportType.TRAIL_RUNNING, SportType.MOUNTAIN_CLIMBING, SportType.HORSE_RIDING, SportType.GYM_WORKOUT,
)

@Composable
fun SportSelectScreen(
    selected: SportType,
    lastUsed: Map<SportType, String>,
    gps: GpsSignalQuality,
    gpsAccuracyM: Int?,
    onSelect: (SportType) -> Unit,
    onStart: () -> Unit,
    settings: TrackingSettings = TrackingSettings(),
    targetOptions: List<Double> = targetOptions(selected),
    autoPauseAvailable: Boolean = true,
    onTarget: (Double?) -> Unit = {},
    onVoiceCues: (Boolean) -> Unit = {},
    onAutoPause: (Boolean) -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(VitalisColors.Ground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = TabBarClearance),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mulai aktivitas", style = VitalisType.DisplayM, color = VitalisColors.Ink)
            GpsBadge(gps, gpsAccuracyM)
        }

        FeaturedSports.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { sport -> SportTile(sport, sport == selected, lastUsed[sport] ?: "—", { onSelect(sport) }, Modifier.weight(1f)) }
            }
        }

        VCard(radius = 22.dp, padding = PaddingValues(0.dp), spacing = 0.dp) {
            Row(Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                SettingCell(
                    "Target", settings.targetKm?.let { "${VoiceCues.km(it)} km" } ?: "Bebas",
                    listOf<Pair<String, () -> Unit>>("Bebas" to { onTarget(null) }) + targetOptions.map { km -> "${VoiceCues.km(km)} km" to { onTarget(km) } },
                    Modifier.weight(1f),
                )
                VerticalDivider(color = VitalisColors.Hairline)
                SettingCell(
                    "Suara", if (settings.voiceCues) "Tiap 1 km" else "Mati",
                    listOf("Tiap 1 km" to { onVoiceCues(true) }, "Mati" to { onVoiceCues(false) }),
                    Modifier.weight(1f),
                )
                VerticalDivider(color = VitalisColors.Hairline)
                // Climbing and open-water swimming never auto-pause: long legitimate stops (spec §8.4).
                SettingCell(
                    "Auto-pause", if (!autoPauseAvailable) "Tidak ada" else if (settings.autoPause) "Aktif" else "Mati",
                    listOf("Aktif" to { onAutoPause(true) }, "Mati" to { onAutoPause(false) }),
                    Modifier.weight(1f), enabled = autoPauseAvailable,
                )
            }
        }

        PillButton("Mulai ${selected.label().lowercase()}", onStart, Modifier.fillMaxWidth(), icon = Icons.Rounded.PlayArrow, height = 60.dp)
    }
}

/** Sensible distance goals per sport: race distances on foot, longer steps on a bike. */
fun targetOptions(sport: SportType): List<Double> = when {
    sport.isFootSport -> listOf(3.0, 5.0, 10.0, 21.1, 42.2)
    sport == SportType.CYCLING_ROAD || sport == SportType.MOUNTAIN_BIKING -> listOf(10.0, 20.0, 40.0, 80.0)
    else -> listOf(2.0, 5.0, 10.0)
}

/** One tappable cell of the settings card; the choice list drops down from it. */
@Composable
private fun SettingCell(label: String, value: String, options: List<Pair<String, () -> Unit>>, modifier: Modifier, enabled: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, role = Role.DropdownList, onClickLabel = "Ubah $label") { open = true }
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, style = VitalisType.Caption, color = VitalisColors.InkMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = VitalisType.BodyStrong, color = if (enabled) VitalisColors.Ink else VitalisColors.InkFaint)
                if (enabled) Icon(Icons.Rounded.ExpandMore, null, Modifier.size(18.dp), tint = VitalisColors.InkMuted)
            }
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = VitalisColors.Card) {
            options.forEach { (text, pick) ->
                DropdownMenuItem(
                    text = { Text(text, style = VitalisType.Body, fontWeight = if (text == value) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { open = false; pick() },
                )
            }
        }
    }
}

@Composable
private fun GpsBadge(gps: GpsSignalQuality, accuracyM: Int?) {
    val ready = gps >= GpsSignalQuality.GOOD
    Row(
        Modifier.clip(CircleShape).background(if (ready) VitalisColors.SuccessBg else VitalisColors.Sunken).padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (ready) Color(0xFF1F9D5B) else VitalisColors.Orange))
        Text(
            if (ready) "GPS siap" + (accuracyM?.let { " · akurasi $it m" } ?: "") else "Mencari sinyal GPS…",
            style = VitalisType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = if (ready) VitalisColors.Success else VitalisColors.InkMuted,
        )
    }
}

@Composable
private fun SportTile(sport: SportType, selected: Boolean, last: String, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .height(88.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) VitalisColors.Night else VitalisColors.Card)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(sport.icon(), null, Modifier.size(26.dp), tint = if (selected) VitalisColors.Lime else VitalisColors.Ink)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(sport.label(), style = VitalisType.BodyStrong, color = if (selected) Color.White else VitalisColors.Ink)
            Text(last, style = VitalisType.Caption.copy(fontSize = VitalisType.Caption.fontSize * 0.92f), color = if (selected) VitalisColors.OnNightMuted else VitalisColors.InkMuted)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SportSelectPreview() = VitalisTheme {
    SportSelectScreen(SportType.RUNNING, mapOf(SportType.RUNNING to "2 hari lalu", SportType.WALKING to "Kemarin"), GpsSignalQuality.EXCELLENT, 4, {}, {})
}
