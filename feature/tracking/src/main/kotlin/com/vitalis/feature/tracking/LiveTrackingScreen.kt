package com.vitalis.feature.tracking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.label
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.SportType

data class LiveUiState(
    val sport: SportType,
    val distanceM: Double,
    val movingSeconds: Long,
    val currentPaceSecPerKm: Double?,
    val kcalGross: Int,
    val heartRate: Int?,
    val hrZone: Int?,
    val isPaused: Boolean,
    /** Route in normalised 0..1 coordinates, already projected — the map layer owns real projection. */
    val route: List<Offset>,
    /** Distance goal in km, or null when recording without one. */
    val targetKm: Double? = null,
) {
    companion object {
        val Sample = LiveUiState(
            sport = SportType.RUNNING, distanceM = 5240.0, movingSeconds = 1938, currentPaceSecPerKm = 358.0,
            kcalGross = 412, heartRate = 152, hrZone = 3, isPaused = false,
            route = listOf(Offset(.22f, .82f), Offset(.21f, .62f), Offset(.33f, .55f), Offset(.46f, .52f), Offset(.46f, .38f), Offset(.55f, .28f), Offset(.68f, .28f), Offset(.82f, .36f), Offset(.8f, .52f), Offset(.64f, .7f)),
        )
    }
}

/**
 * Fullscreen recording screen (spec §4.5.2). No blur here on purpose — GPS + map + blur
 * drains the battery (spec §10.6). The Stop button needs a long press to avoid pocket taps.
 */
