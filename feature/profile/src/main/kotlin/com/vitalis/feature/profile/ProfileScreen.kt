package com.vitalis.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.component.InkCard
import com.vitalis.core.designsystem.component.SectionHeader
import com.vitalis.core.designsystem.component.Stat
import com.vitalis.core.designsystem.component.TabBarClearance
import com.vitalis.core.designsystem.component.Tag
import com.vitalis.core.designsystem.component.TrackBar
import com.vitalis.core.designsystem.component.VCard
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.LevelTier
import com.vitalis.core.model.Tier

data class QuestUi(val name: String, val progressLabel: String, val fraction: Float, val xp: Int)

/** [tier] null = still locked; [lockedProgress] then says how far along it is. */
data class BadgeUi(val name: String, val icon: ImageVector, val tier: Tier?, val lockedProgress: String? = null)

data class ProfileUiState(
    val name: String,
    val joined: String,
    val level: Int,
    val xpIntoLevel: Int,
    val xpForLevel: Int,
    val streakDays: Int,
    val freezes: Int,
    val badgeCount: Int,
    val badgeTotal: Int,
    val questsEndLabel: String,
    val quests: List<QuestUi>,
    val badges: List<BadgeUi>,
) {
    companion object {
        val Sample = ProfileUiState(
            name = "Rangga", joined = "Bergabung Agustus 2026 · Jakarta",
            level = 12, xpIntoLevel = 2860, xpForLevel = 4157,
            streakDays = 23, freezes = 2, badgeCount = 14, badgeTotal = 40,
            questsEndLabel = "Berakhir Senin",
            quests = listOf(
                QuestUi("Tempuh 15 km", "8,4 / 15 km", .56f, 200),
                QuestUi("Catat makan 5 hari", "3 / 5 hari", .6f, 150),
                QuestUi("Naik total 300 m", "120 / 300 m", .4f, 180),
                QuestUi("Target protein 4 hari", "2 / 4 hari", .5f, 150),
            ),
            badges = listOf(
                BadgeUi("First Steps", Icons.Rounded.DirectionsWalk, Tier.GOLD),
                BadgeUi("Week One", Icons.Rounded.LocalFireDepartment, Tier.SILVER),
                BadgeUi("Road Warrior", Icons.Rounded.Route, Tier.BRONZE),
                BadgeUi("Hill Starter", Icons.Rounded.Terrain, Tier.BRONZE),
                BadgeUi("Macro Master", Icons.Rounded.PieChart, Tier.SILVER),
                BadgeUi("Protein Pro", Icons.Rounded.FitnessCenter, Tier.BRONZE),
                BadgeUi("Hydrated", Icons.Rounded.WaterDrop, null, "18 / 30 hari"),
                BadgeUi("Climber", Icons.Rounded.Landscape, null, "412 / 1.000 m"),
            ),
        )
    }
}

private fun Tier.color(): Color = when (this) {
    Tier.BRONZE -> VitalisColors.TierBronze
    Tier.SILVER -> VitalisColors.TierSilver
    Tier.GOLD -> VitalisColors.TierGold
    Tier.PLATINUM -> Color(0xFF7FB8C9)
}

private fun Tier.label(): String = name.lowercase().replaceFirstChar { it.uppercase() }

@Composable
fun ProfileScreen(state: ProfileUiState, onOpenSettings: () -> Unit, onOpenXpHistory: () -> Unit = {}) {
    val tier = LevelTier.forLevel(state.level)
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
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(VitalisColors.Ink), contentAlignment = Alignment.Center) {
                Text(state.name.take(1), style = VitalisType.DisplayM.copy(fontSize = VitalisType.DisplayM.fontSize * 0.87f), color = VitalisColors.Lime)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.name, style = VitalisType.DisplayM.copy(fontSize = VitalisType.DisplayM.fontSize * 0.87f), color = VitalisColors.Ink)
                Text(state.joined, style = VitalisType.Small, color = VitalisColors.InkMuted)
            }
            CircleIconButton(Icons.Rounded.Tune, "Pengaturan", onOpenSettings)
        }

        InkCard(spacing = 14.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Level ${state.level}", style = VitalisType.DisplayL.copy(fontSize = VitalisType.DisplayL.fontSize * 1.18f), color = Color.White)
                Tag(tier.name.lowercase().replaceFirstChar { it.uppercase() }, background = Color(0xFFC9CCD3), color = VitalisColors.Ink, strong = true)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TrackBar(state.xpIntoLevel / state.xpForLevel.toFloat(), VitalisColors.Lime, track = VitalisColors.NightTrack, height = 10.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${Formatters.kcal(state.xpIntoLevel)} / ${Formatters.kcal(state.xpForLevel)} XP", style = VitalisType.Mono, color = VitalisColors.OnNightMuted)
                    Text("${Formatters.kcal(state.xpForLevel - state.xpIntoLevel)} XP lagi ke level ${state.level + 1}", style = VitalisType.Caption, color = VitalisColors.OnNightMuted)
                }
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpenXpHistory),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Riwayat XP", style = VitalisType.BodyStrong, color = VitalisColors.Lime, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, Modifier.size(20.dp), tint = VitalisColors.Lime)
            }
            HorizontalDivider(color = VitalisColors.NightTrack)
            Row {
                Stat("hari streak", "${state.streakDays}", Modifier.weight(1f), onDark = true)
                Stat("streak freeze", "${state.freezes}", Modifier.weight(1f), onDark = true)
                Stat("badge", "${state.badgeCount}", Modifier.weight(1f), onDark = true)
            }
        }

        VCard {
            SectionHeader("Quest minggu ini", trailing = state.questsEndLabel)
            state.quests.forEach { q ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(q.name, style = VitalisType.Body.copy(fontSize = VitalisType.Small.fontSize * 1.08f), color = VitalisColors.Ink)
                        Text("+${q.xp} XP", style = VitalisType.Mono, color = VitalisColors.InkMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrackBar(q.fraction, VitalisColors.Ink, Modifier.weight(1f))
                        Text(q.progressLabel, style = VitalisType.Caption, color = VitalisColors.InkMuted, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 72.dp))
                    }
                }
            }
            HorizontalDivider(color = VitalisColors.Hairline)
            Text("Selesaikan keempatnya untuk bonus +500 XP dan badge mingguan.", style = VitalisType.Caption, color = VitalisColors.InkMuted)
        }

        VCard {
            SectionHeader("Badge", trailing = "${state.badgeCount} dari ${state.badgeTotal}")
            state.badges.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { b -> Badge(b, Modifier.weight(1f)) }
                    repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Badge(b: BadgeUi, modifier: Modifier) {
    val locked = b.tier == null
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = "${b.name}, ${b.tier?.label() ?: "terkunci, ${b.lockedProgress}"}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(60.dp).clip(CircleShape).background(if (locked) VitalisColors.Ground else VitalisColors.Ink)
                .border(3.dp, b.tier?.color() ?: VitalisColors.Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(b.icon, null, Modifier.size(24.dp), tint = if (locked) VitalisColors.InkFaint else VitalisColors.Lime) }
        Text(b.name, style = VitalisType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = VitalisColors.Ink, textAlign = TextAlign.Center)
        Text(b.tier?.label() ?: b.lockedProgress.orEmpty(), style = VitalisType.Caption.copy(fontSize = VitalisType.Caption.fontSize * 0.92f), color = VitalisColors.InkMuted, textAlign = TextAlign.Center)
    }
}

@Preview(widthDp = 390, heightDp = 1180)
@Composable
private fun ProfilePreview() = VitalisTheme {
    ProfileScreen(ProfileUiState.Sample, onOpenSettings = {})
}
