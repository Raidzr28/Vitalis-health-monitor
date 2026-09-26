package com.vitalis.feature.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalis.core.common.format.Formatters
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
import java.time.LocalDate
import java.util.Locale

enum class ProgressRange(val label: String) { Week("Minggu"), Month("Bulan"), Year("Tahun") }

data class Pillar(val name: String, val score: Int)
data class WeightPoint(val date: LocalDate, val kg: Float)
data class RecordRow(val name: String, val detail: String, val value: String)

data class ProgressUiState(
    val range: ProgressRange,
    val healthScore: Int,
    val healthDelta: Int,
    val pillars: List<Pillar>,
    val tip: String,
    val weights: List<WeightPoint>,
    val targetWeightKg: Float,
    val bmi: Float,
    val waistToHeight: Float,
    val month: LocalDate,
    val today: LocalDate,
    /** Activity intensity 0..3 per day of [month], keyed by day-of-month; missing = no data. */
    val intensityByDay: Map<Int, Int>,
    val records: List<RecordRow>,
) {
    companion object {
        private val lv = listOf(2, 1, 3, 0, 2, 2, 1, 3, 2, 0, 1, 3, 2, 1, 2, 3, 2, 1, 0, 3, 2, 1, 2, 3, 3)
        val Sample = ProgressUiState(
            range = ProgressRange.Month, healthScore = 74, healthDelta = 4,
            pillars = listOf(Pillar("Komposisi tubuh", 62), Pillar("Kebugaran kardio", 78), Pillar("Aktivitas", 85), Pillar("Nutrisi", 76), Pillar("Pemulihan", 70)),
            tip = "Tidur 30 menit lebih awal bisa menaikkan pilar pemulihan.",
            weights = listOf(88.0f, 87.4f, 87.1f, 86.3f, 86.0f, 85.4f, 85.1f, 84.6f).mapIndexed { i, kg -> WeightPoint(LocalDate.of(2026, 8, 1).plusWeeks(i.toLong()), kg) },
            targetWeightKg = 75f, bmi = 27.6f, waistToHeight = 0.53f,
            month = LocalDate.of(2026, 9, 1), today = LocalDate.of(2026, 9, 25),
            intensityByDay = lv.mapIndexed { i, v -> (i + 1) to v }.toMap(),
            records = listOf(
                RecordRow("5K tercepat", "Hari ini", "30:47"),
                RecordRow("10K tercepat", "7 Sep", "1:05:12"),
                RecordRow("Jarak terjauh", "14 Sep · lari", "12,4 km"),
                RecordRow("Elevasi terbanyak", "30 Agu · hiking", "620 m"),
            ),
        )
    }
}

