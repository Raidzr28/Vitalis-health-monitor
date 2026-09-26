package com.vitalis.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalis.core.designsystem.component.Chip
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.component.OptionCard
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.VTextField
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.Goal
import com.vitalis.core.model.Sex
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Entry point used by the app's NavHost. */
@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step != OnboardingStep.Goal) { viewModel.back() }
    OnboardingFlow(
        state = state,
        onEdit = viewModel::edit,
        onNext = viewModel::next,
        onBack = { viewModel.back() },
        onFinish = { viewModel.finish(onFinished) },
    )
}

@Composable
fun OnboardingFlow(
    state: OnboardingUiState,
    onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    today: LocalDate = LocalDate.now(),
) {
    val errors = if (state.showErrors) state.draft.errors(state.step, today) else emptyMap()
    val d = state.draft
    when (state.step) {
        OnboardingStep.Goal -> StepScaffold(state.step, "Apa tujuanmu?", "Kami pakai ini untuk menghitung target kalori harian.", onBack, onNext) {
            OptionCard("Turunkan berat", "Defisit kalori yang aman", d.goal == Goal.LOSE, { onEdit { it.copy(goal = Goal.LOSE) } })
            OptionCard("Jaga berat", "Makan sesuai kebutuhan", d.goal == Goal.MAINTAIN, { onEdit { it.copy(goal = Goal.MAINTAIN) } })
            OptionCard("Naikkan berat", "Surplus untuk menambah massa", d.goal == Goal.GAIN, { onEdit { it.copy(goal = Goal.GAIN) } })
            if (d.goal != Goal.MAINTAIN) {
                Text("Kecepatan per minggu", style = VitalisType.Small, color = VitalisColors.InkMuted, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0.25f, 0.5f, 0.75f, 1f).forEach { r ->
                        Chip("${r.toString().replace('.', ',').removeSuffix(",0")} kg", d.rateKgPerWeek == r, onClick = { onEdit { it.copy(rateKgPerWeek = r) } })
                    }
                }
                VTextField(
                    "Target berat badan", d.targetWeight, { v -> onEdit { it.copy(targetWeight = v) } },
                    suffix = "kg", placeholder = "75", error = errors["target"], keyboardType = KeyboardType.Decimal,
                )
            }
        }

        OnboardingStep.Body -> StepScaffold(state.step, "Tentang tubuhmu", "Untuk menghitung BMR dengan rumus Mifflin-St Jeor.", onBack, onNext) {
            VTextField("Nama panggilan", d.name, { v -> onEdit { it.copy(name = v) } }, placeholder = "Rangga", error = errors["name"])
            Text("Jenis kelamin", style = VitalisType.Small, color = VitalisColors.InkMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionCard("Pria", null, d.sex == Sex.MALE, { onEdit { it.copy(sex = Sex.MALE) } }, Modifier.weight(1f))
                OptionCard("Wanita", null, d.sex == Sex.FEMALE, { onEdit { it.copy(sex = Sex.FEMALE) } }, Modifier.weight(1f))
            }
            errors["sex"]?.let { Text(it, style = VitalisType.Caption, color = VitalisColors.Danger) }
            BirthDateField(d.birthDate, errors["birth"], today) { date -> onEdit { it.copy(birthDate = date) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VTextField("Tinggi", d.height, { v -> onEdit { it.copy(height = v) } }, Modifier.weight(1f), suffix = "cm", placeholder = "170", error = errors["height"], keyboardType = KeyboardType.Decimal)
                VTextField("Berat sekarang", d.weight, { v -> onEdit { it.copy(weight = v) } }, Modifier.weight(1f), suffix = "kg", placeholder = "70", error = errors["weight"], keyboardType = KeyboardType.Decimal)
            }
        }

        OnboardingStep.Activity -> StepScaffold(state.step, "Seberapa aktif kamu?", "Di luar olahraga yang nanti kamu catat sendiri.", onBack, onNext) {
            ActivityLevel.entries.forEach { level ->
                val (title, sub) = level.labels()
                OptionCard(title, sub, d.activityLevel == level, { onEdit { it.copy(activityLevel = level) } })
            }
            errors["activity"]?.let { Text(it, style = VitalisType.Caption, color = VitalisColors.Danger) }
        }

        OnboardingStep.Result -> OnboardingResultScreen(
            profile = d.toProfile(),
            weightKg = requireNotNull(d.weightKg).toDouble(),
            onBack = onBack,
            onStart = onFinish,
            saving = state.saving,
            saveError = state.saveError,
            today = today,
        )
    }
}

private fun ActivityLevel.labels(): Pair<String, String> = when (this) {
    ActivityLevel.SEDENTARY -> "Jarang bergerak" to "Kerja duduk, hampir tidak olahraga"
    ActivityLevel.LIGHTLY_ACTIVE -> "Sedikit aktif" to "Olahraga ringan 1–3× per minggu"
    ActivityLevel.MODERATELY_ACTIVE -> "Aktif sedang" to "Olahraga 3–5× per minggu"
    ActivityLevel.VERY_ACTIVE -> "Sangat aktif" to "Olahraga berat 6–7× per minggu"
    ActivityLevel.EXTRA_ACTIVE -> "Atlet" to "Latihan harian atau kerja fisik berat"
}

@Composable
internal fun StepHeader(step: OnboardingStep, onBack: () -> Unit) {
    val total = OnboardingStep.entries.size
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onBack)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(Modifier.weight(1f).height(4.dp).background(if (i <= step.ordinal) VitalisColors.Ink else VitalisColors.BorderControl, CircleShape))
            }
        }
        Text("${step.ordinal + 1}/$total", style = VitalisType.Small, color = VitalisColors.InkMuted)
    }
}

@Composable
private fun StepScaffold(
    step: OnboardingStep,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(VitalisColors.Ground).statusBarsPadding().navigationBarsPadding().imePadding(),
    ) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StepHeader(step, onBack)
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = VitalisType.DisplayL, color = VitalisColors.Ink)
                Text(subtitle, style = VitalisType.Body, color = VitalisColors.InkMuted)
            }
            content()
        }
        PillButton("Lanjut", onNext, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDateField(value: LocalDate?, error: String?, today: LocalDate, onPick: (LocalDate) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Tanggal lahir", style = VitalisType.Small, color = VitalisColors.InkMuted)
        Box(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(VitalisColors.Card)
                .border(if (error != null) 1.5.dp else 1.dp, if (error != null) VitalisColors.Danger else VitalisColors.Border, shape)
                .clickable { open = true }.padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                value?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))) ?: "Pilih tanggal",
                style = if (value != null) VitalisType.BodyStrong else VitalisType.Body,
                color = if (value != null) VitalisColors.Ink else VitalisColors.InkFaint,
            )
        }
        error?.let { Text(it, style = VitalisType.Caption, color = VitalisColors.Danger) }
    }
    if (open) {
        val todayMillis = today.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = (value ?: today.minusYears(25)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            yearRange = (today.year - MAX_AGE)..today.year,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    open = false
                }) { Text("Pilih", color = VitalisColors.Ink) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Batal", color = VitalisColors.InkMuted) } },
        ) { DatePicker(picker) }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BodyStepPreview() = VitalisTheme {
    OnboardingFlow(OnboardingUiState(step = OnboardingStep.Body, draft = OnboardingDraft(name = "Rangga", sex = Sex.MALE)), {}, {}, {}, {})
}
