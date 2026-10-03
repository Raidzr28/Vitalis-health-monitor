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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.component.VCard
import com.vitalis.core.designsystem.label
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.MealType
import com.vitalis.core.model.QuestType
import com.vitalis.core.model.XpAction
import com.vitalis.data.GamificationRepository
import com.vitalis.data.UserRepository
import com.vitalis.data.XpHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class XpEventKind { MEAL, FULL_DAY, WATER, STEPS, ACTIVITY, RECORD, QUEST, OTHER }

/** One event (a meal slot, a session, a water day) with every award it paid, summed. */
data class XpEventUi(val kind: XpEventKind, val title: String, val detail: String, val xp: Int)
data class XpDayUi(val label: String, val events: List<XpEventUi>)
data class XpHistoryUi(val weekXp: Int, val totalXp: Long, val days: List<XpDayUi>)

private val Id = Locale.forLanguageTag("id")
private val Clock = DateTimeFormatter.ofPattern("HH.mm", Id)

/** Pure mapping from ledger rows (newest first) to the screen; grouped by event, then by day. */
fun xpHistory(entries: List<XpHistoryEntry>, totalXp: Long, weekXp: Int, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): XpHistoryUi {
    val events = entries.groupBy { it.referenceId.ifEmpty { it.awardedAt.toString() } }.values // keeps newest-first order
    val days = events.groupBy { it.first().awardedAt.atZone(zone).toLocalDate() }.map { (date, evs) ->
        XpDayUi(dayLabel(date, today), evs.map { toEvent(it, zone) })
    }
    return XpHistoryUi(weekXp, totalXp, days)
}

private fun dayLabel(date: LocalDate, today: LocalDate) = when (date) {
    today -> "Hari ini"
    today.minusDays(1) -> "Kemarin"
    else -> date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Id)).replaceFirstChar { it.uppercase() }
}

private fun toEvent(awards: List<XpHistoryEntry>, zone: ZoneId): XpEventUi {
    val first = awards.first()
    val ref = first.referenceId
    val xp = awards.sumOf { it.award.amount }
    val time = first.awardedAt.atZone(zone).format(Clock)
    val detail = if (awards.size > 1) awards.joinToString(" · ") { "${it.award.label()} ${it.award.amount}" } else time
    val kind: XpEventKind
    val title: String
    when {
        ref.startsWith("meal:") -> {
            kind = XpEventKind.MEAL
            val meal = MealType.entries.find { it.name == ref.substringAfterLast(':') }
            title = "${meal?.label() ?: "Makanan"} dicatat"
        }
        ref.startsWith("daylog:") -> { kind = XpEventKind.FULL_DAY; title = "Hari lengkap" }
        ref.startsWith("quest-chest:") -> { kind = XpEventKind.QUEST; title = "Semua quest minggu ini selesai" }
        ref.startsWith("quest:") -> {
            kind = XpEventKind.QUEST
            title = when (QuestType.entries.find { it.name == ref.substringAfterLast(':') }) {
                QuestType.DISTANCE_KM -> "Quest jarak selesai"
                QuestType.ELEVATION_M -> "Quest elevasi selesai"
                QuestType.ACTIVITY_COUNT -> "Quest aktivitas selesai"
                QuestType.LOG_DAYS -> "Quest catat makan selesai"
                QuestType.PROTEIN_DAYS -> "Quest protein selesai"
                else -> "Quest mingguan selesai"
            }
        }
        ref.startsWith("weight:") -> { kind = XpEventKind.OTHER; title = "Berat dicatat" }
        ref.startsWith("water:") -> { kind = XpEventKind.WATER; title = "Target air tercapai" }
        ref.startsWith("steps:") -> { kind = XpEventKind.STEPS; title = "Target langkah tercapai" }
        ref.startsWith("activity:") -> {
            kind = if (awards.any { it.award.action == XpAction.BREAK_PERSONAL_RECORD }) XpEventKind.RECORD else XpEventKind.ACTIVITY
            val distance = first.distanceMeters?.takeIf { it > 0 }?.let { " ${Formatters.distance(it)}" }.orEmpty()
            title = (first.sport?.label() ?: "Aktivitas") + distance
        }
        else -> { kind = XpEventKind.OTHER; title = first.award.label() }
    }
    return XpEventUi(kind, title, detail, xp)
}

@HiltViewModel
class XpHistoryViewModel @Inject constructor(gamification: GamificationRepository, users: UserRepository) : ViewModel() {
    private val today = LocalDate.now()
    private val weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant()

