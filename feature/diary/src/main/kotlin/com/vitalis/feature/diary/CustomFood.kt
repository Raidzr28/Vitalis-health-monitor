package com.vitalis.feature.diary

import com.vitalis.core.model.FoodItem
import com.vitalis.core.model.FoodSource
import java.util.UUID

/** What a nutrition label gives you: values per serving, as typed. */
data class CustomFoodInput(
    val name: String = "",
    val servingLabel: String = "1 porsi",
    val servingG: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
)

sealed interface CustomFoodResult {
    data class Ok(val item: FoodItem) : CustomFoodResult
    data class Invalid(val errors: Map<String, String>) : CustomFoodResult
}

private fun String.num(): Float? = trim().replace(',', '.').toFloatOrNull()

/** Validates label input and converts per-serving values to the per-100 g the catalogue stores. */
fun CustomFoodInput.toFoodItem(id: String = "user-${UUID.randomUUID()}"): CustomFoodResult {
    val errors = mutableMapOf<String, String>()
    if (name.isBlank()) errors["name"] = "Isi nama makanan" else if (name.trim().length > 60) errors["name"] = "Maksimal 60 karakter"
    if (servingLabel.isBlank()) errors["serving"] = "Contoh: 1 porsi, 1 bungkus"
    val g = servingG.num()
    if (g == null || g !in 1f..2000f) errors["grams"] = "1–2000 g"
    val k = kcal.num()
    if (k == null || k !in 0f..5000f) errors["kcal"] = "0–5000 kkal"
    val p = if (protein.isBlank()) 0f else protein.num()
    val c = if (carbs.isBlank()) 0f else carbs.num()
    val f = if (fat.isBlank()) 0f else fat.num()
    listOf("protein" to p, "carbs" to c, "fat" to f).forEach { (key, v) -> if (v == null || v < 0f) errors[key] = "Angka ≥ 0" }
    if (g != null && p != null && c != null && f != null && p + c + f > g) errors["grams"] = "Protein + karbo + lemak melebihi berat porsi"
    if (errors.isNotEmpty()) return CustomFoodResult.Invalid(errors)

    val per100 = 100f / g!!
    return CustomFoodResult.Ok(
        FoodItem(
            id = id,
            name = name.trim(),
            source = FoodSource.USER,
            servingSizeG = g,
            servingLabel = servingLabel.trim(),
            kcalPer100g = k!! * per100,
            proteinPer100g = p!! * per100,
            carbsPer100g = c!! * per100,
            fatPer100g = f!! * per100,
            isVerified = false,
        ),
    )
}
