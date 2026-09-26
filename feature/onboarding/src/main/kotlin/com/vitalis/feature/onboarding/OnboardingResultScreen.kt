package com.vitalis.feature.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.component.InkCard
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.VCard
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.domain.energy.DailyTargetCalculator
import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.Goal
import com.vitalis.core.model.Sex
import com.vitalis.core.model.TargetWarning
import com.vitalis.core.model.UserProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Last onboarding step (spec §4.1): the numbers, where they came from, and the goal date. */
@Composable
fun OnboardingResultScreen(
    profile: UserProfile,
    weightKg: Double,
    onBack: () -> Unit,
    onStart: () -> Unit,
    saving: Boolean = false,
    saveError: String? = null,
    today: LocalDate = LocalDate.now(),
) {
    val targets = remember(profile, weightKg, today) { DailyTargetCalculator.calculate(profile, weightKg, today = today) }
    val id = Locale.forLanguageTag("id")
    val age = ChronoUnit.YEARS.between(profile.birthDate, today)
    val deficit = targets.tdeeKcal - targets.targetKcal
    val goalDate = targets.projectedGoalDate

    Column(
        Modifier
            .fillMaxSize()
            .background(VitalisColors.Ground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StepHeader(OnboardingStep.Result, onBack)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Rencana kamu siap, ${profile.displayName}.", style = VitalisType.DisplayL, color = VitalisColors.Ink)
            Text(
                "Dihitung dari profilmu: ${if (profile.sex == Sex.MALE) "pria" else "wanita"}, $age tahun, " +
                    "${Formatters.height(profile.heightCm)}, ${weightKg.toInt()} kg.",
                style = VitalisType.Body, color = VitalisColors.InkMuted,
            )
        }

        InkCard(spacing = 14.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Target kalori harian", style = VitalisType.Small, color = VitalisColors.OnNightMuted)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(Formatters.kcal(targets.targetKcal), style = VitalisType.Hero.copy(fontSize = VitalisType.Hero.fontSize * 0.93f), color = VitalisColors.Lime)
                    Text("kkal", style = VitalisType.Body, color = VitalisColors.OnNightMuted, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            Column {
                BreakdownRow("BMR", Formatters.kcal(targets.bmrKcal))
                BreakdownRow("TDEE · × ${String.format(id, "%.2f", profile.activityLevel.factor)}", Formatters.kcal(targets.tdeeKcal))
                if (profile.goal != Goal.MAINTAIN) {
                    val rate = String.format(id, "%.2f", profile.goalRateKgPerWeek).trimEnd('0').trimEnd(',')
                    BreakdownRow(
                        if (profile.goal == Goal.GAIN) "Surplus · $rate kg/minggu" else "Defisit · $rate kg/minggu",
                        Formatters.signed(-deficit),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroTile("Protein", "${targets.macros.proteinG} g", Modifier.weight(1f))
            MacroTile("Karbo", "${targets.macros.carbsG} g", Modifier.weight(1f))
            MacroTile("Lemak", "${targets.macros.fatG} g", Modifier.weight(1f))
        }

        if (goalDate != null && profile.targetWeightKg != null) {
            VCard(color = VitalisColors.Lime, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Perkiraan capai ${profile.targetWeightKg!!.toInt()} kg", style = VitalisType.Small, color = VitalisColors.OnLimeMuted)
                        Text(goalDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", id)), style = VitalisType.DisplayL.copy(fontSize = VitalisType.DisplayL.fontSize * 0.77f), color = VitalisColors.Ink)
                        Text(
                            "air ${String.format(id, "%.2f", targets.waterTargetMl / 1000f)} L · ${Formatters.kcal(targets.stepsTarget)} langkah",
                            style = VitalisType.Caption, color = VitalisColors.OnLimeMuted,
                        )
                    }
                    DescendingLine(Modifier.size(96.dp, 60.dp))
                }
            }
        }

        // Guardrails (spec §4.4.3): explain every adjustment; block only an unsafe target.
        targets.warnings.forEach { w ->
            Text(w.message(), style = VitalisType.Small, color = if (w.isBlocking) VitalisColors.Danger else VitalisColors.InkMuted)
        }
        saveError?.let { Text(it, style = VitalisType.Small, color = VitalisColors.Danger) }
        Spacer(Modifier.height(8.dp))
        val blocked = targets.warnings.any { it.isBlocking }
        PillButton(
            when { saving -> "Menyimpan…"; blocked -> "Ubah target dulu"; else -> "Mulai hari pertama" },
            if (blocked) onBack else if (saving) ({}) else onStart,
            Modifier.fillMaxWidth(),
        )
        Text("Target bisa kamu ubah kapan saja di Profil.", style = VitalisType.Caption, color = VitalisColors.InkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

private fun TargetWarning.message(): String = when (this) {
    TargetWarning.DEFICIT_EXCEEDS_25_PERCENT -> "Defisit dibatasi 25% dari TDEE supaya tetap aman."
    TargetWarning.BELOW_CALORIE_FLOOR -> "Target dinaikkan ke batas minimum kalori harian."
    TargetWarning.TARGET_BMI_UNDERWEIGHT -> "Berat target ada di bawah rentang BMI sehat. Pertimbangkan target yang lebih tinggi."
    TargetWarning.TARGET_BMI_UNSAFE -> "Berat target terlalu rendah untuk tinggi badanmu. Naikkan targetnya untuk lanjut."
    TargetWarning.MINOR_INFORMATIONAL_ONLY -> "Di bawah 18 tahun, VITALIS hanya memberi informasi, tanpa target defisit."
    TargetWarning.BMI_OVER_35_ESTIMATE_MAY_BE_HIGH -> "Untuk BMI di atas 35, perkiraan BMR bisa sedikit terlalu tinggi."
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    HorizontalDivider(color = VitalisColors.NightTrack)
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = VitalisType.Small, color = VitalisColors.OnNightMuted)
        Text(value, style = VitalisType.Mono.copy(fontSize = VitalisType.Small.fontSize), color = Color.White)
    }
}

@Composable
private fun MacroTile(label: String, value: String, modifier: Modifier) {
    VCard(modifier, radius = 18.dp, padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp), spacing = 2.dp) {
        Text(label, style = VitalisType.Caption, color = VitalisColors.InkMuted)
        Text(value, style = VitalisType.Value.copy(fontSize = VitalisType.Value.fontSize * 0.9f), color = VitalisColors.Ink)
    }
}

@Composable
private fun DescendingLine(modifier: Modifier) {
    Canvas(modifier) {
        val start = Offset(4.dp.toPx(), 8.dp.toPx())
        val end = Offset(size.width - 4.dp.toPx(), size.height - 8.dp.toPx())
        val path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(size.width * 0.3f, size.height * 0.25f, size.width * 0.5f, size.height * 0.5f, end.x, end.y)
        }
        drawPath(path, VitalisColors.Ink, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(VitalisColors.Ink, 4.dp.toPx(), start)
        drawCircle(VitalisColors.Lime, 5.dp.toPx(), end)
        drawCircle(VitalisColors.Ink, 5.dp.toPx(), end, style = Stroke(2.5.dp.toPx()))
    }
}

/** Rangga from the spec persona — used until onboarding input steps persist a real profile. */
val SampleProfile = UserProfile(
    id = UserProfile.DEFAULT_ID,
    displayName = "Rangga",
    sex = Sex.MALE,
    birthDate = LocalDate.of(1998, 3, 14),
    heightCm = 175f,
    activityLevel = ActivityLevel.MODERATELY_ACTIVE,
    goal = Goal.LOSE,
    goalRateKgPerWeek = 0.5f,
    targetWeightKg = 75f,
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingResultPreview() = VitalisTheme {
    OnboardingResultScreen(SampleProfile, 88.0, onBack = {}, onStart = {}, today = LocalDate.of(2026, 9, 25))
}