@Composable
fun LiveTrackingScreen(state: LiveUiState, onTogglePause: () -> Unit, onLap: () -> Unit, onStop: () -> Unit, onLock: () -> Unit) {
    Box(Modifier.fillMaxSize().background(VitalisColors.NightDeep)) {
        RouteMap(state.route, Modifier.fillMaxWidth().height(360.dp))

        Row(Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(
                Modifier.height(40.dp).clip(CircleShape).background(VitalisColors.NightDeep).border(1.dp, VitalisColors.NightTrack, CircleShape).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.isPaused) VitalisColors.OnNightMuted else VitalisColors.Orange))
                Text("${state.sport.label()} · ${if (state.isPaused) "dijeda" else "merekam"}", style = VitalisType.BodyStrong.copy(fontSize = VitalisType.Small.fontSize), color = Color.White)
            }
            CircleIconButton(Icons.Rounded.Lock, "Kunci layar", onLock, size = 40.dp, background = VitalisColors.NightDeep, tint = Color.White, border = BorderStroke(1.dp, VitalisColors.NightTrack), iconSize = 18.dp)
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 320.dp)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(VitalisColors.NightDeep)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Jarak", style = VitalisType.Small, color = VitalisColors.OnNightMuted)
                Text(Formatters.distanceValue(state.distanceM), style = VitalisType.HeroXl, color = Color.White)
                val target = state.targetKm
                when {
                    target == null -> Text("kilometer", style = VitalisType.Small, color = VitalisColors.OnNightMuted)
                    state.distanceM >= target * 1000 -> Text("kilometer · target tercapai", style = VitalisType.Small.copy(fontWeight = FontWeight.SemiBold), color = VitalisColors.Lime)
                    else -> Text("kilometer · target ${VoiceCues.km(target)} km", style = VitalisType.Small, color = VitalisColors.OnNightMuted)
                }
            }

            Column {
                HorizontalDivider(color = VitalisColors.NightLine)
                Row(Modifier.height(IntrinsicSize.Min)) {
                    LiveMetric("Durasi", Formatters.duration(state.movingSeconds), null, Modifier.weight(1f))
                    VerticalDivider(color = VitalisColors.NightLine)
                    LiveMetric("Pace saat ini", Formatters.pace(state.currentPaceSecPerKm), Formatters.paceUnit(), Modifier.weight(1f).padding(start = 16.dp))
                }
                HorizontalDivider(color = VitalisColors.NightLine)
                Row(Modifier.height(IntrinsicSize.Min)) {
                    LiveMetric("Kalori", "${state.kcalGross}", "kkal", Modifier.weight(1f))
                    VerticalDivider(color = VitalisColors.NightLine)
                    LiveMetric(
                        "Detak" + (state.hrZone?.let { " · zona $it" } ?: ""),
                        state.heartRate?.toString() ?: "--", "bpm", Modifier.weight(1f).padding(start = 16.dp), VitalisColors.HeartRate,
                    )
                }
                HorizontalDivider(color = VitalisColors.NightLine)
            }

            Box(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.Top) {
                Control("Lap") { CircleIconButton(Icons.Rounded.Flag, "Lap", onLap, size = 64.dp, background = Color.Transparent, tint = Color.White, border = BorderStroke(1.5.dp, Color(0xFF3A3B42)), iconSize = 22.dp) }
                Control(if (state.isPaused) "Lanjut" else "Jeda") {
                    CircleIconButton(
                        if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        if (state.isPaused) "Lanjut" else "Jeda", onTogglePause,
                        size = 88.dp, background = VitalisColors.Lime, border = null, iconSize = 34.dp,
                    )
                }
                Control("Tahan · selesai") {
                    Box(
                        Modifier.size(64.dp).clip(CircleShape).background(VitalisColors.NightTrack)
                            .combinedClickable(onClick = {}, onLongClick = onStop)
                            .semantics { contentDescription = "Tahan untuk menyelesaikan aktivitas" },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Stop, null, Modifier.size(24.dp), tint = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun Control(label: String, button: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        button()
        Text(label, style = VitalisType.Caption, color = VitalisColors.OnNightMuted)
    }
}

@Composable
private fun LiveMetric(label: String, value: String, unit: String?, modifier: Modifier, valueColor: Color = Color.White) {
    Column(modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = VitalisType.Caption, color = VitalisColors.OnNightMuted)
        Text(
            buildAnnotatedString {
                append(value)
                if (unit != null) withStyle(SpanStyle(fontSize = VitalisType.Small.fontSize, color = VitalisColors.OnNightMuted)) { append(" $unit") }
            },
            style = VitalisType.Mono.copy(fontSize = VitalisType.Metric.fontSize * 0.88f, fontFeatureSettings = "tnum"),
            color = valueColor,
        )
    }
}

/**
 * ponytail: stylised street grid + route drawn on Canvas. Swap for maps-compose (already in the
 * version catalog) once the tracking service emits real LatLngs.
 */
@Composable
fun RouteMap(route: List<Offset>, modifier: Modifier, dark: Boolean = true) {
    val bg = if (dark) Color(0xFF17181C) else Color(0xFFE4E0D6)
    val street = if (dark) Color(0xFF23252A) else Color(0xFFF4F2EC)
    val park = if (dark) Color(0xFF1A251D) else Color(0xFFD2DDC0)
    val line = if (dark) VitalisColors.Lime else VitalisColors.Ink
    val head = if (dark) VitalisColors.Lime else VitalisColors.Orange
    Canvas(modifier.background(bg).semantics { contentDescription = "Peta rute" }) {
        drawPark(park)
        listOf(.18f, .55f, .88f).forEach { y -> drawLine(street, Offset(0f, size.height * y), Offset(size.width, size.height * (y + .05f)), 12.dp.toPx()) }
        listOf(.18f, .48f, .8f).forEach { x -> drawLine(street, Offset(size.width * x, 0f), Offset(size.width * (x + .04f), size.height), 12.dp.toPx()) }
        if (route.size < 2) return@Canvas
        val pts = route.map { Offset(it.x * size.width, it.y * size.height) }
        val path = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until pts.size) {
                val mid = (pts[i - 1] + pts[i]) / 2f
                quadraticTo(pts[i - 1].x, pts[i - 1].y, mid.x, mid.y)
            }
            lineTo(pts.last().x, pts.last().y)
        }
        drawPath(path, line, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(Color.White, 6.dp.toPx(), pts.first())
        drawCircle(bg, 6.dp.toPx(), pts.first(), style = Stroke(3.dp.toPx()))
        drawCircle(head.copy(alpha = .18f), 22.dp.toPx(), pts.last())
        drawCircle(head, 8.dp.toPx(), pts.last())
        drawCircle(if (dark) VitalisColors.NightDeep else VitalisColors.Ink, 8.dp.toPx(), pts.last(), style = Stroke(3.dp.toPx()))
    }
}

private fun DrawScope.drawPark(color: Color) {
    val p = Path().apply {
        moveTo(size.width * .6f, size.height * .45f)
        cubicTo(size.width * .7f, size.height * .38f, size.width * .85f, size.height * .42f, size.width, size.height * .35f)
        lineTo(size.width, size.height * .68f)
        cubicTo(size.width * .85f, size.height * .72f, size.width * .7f, size.height * .7f, size.width * .6f, size.height * .45f)
        close()
    }
    drawPath(p, color)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LivePreview() = VitalisTheme {
    LiveTrackingScreen(LiveUiState.Sample, {}, {}, {}, {})
}
