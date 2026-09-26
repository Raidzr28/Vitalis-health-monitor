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
import com.vitalis.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
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
) : ViewModel() {
    // ponytail: date fixed at creation; a screen left open past midnight shows yesterday until reopened.
    private val date = LocalDate.now()

    val state: StateFlow<TodayUiState?> = combine(
        users.profile,
        users.observeDay(date),
        nutrition.observeLogs(date),
        nutrition.observeWaterMl(date),
        combine(activity.observeSessionsOn(date), users.gamification, ::Pair),
    ) { profile, day, logs, water, (sessions, game) ->
        if (profile == null || day == null) null
        else todayUiState(date, profile, day, logs, water, sessions, game)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addWater() {
        viewModelScope.launch { nutrition.addWater(date, 250) }
    }
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
        stepsByHour = emptyList(), // filled once the step-counter sprint records hourly buckets
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
