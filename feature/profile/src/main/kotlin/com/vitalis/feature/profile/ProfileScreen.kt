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
import androidx.compose.ui.text.font.FontWeight
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
import com.vitalis.core.domain.gamification.BadgeCatalog
import com.vitalis.data.BadgeProgress
import java.time.Instant

data class QuestUi(val name: String, val progressLabel: String, val fraction: Float, val xp: Int, val done: Boolean = false)

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
    val badges: List<BadgeProgress>,
) {
    companion object {
        val Sample = ProfileUiState(
            name = "Rangga", joined = "Bergabung Agustus 2026 · Jakarta",
            level = 12, xpIntoLevel = 2860, xpForLevel = 4157,
            streakDays = 23, freezes = 2, badgeCount = 5, badgeTotal = 21,
            questsEndLabel = "Berakhir Senin",
            quests = listOf(
                QuestUi("Tempuh 15 km", "8,4 / 15 km", .56f, 200),
                QuestUi("Catat makan 5 hari", "3 / 5 hari", .6f, 150),
                QuestUi("Naik total 300 m", "120 / 300 m", .4f, 180),
                QuestUi("Target protein 4 hari", "2 / 4 hari", .5f, 150),
            ),
            // Five earned, three in progress, straight from the real catalogue.
            badges = BadgeCatalog.ALL.filter { !it.hidden }.take(8).mapIndexed { i, b ->
                BadgeProgress(b, if (i < 5) 1f else .4f, if (i < 5) Instant.EPOCH else null)
            },
        )
    }
}


@Composable
fun ProfileScreen(state: ProfileUiState, onOpenSettings: () -> Unit, onOpenXpHistory: () -> Unit = {}, onOpenBadges: () -> Unit = {}) {
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
                        Text("+${q.xp} XP", style = VitalisType.Mono, color = if (q.done) VitalisColors.Success else VitalisColors.InkMuted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TrackBar(q.fraction, if (q.done) VitalisColors.Olive else VitalisColors.Ink, Modifier.weight(1f))
                        Text(
                            q.progressLabel, style = VitalisType.Caption.copy(fontWeight = if (q.done) FontWeight.Bold else FontWeight.Normal),
                            color = if (q.done) VitalisColors.Success else VitalisColors.InkMuted, textAlign = TextAlign.End, modifier = Modifier.widthIn(min = 72.dp),
                        )
                    }
                }
            }
            HorizontalDivider(color = VitalisColors.Hairline)
            Text(
                if (state.quests.isNotEmpty() && state.quests.all { it.done }) "Keempatnya selesai: bonus +500 XP sudah masuk. Quest baru hari Senin."
                else "Selesaikan keempatnya untuk bonus +500 XP.",
                style = VitalisType.Caption, color = VitalisColors.InkMuted,
            )
        }

        VCard {
            SectionHeader("Badge", trailing = "${state.badgeCount} dari ${state.badgeTotal} · Lihat semua", onTrailing = onOpenBadges)
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
private fun Badge(b: BadgeProgress, modifier: Modifier) {
    val status = if (b.unlocked) b.badge.tier.label() else b.progressLabel()
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = "${b.badge.title}, ${if (b.unlocked) status else "terkunci, $status"}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BadgeMedallion(b.badge, b.unlocked, 60.dp)
        Text(b.badge.title, style = VitalisType.Caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = VitalisColors.Ink, textAlign = TextAlign.Center)
        Text(status, style = VitalisType.Caption.copy(fontSize = VitalisType.Caption.fontSize * 0.92f), color = VitalisColors.InkMuted, textAlign = TextAlign.Center)
    }
}

@Preview(widthDp = 390, heightDp = 1180)
@Composable
private fun ProfilePreview() = VitalisTheme {
    ProfileScreen(ProfileUiState.Sample, onOpenSettings = {})
}
