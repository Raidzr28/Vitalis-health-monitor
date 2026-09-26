package com.vitalis.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.ButtonKind
import com.vitalis.core.designsystem.component.InkCard
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.RingSpec
import com.vitalis.core.designsystem.component.RingStack
import com.vitalis.core.designsystem.component.SectionHeader
import com.vitalis.core.designsystem.component.TabBarClearance
import com.vitalis.core.designsystem.component.TrackBar
import com.vitalis.core.designsystem.component.VCard
import com.vitalis.core.designsystem.label
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.EnergyBudget
import com.vitalis.core.model.MealType
import java.util.Locale

data class MacroProgress(val label: String, val eatenG: Int, val targetG: Int, val color: Color)
data class MealSummary(val type: MealType, val kcal: Int, val items: String)

data class TodayUiState(
    val name: String,
    val dateLabel: String,
    val streakDays: Int,
    val budget: EnergyBudget,
    val activeMinutes: Int,
    val activeTarget: Int,
    val steps: Int,
    val stepsTarget: Int,
    val stepsKm: Double,
    /** Normalised 0..1 per hour bucket, for the mini bar chart. */
    val stepsByHour: List<Float>,
    val macros: List<MacroProgress>,
    val waterMl: Int,
    val waterTargetMl: Int,
    val meals: List<MealSummary>,
    val level: Int,
    val tierLabel: String,
    val xp: Int,
    val xpForNext: Int,
) {
    companion object {
        val Sample = TodayUiState(
            name = "Rangga",
            dateLabel = "Jumat, 25 September",
            streakDays = 23,
            budget = EnergyBudget(targetKcal = 2300, consumedKcal = 1450, burnedNetKcal = 371, bmrKcal = 1839),
            activeMinutes = 42, activeTarget = 60,
            steps = 7842, stepsTarget = 10000, stepsKm = 6.1,
            stepsByHour = listOf(.09f, .22f, 1f, .48f, .3f, .4f, .65f, .26f, .43f, .17f, .17f),
            macros = listOf(
                MacroProgress("Protein", 96, 158, VitalisColors.Ink),
                MacroProgress("Karbo", 150, 259, VitalisColors.Orange),
                MacroProgress("Lemak", 52, 70, VitalisColors.Olive),
            ),
            waterMl = 1750, waterTargetMl = 2750,
            meals = listOf(
                MealSummary(MealType.BREAKFAST, 520, "Nasi uduk, telur balado"),
                MealSummary(MealType.LUNCH, 680, "Ayam bakar, nasi putih, lalapan"),
                MealSummary(MealType.SNACK, 250, "Pisang ambon, kopi susu"),
            ),
            level = 12, tierLabel = "Silver", xp = 2860, xpForNext = 4157,
        )
    }
}

