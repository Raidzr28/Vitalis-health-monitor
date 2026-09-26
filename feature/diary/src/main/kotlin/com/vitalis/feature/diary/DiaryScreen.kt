package com.vitalis.feature.diary

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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.key
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.ButtonKind
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.Stat
import com.vitalis.core.designsystem.component.TabBarClearance
import com.vitalis.core.designsystem.component.VCard
import com.vitalis.core.designsystem.label
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.FoodSource
import com.vitalis.core.model.MealType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

data class DiaryEntry(val id: String, val name: String, val portion: String, val kcal: Int)
data class DiaryMeal(val type: MealType, val entries: List<DiaryEntry>) {
    val kcal: Int get() = entries.sumOf { it.kcal }
}

data class DiaryUiState(
    val selectedDate: LocalDate,
    val today: LocalDate,
    /** Days in the visible week that have at least one log. */
    val loggedDates: Set<LocalDate>,
    val consumedKcal: Int,
    val burnedNetKcal: Int,
    val remainingKcal: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val meals: List<DiaryMeal>,
    /** kcal logged per meal the day before, offered as a one-tap copy (spec US-12). */
    val yesterdayKcal: Map<MealType, Int>,
) {
    companion object {
        private val today = LocalDate.of(2026, 9, 25)
        val Sample = DiaryUiState(
            selectedDate = today, today = today,
            loggedDates = (21..25).map { LocalDate.of(2026, 9, it) }.toSet(),
            consumedKcal = 1450, burnedNetKcal = 371, remainingKcal = 1221,
            proteinG = 96, carbsG = 150, fatG = 52,
            meals = listOf(
                DiaryMeal(MealType.BREAKFAST, listOf(DiaryEntry("nasi-uduk", "Nasi uduk", "1 porsi · 200 g", 390), DiaryEntry("telur-balado", "Telur balado", "1 butir", 130))),
                DiaryMeal(MealType.LUNCH, listOf(DiaryEntry("ayam-bakar-paha", "Ayam bakar (paha)", "1 potong · 100 g", 290), DiaryEntry("nasi-putih", "Nasi putih", "1 piring · 200 g", 260), DiaryEntry("lalapan-dan-sambal", "Lalapan & sambal", "1 porsi", 130))),
                DiaryMeal(MealType.SNACK, listOf(DiaryEntry("pisang-ambon", "Pisang ambon", "1 buah sedang", 105), DiaryEntry("kopi-susu-gula-aren", "Kopi susu gula aren", "1 gelas · 250 ml", 145))),
                DiaryMeal(MealType.DINNER, emptyList()),
            ),
            yesterdayKcal = mapOf(MealType.DINNER to 610),
        )
    }
}

fun FoodSource.label(): String = when (this) {
    FoodSource.USDA -> "USDA"
    FoodSource.OPEN_FOOD_FACTS -> "Open Food Facts"
    FoodSource.LOCAL_ID -> "Lokal"
    FoodSource.USER -> "Buatan saya"
}

@Composable
fun DiaryScreen(
    state: DiaryUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
    onAddFood: (MealType) -> Unit,
    onCopyYesterday: (MealType) -> Unit,
    onDelete: (DiaryEntry) -> Unit,
    onSearch: () -> Unit,
    onScan: () -> Unit,
) {
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
            Text("Diary", style = VitalisType.DisplayM, color = VitalisColors.Ink, modifier = Modifier.weight(1f))
            CircleIconButton(Icons.Rounded.CalendarMonth, "Pilih tanggal", onPickDate)
        }

        // Search sits at the top, under the title — never near the floating tab bar.
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(CircleShape)
                .background(VitalisColors.Card)
                .border(1.dp, VitalisColors.Border, CircleShape)
                .clickable(onClick = onSearch)
                .padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = VitalisColors.InkMuted)
            Text("Cari makanan atau minuman", style = VitalisType.Body, color = VitalisColors.InkMuted, modifier = Modifier.weight(1f))
            CircleIconButton(Icons.Rounded.QrCodeScanner, "Scan barcode", onScan, background = VitalisColors.Lime, border = null)
        }

        WeekStrip(state, onSelectDate)

        VCard(padding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)) {
            Row {
                Stat("Dimakan", Formatters.kcal(state.consumedKcal), Modifier.weight(1f))
                Stat("Olahraga", Formatters.signed(state.burnedNetKcal), Modifier.weight(1f))
                Stat("Sisa", Formatters.kcal(state.remainingKcal), Modifier.weight(1f), valueColor = VitalisColors.OrangeText)
            }
            MacroSplit(state.proteinG, state.carbsG, state.fatG)
        }

        state.meals.forEach { meal ->
            if (meal.entries.isEmpty()) EmptyMeal(meal.type, state.yesterdayKcal[meal.type], onCopyYesterday, onAddFood)
            else MealCard(meal, onAddFood, onDelete)
        }
    }
}

