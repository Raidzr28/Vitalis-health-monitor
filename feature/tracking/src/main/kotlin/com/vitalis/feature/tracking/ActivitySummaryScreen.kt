package com.vitalis.feature.tracking

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.ButtonKind
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.component.InkCard
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.Stat
import com.vitalis.core.designsystem.component.Tag
import com.vitalis.core.designsystem.component.VCard
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import java.util.Locale

data class SplitUi(val label: String, val paceSecPerKm: Int)
data class PrUi(val title: String, val detail: String, val xp: Int)

data class SummaryUiState(
    val title: String,
    val subtitle: String,
    val distanceM: Double,
    val movingSeconds: Long,
    val avgPaceSecPerKm: Double,
    val elevationGainM: Double,
    val elevationLossM: Double,
    val avgHeartRate: Int?,
    val cadenceSpm: Int?,
    val kcalGross: Int,
    val kcalNet: Int,
    val splits: List<SplitUi>,
    /** Normalised 0..1 altitude samples for the profile chart. */
    val elevationProfile: List<Float>,
    val route: List<Offset>,
    val newRecord: PrUi?,
    val xpBreakdown: List<Pair<String, Int>>,
) {
    companion object {
        val Sample = SummaryUiState(
            title = "Lari pagi", subtitle = "Jumat, 25 Sep · 06.12–06.47 · Senayan",
            distanceM = 5240.0, movingSeconds = 1938, avgPaceSecPerKm = 370.0,
            elevationGainM = 48.0, elevationLossM = 45.0, avgHeartRate = 148, cadenceSpm = 164,
            kcalGross = 412, kcalNet = 371,
            splits = listOf(SplitUi("1", 382), SplitUi("2", 368), SplitUi("3", 375), SplitUi("4", 362), SplitUi("5", 358), SplitUi("0,24", 351)),
            elevationProfile = listOf(.2f, .26f, .35f, .33f, .5f, .7f, .8f, .66f, .52f, .4f, .3f, .25f),
            route = LiveUiState.Sample.route,
            newRecord = PrUi("Rekor baru: 5K tercepat", "30:47 · 38 detik lebih cepat", 150),
            xpBreakdown = listOf("Aktivitas" to 50, "Jarak" to 52, "Rekor" to 150),
        )
    }
}