    val state: StateFlow<XpHistoryUi?> = combine(gamification.observeHistory(), users.gamification, gamification.observeXpSince(weekStart)) { entries, game, week ->
        xpHistory(entries, game.totalXp, week, today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun XpHistoryRoute(onBack: () -> Unit, vm: XpHistoryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return Box(Modifier.fillMaxSize().background(VitalisColors.Ground))
    XpHistoryScreen(s, onBack)
}

@Composable
fun XpHistoryScreen(state: XpHistoryUi, onBack: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(VitalisColors.Ground).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onBack)
                Text("Riwayat XP", style = VitalisType.DisplayM.copy(fontSize = 24.sp), color = VitalisColors.Ink, modifier = Modifier.semantics { heading() })
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(VitalisColors.Ink).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Minggu ini", style = VitalisType.Caption, color = VitalisColors.OnNightMuted)
                    Text("+${Formatters.kcal(state.weekXp)} XP", style = VitalisType.Value.copy(fontSize = 24.sp), color = VitalisColors.Lime)
                }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(VitalisColors.Card).padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text("Total", style = VitalisType.Caption, color = VitalisColors.InkMuted)
                    Text("${Formatters.kcal(state.totalXp.toInt())} XP", style = VitalisType.Value.copy(fontSize = 24.sp), color = VitalisColors.Ink)
                }
            }
        }
        if (state.days.isEmpty()) {
            item {
                VCard {
                    Text("Belum ada XP", style = VitalisType.Title, color = VitalisColors.Ink)
                    Text("Catat makanan pertamamu untuk +10 XP, atau rekam aktivitas untuk +50 XP.", style = VitalisType.Body, color = VitalisColors.InkMuted)
                }
            }
        }
        items(state.days, key = { it.label }) { day ->
            VCard(padding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 2.dp), spacing = 0.dp) {
                Text(day.label, style = VitalisType.BodyStrong, color = VitalisColors.Ink, modifier = Modifier.padding(bottom = 6.dp))
                day.events.forEach { e ->
                    HorizontalDivider(color = VitalisColors.Hairline)
                    EventRow(e)
                }
            }
        }
        item {
            Row(Modifier.padding(horizontal = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.VerifiedUser, null, Modifier.size(18.dp), tint = VitalisColors.InkMuted)
                Text(
                    "Setiap makanan, sesi, dan target hanya memberi XP sekali. Menghapus lalu mencatat ulang tidak menambah XP.",
                    style = VitalisType.Small, color = VitalisColors.InkMuted,
                )
            }
        }
        item { Box(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun EventRow(e: XpEventUi) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val record = e.kind == XpEventKind.RECORD
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(if (record) VitalisColors.Lime else VitalisColors.Sunken),
            contentAlignment = Alignment.Center,
        ) { Icon(e.kind.icon(), null, Modifier.size(20.dp), tint = VitalisColors.Ink) }
        Column(Modifier.weight(1f)) {
            Text(e.title, style = VitalisType.BodyStrong, color = VitalisColors.Ink)
            Text(e.detail, style = VitalisType.Small, color = VitalisColors.InkMuted)
        }
        Text("+${e.xp}", style = VitalisType.Mono.copy(fontSize = 14.sp), color = VitalisColors.Ink, textAlign = TextAlign.End)
    }
}

private fun XpEventKind.icon(): ImageVector = when (this) {
    XpEventKind.MEAL -> Icons.Rounded.Restaurant
    XpEventKind.FULL_DAY -> Icons.Rounded.TaskAlt
    XpEventKind.WATER -> Icons.Rounded.WaterDrop
    XpEventKind.STEPS -> Icons.Rounded.DirectionsWalk
    XpEventKind.ACTIVITY -> Icons.Rounded.DirectionsRun
    XpEventKind.RECORD -> Icons.Rounded.EmojiEvents
    XpEventKind.QUEST -> Icons.Rounded.Flag
    XpEventKind.OTHER -> Icons.Rounded.Star
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun XpHistoryPreview() = VitalisTheme {
    XpHistoryScreen(
        XpHistoryUi(
            weekXp = 642, totalXp = 6_228,
            days = listOf(
                XpDayUi("Hari ini", listOf(
                    XpEventUi(XpEventKind.RECORD, "Lari 5,24 km", "Aktivitas 50 · Jarak 50 · Rekor 150", 250),
                    XpEventUi(XpEventKind.WATER, "Target air tercapai", "14.20", 20),
                    XpEventUi(XpEventKind.MEAL, "Sarapan dicatat", "07.10", 10),
                )),
                XpDayUi("Kemarin", listOf(XpEventUi(XpEventKind.FULL_DAY, "Hari lengkap", "19.02", 30))),
            ),
        ),
    ) {}
}