@Composable
private fun WeekStrip(state: DiaryUiState, onSelectDate: (LocalDate) -> Unit) {
    val monday = state.selectedDate.minusDays(state.selectedDate.dayOfWeek.value - 1L)
    val id = Locale.forLanguageTag("id")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (0..6).map { monday.plusDays(it.toLong()) }.forEach { date ->
            val selected = date == state.selectedDate
            val future = date.isAfter(state.today)
            Column(
                Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) VitalisColors.Ink else if (future) Color.Transparent else VitalisColors.Card)
                    .clickable(enabled = !future, role = Role.Tab) { onSelectDate(date) }
                    .semantics { this.selected = selected },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
            ) {
                val fg = if (selected) Color.White else if (future) VitalisColors.InkFaint else VitalisColors.Ink
                Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, id).take(3), style = VitalisType.Caption, color = fg.copy(alpha = .75f))
                Text("${date.dayOfMonth}", style = VitalisType.BodyStrong.copy(fontSize = VitalisType.Title.fontSize * 1.06f), color = fg)
                Box(Modifier.size(5.dp).clip(CircleShape).background(
                    when {
                        date !in state.loggedDates -> Color.Transparent
                        selected -> VitalisColors.Lime
                        else -> VitalisColors.Olive
                    },
                ))
            }
        }
    }
}

@Composable
private fun MacroSplit(p: Int, c: Int, f: Int) {
    val kP = p * 4f; val kC = c * 4f; val kF = f * 9f
    val total = (kP + kC + kF).coerceAtLeast(1f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.weight(kP / total).fillMaxHeight().background(VitalisColors.Ink))
            Box(Modifier.weight(kC / total).fillMaxHeight().background(VitalisColors.Orange))
            Box(Modifier.weight(kF / total).fillMaxHeight().background(Color(0xFFC5E23A)))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("Protein" to p to kP, "Karbo" to c to kC, "Lemak" to f to kF).forEach { (lg, k) ->
                Text("${lg.first} ${lg.second} g · ${Formatters.percent(k / total)}", style = VitalisType.Caption, color = VitalisColors.InkMuted)
            }
        }
    }
}

@Composable
private fun MealCard(meal: DiaryMeal, onAddFood: (MealType) -> Unit, onDelete: (DiaryEntry) -> Unit) {
    VCard(radius = 22.dp, padding = PaddingValues(0.dp), spacing = 0.dp) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(meal.type.label(), style = VitalisType.Title, color = VitalisColors.Ink)
                Text("${Formatters.kcal(meal.kcal)} kkal", style = VitalisType.Small, color = VitalisColors.InkMuted)
            }
            CircleIconButton(Icons.Rounded.Add, "Tambah ke ${meal.type.label().lowercase()}", { onAddFood(meal.type) }, size = 40.dp, background = VitalisColors.Ground, border = null, iconSize = 18.dp)
        }
        meal.entries.forEach { e ->
            HorizontalDivider(color = VitalisColors.Hairline)
            key(e.id) { EntryRow(e, onDelete) }
        }
        Box(Modifier.height(4.dp))
    }
}

/** Swipe left to delete; Undo lives in the snackbar. TalkBack users get a "Hapus" custom action. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(e: DiaryEntry, onDelete: (DiaryEntry) -> Unit) {
    val dismiss = rememberSwipeToDismissBoxState(
        confirmValueChange = { v -> if (v == SwipeToDismissBoxValue.EndToStart) onDelete(e); v == SwipeToDismissBoxValue.EndToStart },
    )
    SwipeToDismissBox(
        state = dismiss,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                Modifier.fillMaxSize().background(VitalisColors.Danger).padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) { Icon(Icons.Rounded.Delete, null, Modifier.size(20.dp), tint = Color.White) }
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(VitalisColors.Card)
                .semantics { customActions = listOf(CustomAccessibilityAction("Hapus") { onDelete(e); true }) }
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(e.name, style = VitalisType.Body.copy(fontSize = VitalisType.Small.fontSize * 1.08f), color = VitalisColors.Ink)
                Text(e.portion, style = VitalisType.Caption, color = VitalisColors.InkMuted)
            }
            Text(Formatters.kcal(e.kcal), style = VitalisType.BodyStrong.copy(fontSize = VitalisType.Small.fontSize * 1.08f), color = VitalisColors.Ink)
        }
    }
}

@Composable
private fun EmptyMeal(type: MealType, yesterdayKcal: Int?, onCopyYesterday: (MealType) -> Unit, onAddFood: (MealType) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).border(1.5.dp, VitalisColors.BorderControl, shape).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(type.label(), style = VitalisType.Title, color = VitalisColors.Ink)
            Text(
                if (yesterdayKcal != null) "Belum ada catatan. Ulangi menu kemarin dengan satu tap." else "Belum ada catatan.",
                style = VitalisType.Small, color = VitalisColors.InkMuted,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (yesterdayKcal != null) PillButton("Salin kemarin · ${Formatters.kcal(yesterdayKcal)} kkal", { onCopyYesterday(type) }, height = 44.dp)
            PillButton("Cari", { onAddFood(type) }, kind = ButtonKind.Outline, height = 44.dp)
        }
    }
}

@Preview(widthDp = 390, heightDp = 1180)
@Composable
private fun DiaryPreview() = VitalisTheme {
    DiaryScreen(DiaryUiState.Sample, {}, {}, {}, {}, {}, {}, {})
}
