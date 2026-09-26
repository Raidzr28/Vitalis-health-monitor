package com.vitalis.feature.diary

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalis.core.common.format.Formatters
import com.vitalis.core.designsystem.component.Chip
import com.vitalis.core.designsystem.component.CircleIconButton
import com.vitalis.core.designsystem.component.PillButton
import com.vitalis.core.designsystem.component.Stat
import com.vitalis.core.designsystem.component.VTextField
import com.vitalis.core.designsystem.label
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.core.designsystem.theme.VitalisType
import com.vitalis.core.model.FoodItem
import com.vitalis.core.model.FoodSource
import com.vitalis.core.model.MealType
import com.vitalis.data.FoodFilter
import java.util.Locale
import kotlin.math.roundToInt

fun FoodFilter.label(): String = when (this) {
    FoodFilter.ALL -> "Semua"
    FoodFilter.RECENT -> "Terakhir"
    FoodFilter.FREQUENT -> "Sering"
    FoodFilter.MINE -> "Buatan saya"
}

private fun FoodItem.perServing(qty: Float, per100g: Float) = per100g * servingSizeG / 100f * qty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    filter: FoodFilter,
    onFilterChange: (FoodFilter) -> Unit,
    results: List<FoodItem>,
    targetMeal: MealType,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onAdd: (food: FoodItem, servings: Float, meal: MealType) -> Unit,
    /** Validates and saves; returns field errors, empty when saved. */
    onSaveCustom: (CustomFoodInput) -> Map<String, String>,
) {
    var selected by remember { mutableStateOf<FoodItem?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(VitalisColors.Ground).statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Kembali", onBack)
            Row(
                Modifier.weight(1f).height(48.dp).clip(CircleShape).background(VitalisColors.Card).border(1.5.dp, VitalisColors.Ink, CircleShape).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = VitalisColors.InkMuted)
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = VitalisType.Body.copy(color = VitalisColors.Ink),
                    cursorBrush = SolidColor(VitalisColors.Ink),
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Cari makanan" },
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Cari makanan atau minuman", style = VitalisType.Body, color = VitalisColors.InkFaint)
                        inner()
                    },
                )
            }
            CircleIconButton(Icons.Rounded.QrCodeScanner, "Scan barcode", onScan, size = 48.dp, background = VitalisColors.Lime, border = null)
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FoodFilter.entries.forEach { Chip(it.label(), it == filter, onClick = { onFilterChange(it) }) }
        }

        LazyColumn(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)).background(VitalisColors.Card),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            if (results.isEmpty()) {
                item {
                    Text(
                        when {
                            query.isNotBlank() -> "Tidak ada hasil untuk “${query.trim()}”."
                            filter == FoodFilter.RECENT -> "Belum ada makanan yang pernah kamu catat."
                            filter == FoodFilter.FREQUENT -> "Makanan yang sering kamu catat akan muncul di sini."
                            filter == FoodFilter.MINE -> "Belum ada makanan buatanmu."
                            else -> "Katalog kosong."
                        },
                        style = VitalisType.Body, color = VitalisColors.InkMuted,
                        modifier = Modifier.padding(18.dp),
                    )
                }
            }
            itemsIndexed(results, key = { _, f -> f.id }) { i, food ->
                if (i > 0) HorizontalDivider(color = VitalisColors.Hairline)
                Row(
                    Modifier.fillMaxWidth().clickable { selected = food }.padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(food.name, style = VitalisType.Body, color = VitalisColors.Ink)
                        Text("${food.servingLabel} · ${food.source.label()}", style = VitalisType.Caption, color = VitalisColors.InkMuted)
                    }
                    Text("${food.perServing(1f, food.kcalPer100g).roundToInt()}", style = VitalisType.BodyStrong.copy(fontSize = VitalisType.Small.fontSize * 1.08f), color = VitalisColors.Ink)
                    CircleIconButton(Icons.Rounded.Add, "Tambah ${food.name}", { selected = food }, size = 36.dp, background = VitalisColors.Ground, border = null, iconSize = 16.dp)
                }
            }
            item {
                HorizontalDivider(color = VitalisColors.Hairline)
                Row(
                    Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(20.dp), tint = VitalisColors.Ink)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Buat makanan sendiri", style = VitalisType.BodyStrong, color = VitalisColors.Ink)
                        Text("Salin dari label gizi kemasan", style = VitalisType.Caption, color = VitalisColors.InkMuted)
                    }
                }
            }
        }
    }

    selected?.let { food ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = VitalisColors.Card,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        ) {
            FoodDetailSheet(food, targetMeal) { servings, meal ->
                onAdd(food, servings, meal)
                selected = null
            }
        }
    }

    if (creating) {
        ModalBottomSheet(
            onDismissRequest = { creating = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = VitalisColors.Card,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        ) {
            CustomFoodSheet(initialName = query.trim()) { input ->
                onSaveCustom(input).also { if (it.isEmpty()) creating = false }
            }
        }
    }
}

