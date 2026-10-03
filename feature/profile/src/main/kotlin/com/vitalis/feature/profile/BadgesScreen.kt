package com.vitalis.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.component.TrackBar
import com.vitalis.core.designsystem.component.VCard
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.domain.gamification.BadgeCatalog
import com.vitalis.core.model.AchievementCategory
import com.vitalis.data.BadgeProgress
import com.vitalis.data.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private val Id = Locale.forLanguageTag("id")

@HiltViewModel
class BadgesViewModel @Inject constructor(private val gamification: GamificationRepository) : ViewModel() {
    val badges: StateFlow<List<BadgeProgress>?> = gamification.badges.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    suspend fun refresh() = gamification.refreshBadges()
}

@Composable
fun BadgesRoute(onBack: () -> Unit, vm: BadgesViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.refresh() }
    val badges by vm.badges.collectAsStateWithLifecycle()
    val b = badges ?: return Box(Modifier.fillMaxSize().background(VitalisColors.Ground))
    BadgesScreen(b, onBack)
}

@Composable
fun BadgesScreen(badges: List<BadgeProgress>, onBack: () -> Unit) {
    val earned = badges.count { it.unlocked }
    val sections = AchievementCategory.entries.map { cat -> cat to badges.filter { it.badge.category == cat } }.filter { it.second.isNotEmpty() }
    LazyColumn(
        Modifier.fillMaxSize().background(VitalisColors.Ground).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onBack)
                Text("Badge", style = VitalisType.DisplayM.copy(fontSize = 24.sp), color = VitalisColors.Ink, modifier = Modifier.semantics { heading() })
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(VitalisColors.Ink).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$earned", style = VitalisType.DisplayL, color = VitalisColors.Lime)
                    Text("dari ${badges.size} badge terbuka", style = VitalisType.Body, color = VitalisColors.OnNightMuted, modifier = Modifier.padding(bottom = 4.dp))
                }
                TrackBar(earned / badges.size.coerceAtLeast(1).toFloat(), VitalisColors.Lime, track = VitalisColors.NightTrack, height = 8.dp)
            }
        }
        items(sections, key = { it.first }) { (category, list) ->
            VCard(padding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 2.dp), spacing = 0.dp) {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category.label(), style = VitalisType.Title, color = VitalisColors.Ink, modifier = Modifier.semantics { heading() })
                    Text("${list.count { it.unlocked }} / ${list.size}", style = VitalisType.Small, color = VitalisColors.InkMuted)
                }
                list.forEach { b ->
                    HorizontalDivider(color = VitalisColors.Hairline)
                    BadgeRow(b)
                }
            }
        }
        item { Box(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun BadgeRow(b: BadgeProgress) {
    val secret = b.badge.hidden && !b.unlocked
    val title = if (secret) "Badge rahasia" else b.badge.title
    val body = if (secret) "Terus bergerak untuk menemukannya." else b.badge.description()
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp).semantics(mergeDescendants = true) {
            contentDescription = "$title. $body. " + when {
                b.unlocked -> "Terbuka, ${b.badge.tier.label()}"
                secret -> "Terkunci"
                else -> "Terkunci, ${b.progressLabel()}"
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BadgeMedallion(b.badge, b.unlocked, 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = VitalisType.BodyStrong, color = if (secret) VitalisColors.InkMuted else VitalisColors.Ink)
            Text(body, style = VitalisType.Small, color = VitalisColors.InkMuted)
            when {
                b.unlocked -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(b.badge.tier.color()))
                    Text(
                        "${b.badge.tier.label()} · dibuka ${b.unlockedAt!!.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy", Id))}",
                        style = VitalisType.Caption.copy(fontWeight = FontWeight.SemiBold), color = VitalisColors.Ink,
                    )
                }
                !secret -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                    TrackBar(b.progress, VitalisColors.Ink, Modifier.weight(1f))
                    Text(b.progressLabel(), style = VitalisType.Caption, color = VitalisColors.InkMuted)
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 1400)
@Composable
private fun BadgesPreview() = VitalisTheme {
    BadgesScreen(
        BadgeCatalog.ALL.mapIndexed { i, b ->
            when {
                i % 4 == 0 -> BadgeProgress(b, 1f, Instant.parse("2026-09-14T06:00:00Z"))
                else -> BadgeProgress(b, (i % 5) / 5f, null)
            }
        },
    ) {}
}
