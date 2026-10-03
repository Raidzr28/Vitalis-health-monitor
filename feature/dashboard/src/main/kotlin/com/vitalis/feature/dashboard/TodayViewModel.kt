package com.vitalis.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.domain.energy.NeatCalculator
import com.vitalis.core.domain.gamification.LevelCurve
import com.vitalis.core.model.ActivitySession
import com.vitalis.core.model.EnergyBudget
import com.vitalis.core.model.FoodLog
import com.vitalis.core.model.GamificationState
import com.vitalis.core.model.Macros
import com.vitalis.core.model.UserProfile
import com.vitalis.data.ActivityRepository
import com.vitalis.data.DayStats
import com.vitalis.data.NutritionRepository
import com.vitalis.data.StepRepository
import com.vitalis.data.StepTracking
import com.vitalis.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** WHO's 150 min/week spread over five days (spec §4.4.2 activity pillar). */
const val ACTIVE_MINUTES_TARGET = 30

@HiltViewModel
class TodayViewModel @Inject constructor(
    users: UserRepository,
    private val nutrition: NutritionRepository,
    activity: ActivityRepository,
    private val steps: StepRepository,
) : ViewModel() {
    // ponytail: date fixed at creation; a screen left open past midnight shows yesterday until reopened.
    private val date = LocalDate.now()
    private val stepTracking = MutableStateFlow(steps.tracking())

    val state: StateFlow<TodayUiState?> = combine(
        users.profile,
        users.observeDay(date),
        nutrition.observeLogs(date),
        nutrition.observeWaterMl(date),
        combine(activity.observeSessionsOn(date), users.gamification, steps.observeHours(date), stepTracking, ::TodayExtras),
    ) { profile, day, logs, water, x ->
        if (profile == null || day == null) null
        else todayUiState(date, profile, day, logs, water, x.sessions, x.game, x.stepHours, x.stepTracking)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** On every return to Today, and right after the permission is granted: sample the counter now. */
    fun refreshSteps() {
        stepTracking.value = steps.tracking()
        if (stepTracking.value != StepTracking.ACTIVE) return
        steps.scheduleBackgroundSync()
        viewModelScope.launch { steps.sync() }
    }

    fun addWater() {
        viewModelScope.launch { nutrition.addWater(date, 250) }
    }
}

private data class TodayExtras(
    val sessions: List<ActivitySession>,
    val game: GamificationState,
    val stepHours: List<Int>,
    val stepTracking: StepTracking,
)

/** 24 hourly counts → 12 two-hour bars scaled to the busiest one (a half-width card has no room for 24). */
fun stepBars(hours: List<Int>): List<Float> {
    val bars = hours.chunked(2) { it.sum() }
    val max = bars.maxOrNull()?.takeIf { it > 0 } ?: return List(bars.size) { 0f }
    return bars.map { it / max.toFloat() }
}

private val idLocale = Locale.forLanguageTag("id")

/** Pure mapping from stored data to what the Today screen draws. */
fun todayUiState(
    date: LocalDate,
    profile: UserProfile,
    day: DayStats,
    logs: List<FoodLog>,
    waterMl: Int,
    sessions: List<ActivitySession>,
    game: GamificationState,
    stepHours: List<Int> = List(24) { 0 },
    stepTracking: StepTracking = StepTracking.ACTIVE,
): TodayUiState {
    val eaten = logs.fold(Macros.EMPTY) { acc, l -> acc + l.macros }
    val t = day.targets
    val level = LevelCurve.progressFor(game.totalXp)
    val meals = logs.groupBy { it.mealType }
        .toSortedMap(compareBy { it.ordinal })
        .map { (type, items) -> MealSummary(type, items.sumOf { it.macros.kcal.toDouble() }.roundToInt(), items.joinToString(", ") { it.foodName }) }

    return TodayUiState(
        name = profile.displayName,
        dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", idLocale)).replaceFirstChar { it.uppercase() },
        streakDays = game.currentStreakDays,
        budget = EnergyBudget(
            targetKcal = t.targetKcal,
            consumedKcal = eaten.kcal.roundToInt(),
            burnedNetKcal = sessions.sumOf { it.kcalNet },
            bmrKcal = t.bmrKcal,
        ),
        activeMinutes = (sessions.sumOf { it.movingSeconds } / 60).toInt(),
        activeTarget = ACTIVE_MINUTES_TARGET,
        steps = day.steps,
        stepsTarget = t.stepsTarget,
        stepsKm = NeatCalculator.distanceMetersFromSteps(day.steps, profile.heightCm.toDouble(), profile.sex) / 1000.0,
        stepsByHour = stepBars(stepHours),
        stepTracking = stepTracking,
        macros = listOf(
            MacroProgress("Protein", eaten.proteinG.roundToInt(), t.macros.proteinG, VitalisColors.Ink),
            MacroProgress("Karbo", eaten.carbsG.roundToInt(), t.macros.carbsG, VitalisColors.Orange),
            MacroProgress("Lemak", eaten.fatG.roundToInt(), t.macros.fatG, VitalisColors.Olive),
        ),
        waterMl = waterMl,
        waterTargetMl = t.waterTargetMl,
        meals = meals,
        level = level.level,
        tierLabel = level.tier.name.lowercase().replaceFirstChar { it.uppercase() },
        xp = level.xpIntoLevel.toInt(),
        xpForNext = level.xpNeededForLevel.toInt(),
    )
}