@Composable
fun ProgressScreen(state: ProgressUiState, onRangeChange: (ProgressRange) -> Unit) {
    val id = Locale.forLanguageTag("id")
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Progress", style = VitalisType.DisplayM, color = VitalisColors.Ink, modifier = Modifier.weight(1f))
            Row(Modifier.clip(CircleShape).background(VitalisColors.Sunken).padding(4.dp)) {
                ProgressRange.entries.forEach { r ->
                    val on = r == state.range
                    Text(
                        r.label, style = VitalisType.Small.copy(fontWeight = FontWeight.SemiBold), color = VitalisColors.Ink,
                        modifier = Modifier.clip(CircleShape).background(if (on) VitalisColors.Card else Color.Transparent)
                            .clickable(role = Role.Tab) { onRangeChange(r) }.semantics { selected = on }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                    )
                }
            }
        }

        // Health score (spec §4.4.2): trend first, never a verdict.
        InkCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${state.healthScore}", style = VitalisType.Hero.copy(fontSize = VitalisType.Hero.fontSize * 1.27f), color = Color.White)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Health Score", style = VitalisType.Title, color = Color.White)
                    Tag("${Formatters.signed(state.healthDelta)} dari minggu lalu", background = VitalisColors.Lime, color = VitalisColors.Ink, strong = true)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.pillars.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(p.name, style = VitalisType.Small, color = Color(0xFFC9CACE), modifier = Modifier.width(118.dp))
                        TrackBar(p.score / 100f, VitalisColors.Lime, Modifier.weight(1f), track = VitalisColors.NightTrack)
                        Text("${p.score}", style = VitalisType.Mono.copy(fontSize = VitalisType.Small.fontSize), color = Color.White, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                    }
                }
            }
            HorizontalDivider(color = VitalisColors.NightTrack)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(state.tip, style = VitalisType.Small, color = Color.White)
                Text("Ringkasan tren, bukan alat diagnosis medis.", style = VitalisType.Caption.copy(fontSize = VitalisType.Caption.fontSize * 0.92f), color = VitalisColors.OnNightMuted)
            }
        }

        // Weight
        val first = state.weights.first()
        val last = state.weights.last()
        VCard(spacing = 12.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Berat badan", style = VitalisType.Small, color = VitalisColors.InkMuted)
                    Text(Formatters.weight(last.kg), style = VitalisType.DisplayL, color = VitalisColors.Ink)
                }
                Text(
                    "${String.format(id, "%+.1f", last.kg - first.kg).replace('-', '−')} kg sejak ${first.date.dayOfMonth} ${first.date.month.getDisplayName(java.time.format.TextStyle.SHORT, id)}",
                    style = VitalisType.Small.copy(fontWeight = FontWeight.SemiBold),
                    color = if (last.kg <= first.kg) VitalisColors.Success else VitalisColors.InkMuted,
                )
            }
            WeightChart(state.weights.map { it.kg }, Modifier.fillMaxWidth().height(130.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf(first.date, state.weights[state.weights.size / 2].date, last.date).forEach {
                    Text("${it.dayOfMonth} ${it.month.getDisplayName(java.time.format.TextStyle.SHORT, id)}", style = VitalisType.Caption.copy(fontSize = VitalisType.Caption.fontSize * 0.92f), color = VitalisColors.InkMuted)
                }
            }
            HorizontalDivider(color = VitalisColors.Hairline)
            Row {
                Stat("BMI", String.format(id, "%.1f", state.bmi), Modifier.weight(1f), valueStyle = VitalisType.BodyStrong)
                Stat("Pinggang/tinggi", String.format(id, "%.2f", state.waistToHeight), Modifier.weight(1f), valueStyle = VitalisType.BodyStrong)
                Stat("Target", Formatters.weight(state.targetWeightKg), Modifier.weight(1f), valueStyle = VitalisType.BodyStrong)
            }
        }

        // Consistency heatmap (spec §9.7)
        VCard(spacing = 12.dp) {
            val activeDays = state.intensityByDay.values.count { it > 0 }
            SectionHeader("Konsistensi", trailing = "${state.month.month.getDisplayName(java.time.format.TextStyle.FULL, id).replaceFirstChar { it.uppercase() }} · $activeDays hari aktif")
            Heatmap(state)
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ringan", style = VitalisType.Caption, color = VitalisColors.InkMuted)
                VitalisColors.Heat.drop(1).forEach { Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(it)) }
                Text("Berat", style = VitalisType.Caption, color = VitalisColors.InkMuted)
            }
        }

        VCard(padding = PaddingValues(vertical = 6.dp), spacing = 0.dp) {
            Text("Rekor pribadi", style = VitalisType.Title, color = VitalisColors.Ink, modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 6.dp))
            state.records.forEach { r ->
                HorizontalDivider(color = VitalisColors.Hairline)
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(r.name, style = VitalisType.Body.copy(fontSize = VitalisType.Small.fontSize * 1.08f), color = VitalisColors.Ink)
                        Text(r.detail, style = VitalisType.Caption, color = VitalisColors.InkMuted)
                    }
                    Text(r.value, style = VitalisType.Mono.copy(fontSize = VitalisType.BodyStrong.fontSize, fontWeight = FontWeight.SemiBold), color = VitalisColors.Ink)
                }
            }
        }
    }
}

@Composable
private fun WeightChart(kgs: List<Float>, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Grafik berat badan dari ${kgs.first()} ke ${kgs.last()} kg" }) {
        val hi = kgs.max() + 1f
        val lo = kgs.min() - 1f
        listOf(.18f, .55f, .92f).forEach { f -> drawLine(VitalisColors.Hairline, Offset(0f, size.height * f), Offset(size.width, size.height * f), 1.dp.toPx()) }
        val step = size.width / (kgs.size - 1).coerceAtLeast(1)
        val pts = kgs.mapIndexed { i, kg -> Offset(i * step, size.height * (hi - kg) / (hi - lo)) }
        val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
        drawPath(path, VitalisColors.Ink, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(VitalisColors.Lime, 5.dp.toPx(), pts.last())
        drawCircle(VitalisColors.Ink, 5.dp.toPx(), pts.last(), style = Stroke(2.5.dp.toPx()))
    }
}

@Composable
private fun Heatmap(state: ProgressUiState) {
    val firstDow = state.month.withDayOfMonth(1).dayOfWeek.value - 1 // Monday = 0
    val days = state.month.lengthOfMonth()
    val cells: List<Int?> = List(firstDow) { null } + (1..days).toList()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("S", "S", "R", "K", "J", "S", "M").forEach { Text(it, style = VitalisType.Caption, color = VitalisColors.InkMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    val shape = RoundedCornerShape(9.dp)
                    val level = day?.let { state.intensityByDay[it] }
                    val future = day != null && state.month.withDayOfMonth(day).isAfter(state.today)
                    val isToday = day != null && state.month.withDayOfMonth(day) == state.today
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(shape)
                            .background(if (level != null) VitalisColors.Heat[level] else Color.Transparent)
                            .then(
                                when {
                                    isToday -> Modifier.border(2.dp, VitalisColors.Orange, shape)
                                    future -> Modifier.border(1.dp, VitalisColors.BorderControl, shape)
                                    else -> Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) Text("$day", style = VitalisType.Caption.copy(fontSize = VitalisType.Caption.fontSize * 0.92f), color = if (level == 3) Color.White else if (future) VitalisColors.InkFaint else VitalisColors.Ink)
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 1520)
@Composable
private fun ProgressPreview() = VitalisTheme {
    ProgressScreen(ProgressUiState.Sample) {}
}