private fun MealType.icon(): ImageVector = when (this) {
    MealType.BREAKFAST -> Icons.Rounded.WbTwilight
    MealType.LUNCH -> Icons.Rounded.WbSunny
    MealType.DINNER -> Icons.Rounded.Bedtime
    else -> Icons.Rounded.LocalCafe
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onAddWater: () -> Unit,
    onOpenDiary: () -> Unit,
    onAddFood: (MealType) -> Unit,
    onOpenProfile: () -> Unit,
) {
    val b = state.budget
    val budgetWithExercise = b.targetKcal + b.burnedNetKcal
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
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(state.dateLabel, style = VitalisType.Small, color = VitalisColors.InkMuted)
                Text("Halo, ${state.name}", style = VitalisType.DisplayM, color = VitalisColors.Ink)
            }
            Row(
                Modifier.height(40.dp).clip(CircleShape).background(VitalisColors.Card).border(1.dp, VitalisColors.Border, CircleShape).padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(18.dp), tint = VitalisColors.Orange)
                Text("${state.streakDays} hari", style = VitalisType.BodyStrong.copy(fontSize = VitalisType.Small.fontSize), color = VitalisColors.Ink)
            }
            Box(
                Modifier.padding(start = 8.dp).size(40.dp).clip(CircleShape).background(VitalisColors.Ink).clickable(onClick = onOpenProfile)
                    .semantics { contentDescription = "Profil" },
                contentAlignment = Alignment.Center,
            ) { Text(state.name.take(1), style = VitalisType.BodyStrong, color = VitalisColors.Lime) }
        }

        // Energy hero
        InkCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Sisa kalori hari ini", style = VitalisType.Small, color = VitalisColors.OnNightMuted)
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(Formatters.kcal(b.remainingKcal), style = VitalisType.Hero, color = if (b.isOverBudget) VitalisColors.HeartRate else Color.White)
                        Text("kkal", style = VitalisType.Body, color = VitalisColors.OnNightMuted, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Text(
                        "${Formatters.kcal(b.targetKcal)} target\n− ${Formatters.kcal(b.consumedKcal)} makan + ${Formatters.kcal(b.burnedNetKcal)} olahraga",
                        style = VitalisType.Mono.copy(fontSize = VitalisType.Mono.fontSize * 0.92f), color = VitalisColors.OnNightMuted,
                    )
                }
                RingStack(
                    rings = listOf(
                        RingSpec(b.consumedKcal / budgetWithExercise.toFloat(), VitalisColors.Orange),
                        RingSpec(state.activeMinutes / state.activeTarget.toFloat(), VitalisColors.Lime),
                        RingSpec(state.steps / state.stepsTarget.toFloat(), VitalisColors.StepsBlue),
                    ),
                    description = "Kalori ${b.consumedKcal} dari $budgetWithExercise, aktif ${state.activeMinutes} dari ${state.activeTarget} menit, langkah ${state.steps} dari ${state.stepsTarget}",
                )
            }
            HorizontalDivider(color = VitalisColors.NightTrack)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RingLegend("Kalori", Formatters.kcal(b.consumedKcal), " / ${Formatters.kcal(budgetWithExercise)}", VitalisColors.Orange, Modifier.weight(1f))
                RingLegend("Aktif", "${state.activeMinutes}", " / ${state.activeTarget} mnt", VitalisColors.Lime, Modifier.weight(1f))
                RingLegend("Langkah", Formatters.kcal(state.steps), "", VitalisColors.StepsBlue, Modifier.weight(1f))
            }
        }

        // Level
        VCard(padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp), spacing = 10.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Level ${state.level} · ${state.tierLabel}", style = VitalisType.BodyStrong.copy(fontSize = VitalisType.Small.fontSize * 1.08f), color = VitalisColors.Ink)
                Text("${Formatters.kcal(state.xp)} / ${Formatters.kcal(state.xpForNext)} XP", style = VitalisType.Mono, color = VitalisColors.InkMuted)
            }
            TrackBar(state.xp / state.xpForNext.toFloat(), VitalisColors.Ink, height = 8.dp)
            Text("Catat makan berikutnya untuk +10 XP dan jaga streak ${state.streakDays} hari.", style = VitalisType.Small, color = VitalisColors.InkMuted)
        }

        // Macros
        VCard {
            SectionHeader("Makro", trailing = "Lihat detail", onTrailing = onOpenDiary)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                state.macros.forEach { m ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(m.label, style = VitalisType.Caption, color = VitalisColors.InkMuted)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("${m.eatenG}", style = VitalisType.BodyStrong, color = VitalisColors.Ink)
                            Text(" / ${m.targetG} g", style = VitalisType.Small, color = VitalisColors.InkMuted)
                        }
                        TrackBar(m.eatenG / m.targetG.toFloat(), m.color)
                    }
                }
            }
        }

        // Water + steps bento
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VCard(Modifier.weight(1f), spacing = 12.dp) {
                IconLabel(Icons.Rounded.WaterDrop, "Air minum", VitalisColors.Water, VitalisColors.InkMuted)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(String.format(Locale.forLanguageTag("id"), "%.2f", state.waterMl / 1000f), style = VitalisType.Metric, color = VitalisColors.Ink)
                    Text(String.format(Locale.forLanguageTag("id"), " / %.2f L", state.waterTargetMl / 1000f), style = VitalisType.Small, color = VitalisColors.InkMuted, modifier = Modifier.padding(bottom = 3.dp))
                }
                val glasses = state.waterTargetMl / 250
                Row(Modifier.fillMaxWidth().height(20.dp).semantics { contentDescription = "${state.waterMl / 250} dari $glasses gelas" }, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(glasses) { i ->
                        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(if (i < state.waterMl / 250) VitalisColors.Water else VitalisColors.WaterEmpty))
                    }
                }
                PillButton("250 ml", onAddWater, Modifier.fillMaxWidth(), kind = ButtonKind.Outline, icon = Icons.Rounded.Add, height = 44.dp)
            }
            VCard(Modifier.weight(1f), color = VitalisColors.Lime, spacing = 12.dp) {
                IconLabel(Icons.Rounded.DirectionsWalk, "Langkah", VitalisColors.OnLimeMuted, VitalisColors.OnLimeMuted)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(Formatters.kcal(state.steps), style = VitalisType.Metric, color = VitalisColors.Ink)
                    Text(
                        "dari ${Formatters.kcal(state.stepsTarget)} · ${String.format(Locale.forLanguageTag("id"), "%.1f", state.stepsKm)} km",
                        style = VitalisType.Small, color = VitalisColors.OnLimeMuted,
                    )
                }
                Row(Modifier.fillMaxWidth().height(46.dp).semantics { contentDescription = "Langkah per jam" }, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                    state.stepsByHour.forEach { f ->
                        Box(Modifier.weight(1f).fillMaxHeight(f.coerceAtLeast(0.08f)).clip(RoundedCornerShape(2.dp)).background(VitalisColors.Ink))
                    }
                }
            }
        }

        // Meals
        SectionHeader("Makan hari ini", Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp), trailing = "${Formatters.kcal(b.consumedKcal)} kkal")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.meals.forEach { meal ->
                MealRow(meal.type.icon(), meal.type.label(), meal.items, trailing = { Text("${meal.kcal}", style = VitalisType.BodyStrong, color = VitalisColors.Ink) }, onClick = onOpenDiary)
            }
            val logged = state.meals.map { it.type }.toSet()
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER).firstOrNull { it !in logged }?.let { next ->
                MealRow(
                    next.icon(), next.label(), "Masih ada ${Formatters.kcal(b.remainingKcal)} kkal",
                    dashed = true,
                    trailing = {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(VitalisColors.Lime), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Add, "Tambah ${next.label()}", Modifier.size(20.dp), tint = VitalisColors.Ink)
                        }
                    },
                    onClick = { onAddFood(next) },
                )
            }
        }

    }
}

