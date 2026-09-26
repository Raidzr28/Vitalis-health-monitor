package com.vitalis.feature.onboarding

import com.vitalis.core.model.ActivityLevel
import com.vitalis.core.model.Goal
import com.vitalis.core.model.Sex
import com.vitalis.core.model.UserProfile
import java.time.LocalDate
import java.time.Period

/** Everything the onboarding form collects. Numbers stay as typed text until validated. */
data class OnboardingDraft(
    val goal: Goal = Goal.LOSE,
    val rateKgPerWeek: Float = 0.5f,
    val targetWeight: String = "",
    val name: String = "",
    val sex: Sex? = null,
    val birthDate: LocalDate? = null,
    val height: String = "",
    val weight: String = "",
    val activityLevel: ActivityLevel? = null,
) {
    val heightCm: Float? get() = height.toKg()
    val weightKg: Float? get() = weight.toKg()
    val targetWeightKg: Float? get() = targetWeight.toKg()

    fun toProfile(): UserProfile = UserProfile(
        id = UserProfile.DEFAULT_ID,
        displayName = name.trim(),
        sex = requireNotNull(sex),
        birthDate = requireNotNull(birthDate),
        heightCm = requireNotNull(heightCm),
        activityLevel = requireNotNull(activityLevel),
        goal = goal,
        goalRateKgPerWeek = if (goal == Goal.MAINTAIN) 0f else rateKgPerWeek,
        targetWeightKg = if (goal == Goal.MAINTAIN) null else targetWeightKg,
    )
}

/** Accepts "72,5" as well as "72.5" — Indonesian keyboards default to the comma. */
private fun String.toKg(): Float? = trim().replace(',', '.').toFloatOrNull()

enum class OnboardingStep { Goal, Body, Activity, Result }

/**
 * Input validation for one step (trust boundary: free-typed numbers).
 * Returns field → message; empty means the step may advance.
 */
fun OnboardingDraft.errors(step: OnboardingStep, today: LocalDate): Map<String, String> = buildMap {
    when (step) {
        OnboardingStep.Goal -> if (goal != Goal.MAINTAIN) {
            val t = targetWeightKg
            when {
                targetWeight.isBlank() -> put("target", "Isi target berat badan")
                t == null || t !in WEIGHT_RANGE -> put("target", "Masukkan berat antara 30 dan 250 kg")
            }
        }
        OnboardingStep.Body -> {
            if (name.isBlank()) put("name", "Isi nama panggilan") else if (name.trim().length > 30) put("name", "Maksimal 30 karakter")
            if (sex == null) put("sex", "Pilih salah satu")
            val age = birthDate?.let { Period.between(it, today).years }
            when {
                birthDate == null -> put("birth", "Pilih tanggal lahir")
                age == null || age < MIN_AGE -> put("birth", "VITALIS untuk usia $MIN_AGE tahun ke atas")
                age > MAX_AGE -> put("birth", "Periksa lagi tanggal lahirnya")
            }
            val h = heightCm
            if (h == null || h !in HEIGHT_RANGE) put("height", "Masukkan tinggi antara 120 dan 230 cm")
            val w = weightKg
            if (w == null || w !in WEIGHT_RANGE) put("weight", "Masukkan berat antara 30 dan 250 kg")
            // The goal step asked for a target before we knew the current weight — check direction here.
            val t = targetWeightKg
            if (w != null && t != null && goal == Goal.LOSE && t >= w) put("weight", "Target ${fmt(t)} kg harus di bawah berat sekarang")
            if (w != null && t != null && goal == Goal.GAIN && t <= w) put("weight", "Target ${fmt(t)} kg harus di atas berat sekarang")
        }
        OnboardingStep.Activity -> if (activityLevel == null) put("activity", "Pilih level aktivitas")
        OnboardingStep.Result -> Unit
    }
}

private fun fmt(v: Float) = if (v % 1f == 0f) v.toInt().toString() else v.toString().replace('.', ',')

val WEIGHT_RANGE = 30f..250f
val HEIGHT_RANGE = 120f..230f
const val MIN_AGE = 13
const val MAX_AGE = 100