@Composable
fun ActivitySummaryScreen(state: SummaryUiState, onBack: () -> Unit, onShare: () -> Unit, onSave: () -> Unit, onDiscard: () -> Unit) {
    val id = Locale.forLanguageTag("id")
    Column(
        Modifier
            .fillMaxSize()
            .background(VitalisColors.Ground)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onBack)
            CircleIconButton(Icons.Rounded.IosShare, "Bagikan", onShare)
        }
        Column(Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.title, style = VitalisType.DisplayM, color = VitalisColors.Ink)
            Text(state.subtitle, style = VitalisType.Small, color = VitalisColors.InkMuted)
        }

        state.newRecord?.let { pr ->
            VCard(color = VitalisColors.Lime, radius = 22.dp, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(VitalisColors.Ink), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.EmojiEvents, null, Modifier.size(22.dp), tint = VitalisColors.Lime)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(pr.title, style = VitalisType.BodyStrong.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = VitalisColors.Ink)
                        Text(pr.detail, style = VitalisType.Small, color = VitalisColors.OnLimeMuted)
                    }
                    Text("+${pr.xp} XP", style = VitalisType.Mono.copy(fontSize = VitalisType.Small.fontSize), color = VitalisColors.Ink)
                }
            }
        }

        RouteMap(state.route, Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp)), dark = false)

        VCard(padding = PaddingValues(0.dp), spacing = 0.dp) {
            MetricRow(
                "Jarak" to "${Formatters.distanceValue(state.distanceM)} km",
                "Waktu gerak" to Formatters.duration(state.movingSeconds),
                "Pace rata-rata" to Formatters.pace(state.avgPaceSecPerKm),
            )
            HorizontalDivider(color = VitalisColors.Hairline)
            MetricRow(
                "Elevasi naik" to "+${Formatters.elevation(state.elevationGainM)}",
                "Detak rata" to (state.avgHeartRate?.toString() ?: "--"),
                "Kadens" to (state.cadenceSpm?.let { "$it spm" } ?: "--"),
            )
        }

        InkCard(spacing = 12.dp) {
            Row {
                Stat("Kalori kotor", "${state.kcalGross} kkal", Modifier.weight(1f), onDark = true, valueStyle = VitalisType.Value.copy(fontSize = VitalisType.Value.fontSize * 1.2f))
                Stat("Masuk ke budget", "+${state.kcalNet} kkal", Modifier.weight(1f), onDark = true, valueStyle = VitalisType.Value.copy(fontSize = VitalisType.Value.fontSize * 1.2f), valueColor = VitalisColors.Lime)
            }
            Text(
                "Bersih = kotor − BMR selama ${state.movingSeconds / 60} menit (${state.kcalGross - state.kcalNet} kkal), supaya kalori istirahat tidak dihitung dua kali.",
                style = VitalisType.Caption, color = VitalisColors.OnNightMuted,
            )
        }

        VCard(spacing = 10.dp) {
            Text("Split per km", style = VitalisType.Title, color = VitalisColors.Ink, modifier = Modifier.padding(bottom = 4.dp))
            val fastest = state.splits.minOf { it.paceSecPerKm }
            val slowest = state.splits.maxOf { it.paceSecPerKm }
            state.splits.forEach { s ->
                // Faster split → longer bar; the slowest still gets 55% so it stays readable.
                val span = (slowest - fastest).coerceAtLeast(1)
                val w = .55f + .45f * (slowest - s.paceSecPerKm) / span
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(s.label, style = VitalisType.Small, color = VitalisColors.InkMuted, modifier = Modifier.width(34.dp))
                    Box(Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth(w).height(22.dp).clip(RoundedCornerShape(6.dp)).background(if (s.paceSecPerKm == fastest) VitalisColors.Orange else VitalisColors.Ink))
                    }
                    Text(Formatters.pace(s.paceSecPerKm.toDouble()), style = VitalisType.Mono.copy(fontSize = VitalisType.Small.fontSize), color = VitalisColors.Ink, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
                }
            }
        }

        VCard(spacing = 10.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Profil elevasi", style = VitalisType.Title, color = VitalisColors.Ink)
                Text("naik ${Formatters.elevation(state.elevationGainM)} · turun ${Formatters.elevation(state.elevationLossM)}", style = VitalisType.Caption, color = VitalisColors.InkMuted)
            }
            ElevationChart(state.elevationProfile, Modifier.fillMaxWidth().height(80.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tag("+${state.xpBreakdown.sumOf { it.second }} XP total", background = VitalisColors.Card, color = VitalisColors.Ink, strong = true)
            state.xpBreakdown.forEach { (k, v) -> Tag("$k +$v") }
        }

        PillButton("Simpan aktivitas", onSave, Modifier.fillMaxWidth().padding(top = 8.dp))
        PillButton("Buang aktivitas", onDiscard, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost, height = 48.dp, textColor = VitalisColors.Danger)
    }
}

@Composable
private fun MetricRow(vararg cells: Pair<String, String>) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        cells.forEachIndexed { i, (label, value) ->
            if (i > 0) VerticalDivider(color = VitalisColors.Hairline)
            Stat(label, value, Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp))
        }
    }
}

@Composable
private fun ElevationChart(samples: List<Float>, modifier: Modifier) {
    Canvas(modifier.semantics { contentDescription = "Grafik profil elevasi" }) {
        if (samples.size < 2) return@Canvas
        val step = size.width / (samples.size - 1)
        val pts = samples.mapIndexed { i, v -> Offset(i * step, size.height * (1f - v * .85f)) }
        val line = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val mid = (pts[i - 1] + pts[i]) / 2f
                quadraticTo(pts[i - 1].x, pts[i - 1].y, mid.x, mid.y)
            }
            lineTo(pts.last().x, pts.last().y)
        }
        val area = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(area, VitalisColors.Sunken)
        drawPath(line, VitalisColors.Ink, style = Stroke(2.dp.toPx()))
        val peak = pts.minBy { it.y }
        drawCircle(VitalisColors.Orange, 4.dp.toPx(), peak)
    }
}

@Preview(widthDp = 390, heightDp = 1500)
@Composable
private fun SummaryPreview() = VitalisTheme {
    ActivitySummaryScreen(SummaryUiState.Sample, {}, {}, {}, {})
}