@Composable
private fun RingLegend(label: String, value: String, suffix: String, color: Color, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(label, style = VitalisType.Caption, color = VitalisColors.OnNightMuted)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = VitalisType.BodyStrong, color = Color.White)
            Text(suffix, style = VitalisType.Caption, color = VitalisColors.OnNightMuted)
        }
    }
}

@Composable
private fun IconLabel(icon: ImageVector, label: String, tint: Color, textColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Text(label, style = VitalisType.Small, color = textColor)
    }
}

@Composable
private fun MealRow(icon: ImageVector, title: String, subtitle: String, trailing: @Composable () -> Unit, onClick: () -> Unit, dashed: Boolean = false) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (dashed) Modifier.border(1.5.dp, VitalisColors.BorderControl, shape) else Modifier.background(VitalisColors.Card))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(if (dashed) VitalisColors.Sunken else VitalisColors.Ground), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(22.dp), tint = VitalisColors.Ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = VitalisType.BodyStrong, color = VitalisColors.Ink)
            Text(subtitle, style = VitalisType.Small, color = VitalisColors.InkMuted)
        }
        trailing()
    }
}

@Preview(widthDp = 390, heightDp = 1360)
@Composable
private fun TodayPreview() = VitalisTheme {
    TodayScreen(TodayUiState.Sample, {}, {}, {}, {})
}

/** NavHost entry: Today backed by [TodayViewModel]. */
@Composable
fun TodayRoute(
    onOpenDiary: () -> Unit,
    onAddFood: (MealType) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    if (state == null) {
        Box(Modifier.fillMaxSize().background(VitalisColors.Ground))
        return
    }
    TodayScreen(state, onAddWater = viewModel::addWater, onOpenDiary = onOpenDiary, onAddFood = onAddFood, onOpenProfile = onOpenProfile)
}