@Composable
private fun CustomFoodSheet(initialName: String, onSave: (CustomFoodInput) -> Map<String, String>) {
    var input by remember { mutableStateOf(CustomFoodInput(name = initialName)) }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    val num = KeyboardType.Decimal
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Makanan baru", style = VitalisType.DisplayL.copy(fontSize = VitalisType.DisplayL.fontSize * 0.7f), color = VitalisColors.Ink)
        Text("Isi per porsi, seperti di label gizi. Hanya tersimpan di HP ini.", style = VitalisType.Small, color = VitalisColors.InkMuted)
        VTextField("Nama", input.name, { input = input.copy(name = it) }, placeholder = "Keripik kentang", error = errors["name"])
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VTextField("Porsi", input.servingLabel, { input = input.copy(servingLabel = it) }, Modifier.weight(1f), error = errors["serving"])
            VTextField("Berat porsi", input.servingG, { input = input.copy(servingG = it) }, Modifier.weight(1f), suffix = "g", placeholder = "100", error = errors["grams"], keyboardType = num)
        }
        VTextField("Kalori", input.kcal, { input = input.copy(kcal = it) }, suffix = "kkal", placeholder = "0", error = errors["kcal"], keyboardType = num)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VTextField("Protein", input.protein, { input = input.copy(protein = it) }, Modifier.weight(1f), suffix = "g", placeholder = "0", error = errors["protein"], keyboardType = num)
            VTextField("Karbo", input.carbs, { input = input.copy(carbs = it) }, Modifier.weight(1f), suffix = "g", placeholder = "0", error = errors["carbs"], keyboardType = num)
            VTextField("Lemak", input.fat, { input = input.copy(fat = it) }, Modifier.weight(1f), suffix = "g", placeholder = "0", error = errors["fat"], keyboardType = num)
        }
        PillButton("Simpan makanan", { errors = onSave(input) }, Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

@Composable
private fun FoodDetailSheet(food: FoodItem, initialMeal: MealType, onAdd: (Float, MealType) -> Unit) {
    var qty by remember { mutableFloatStateOf(1f) }
    var meal by remember { mutableStateOf(initialMeal) }
    val id = Locale.forLanguageTag("id")
    fun g(v: Float) = if (v >= 10) "${v.roundToInt()}" else String.format(id, "%.1f", v)
    val kcal = food.perServing(qty, food.kcalPer100g).roundToInt()

    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(food.name, style = VitalisType.DisplayL.copy(fontSize = VitalisType.DisplayL.fontSize * 0.7f), color = VitalisColors.Ink)
            Text("${food.source.label()} · per ${food.servingLabel} (${food.servingSizeG.roundToInt()} g) · nilai bisa kamu ubah", style = VitalisType.Small, color = VitalisColors.InkMuted)
        }

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(VitalisColors.Ground).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Porsi", style = VitalisType.Small, color = VitalisColors.InkMuted, modifier = Modifier.weight(1f))
            CircleIconButton(Icons.Rounded.Remove, "Kurangi porsi", { qty = (qty - 0.5f).coerceAtLeast(0.5f) }, size = 40.dp, border = androidx.compose.foundation.BorderStroke(1.dp, VitalisColors.BorderControl), iconSize = 16.dp)
            Text(
                portionLabel(qty, food.servingLabel),
                style = VitalisType.BodyStrong, color = VitalisColors.Ink, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 96.dp).padding(horizontal = 6.dp),
            )
            CircleIconButton(Icons.Rounded.Add, "Tambah porsi", { qty = (qty + 0.5f).coerceAtMost(10f) }, size = 40.dp, background = VitalisColors.Ink, tint = androidx.compose.ui.graphics.Color.White, border = null, iconSize = 16.dp)
        }

        Row {
            Stat("kkal", "$kcal", Modifier.weight(1f), valueStyle = VitalisType.Value.copy(fontSize = VitalisType.Value.fontSize * 1.1f))
            Stat("protein g", g(food.perServing(qty, food.proteinPer100g)), Modifier.weight(1f), valueStyle = VitalisType.Value.copy(fontSize = VitalisType.Value.fontSize * 1.1f))
            Stat("karbo g", g(food.perServing(qty, food.carbsPer100g)), Modifier.weight(1f), valueStyle = VitalisType.Value.copy(fontSize = VitalisType.Value.fontSize * 1.1f))
            Stat("lemak g", g(food.perServing(qty, food.fatPer100g)), Modifier.weight(1f), valueStyle = VitalisType.Value.copy(fontSize = VitalisType.Value.fontSize * 1.1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).forEach { m ->
                val on = m == meal
                Text(
                    m.label(),
                    style = VitalisType.Caption,
                    color = if (on) VitalisColors.Ink else VitalisColors.InkMuted,
                    modifier = Modifier.clip(CircleShape).background(if (on) VitalisColors.Lime else VitalisColors.Ground).clickable { meal = m }.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        PillButton("Tambah ke ${meal.label().lowercase()} · $kcal kkal", { onAdd(qty, meal) }, Modifier.fillMaxWidth())
    }
}

val SampleFoods = listOf(
    FoodItem("ayam-bakar", "Ayam bakar (paha)", source = FoodSource.LOCAL_ID, servingSizeG = 100f, servingLabel = "1 potong", kcalPer100g = 290f, proteinPer100g = 27f, carbsPer100g = 4f, fatPer100g = 18f),
    FoodItem("sate-ayam", "Sate ayam", source = FoodSource.LOCAL_ID, servingSizeG = 150f, servingLabel = "10 tusuk", kcalPer100g = 227f, proteinPer100g = 20f, carbsPer100g = 8f, fatPer100g = 13f),
    FoodItem("soto-ayam", "Soto ayam", source = FoodSource.LOCAL_ID, servingSizeG = 400f, servingLabel = "1 mangkuk", kcalPer100g = 78f, proteinPer100g = 6f, carbsPer100g = 6f, fatPer100g = 3.4f),
    FoodItem("ayam-goreng", "Ayam goreng tepung", source = FoodSource.USDA, servingSizeG = 120f, servingLabel = "1 potong", kcalPer100g = 267f, proteinPer100g = 19f, carbsPer100g = 11f, fatPer100g = 16f),
    FoodItem("dada-ayam", "Dada ayam panggang", source = FoodSource.USDA, servingSizeG = 100f, servingLabel = "100 g", kcalPer100g = 165f, proteinPer100g = 31f, carbsPer100g = 0f, fatPer100g = 3.6f),
    FoodItem("nugget", "Nugget ayam", source = FoodSource.OPEN_FOOD_FACTS, servingSizeG = 80f, servingLabel = "4 keping", kcalPer100g = 262f, proteinPer100g = 14f, carbsPer100g = 16f, fatPer100g = 16f),
)

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun FoodSearchPreview() = VitalisTheme {
    FoodSearchScreen("ayam", {}, FoodFilter.ALL, {}, SampleFoods, MealType.DINNER, {}, {}, { _, _, _ -> }, { emptyMap() })
}
