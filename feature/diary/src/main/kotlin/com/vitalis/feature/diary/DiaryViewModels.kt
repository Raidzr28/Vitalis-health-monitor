package com.vitalis.feature.diary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vitalis.core.designsystem.component.TabBarClearance
import com.vitalis.data.FoodFilter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vitalis.core.model.FoodItem
import com.vitalis.core.model.FoodLog
import com.vitalis.core.model.MealType
import com.vitalis.data.ActivityRepository
import com.vitalis.data.DayStats
import com.vitalis.data.NutritionRepository
import com.vitalis.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val MAIN_MEALS = listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    users: UserRepository,
    private val nutrition: NutritionRepository,
    activity: ActivityRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val selected = MutableStateFlow(today)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DiaryUiState?> = selected.flatMapLatest { date ->
        val monday = date.minusDays(date.dayOfWeek.value - 1L)
        combine(
            nutrition.observeLogs(date),
            nutrition.observeLogs(date.minusDays(1)),
            users.observeDay(date),
            activity.observeSessionsOn(date).map { s -> s.sumOf { it.kcalNet } },
            nutrition.observeLoggedDates(monday, monday.plusDays(6)),
        ) { logs, yesterday, day, burned, logged -> diaryUiState(date, today, logs, yesterday, day, burned, logged) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(date: LocalDate) {
        if (!date.isAfter(today)) selected.value = date
    }

    /** Deleted entry waiting for Undo; the snackbar reads it and clears it when dismissed. */
    val lastDeleted = MutableStateFlow<FoodLog?>(null)

    fun delete(entryId: String) {
        viewModelScope.launch { lastDeleted.value = nutrition.deleteLog(entryId) }
    }

    fun undoDelete() {
        val log = lastDeleted.value ?: return
        lastDeleted.value = null
        viewModelScope.launch { nutrition.restoreLog(log) }
    }

    fun copyYesterday(meal: MealType) {
        val date = selected.value
        viewModelScope.launch { nutrition.copyMeal(date.minusDays(1), date, meal) }
    }
}

fun diaryUiState(
    date: LocalDate,
    today: LocalDate,
    logs: List<FoodLog>,
    yesterday: List<FoodLog>,
    day: DayStats?,
    burnedNetKcal: Int,
    loggedDates: Set<LocalDate>,
): DiaryUiState {
    val consumed = logs.sumOf { it.macros.kcal.toDouble() }.roundToInt()
    val byMeal = logs.groupBy { it.mealType }
    val extraMeals = byMeal.keys.filter { it !in MAIN_MEALS }.sortedBy { it.ordinal }
    return DiaryUiState(
        selectedDate = date,
        today = today,
        loggedDates = loggedDates,
        consumedKcal = consumed,
        burnedNetKcal = burnedNetKcal,
        remainingKcal = (day?.targets?.targetKcal ?: 0) - consumed + burnedNetKcal,
        proteinG = logs.sumOf { it.macros.proteinG.toDouble() }.roundToInt(),
        carbsG = logs.sumOf { it.macros.carbsG.toDouble() }.roundToInt(),
        fatG = logs.sumOf { it.macros.fatG.toDouble() }.roundToInt(),
        meals = (MAIN_MEALS + extraMeals).map { type ->
            DiaryMeal(type, byMeal[type].orEmpty().map { DiaryEntry(it.id, it.foodName, portionLabel(it.quantity, it.unit), it.macros.kcal.roundToInt()) })
        },
        yesterdayKcal = yesterday.groupBy { it.mealType }.mapValues { (_, l) -> l.sumOf { it.macros.kcal.toDouble() }.roundToInt() },
    )
}

/** "1,5 potong" from quantity 1.5 and serving label "1 potong"; falls back to "1,5 × 100 g". */
fun portionLabel(quantity: Float, servingLabel: String): String {
    val q = String.format(Locale.forLanguageTag("id"), if (quantity % 1f == 0f) "%.0f" else "%.1f", quantity)
    return if (servingLabel.startsWith("1 ")) "$q ${servingLabel.removePrefix("1 ")}" else "$q × $servingLabel"
}

@Composable
fun DiaryRoute(onAddFood: (MealType, LocalDate) -> Unit, viewModel: DiaryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted by viewModel.lastDeleted.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(deleted) {
        val log = deleted ?: return@LaunchedEffect
        val result = snackbar.showSnackbar("${log.foodName} dihapus", actionLabel = "Urungkan", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.lastDeleted.value = null
    }
    val s = state ?: return
    Box(Modifier.fillMaxSize()) {
        DiaryScreen(
            state = s,
            onSelectDate = viewModel::select,
            onPickDate = {},
            onAddFood = { onAddFood(it, s.selectedDate) },
            onCopyYesterday = viewModel::copyYesterday,
            onDelete = { viewModel.delete(it.id) },
            onSearch = { onAddFood(MealType.SNACK, s.selectedDate) },
            onScan = {},
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = TabBarClearance - 24.dp))
    }
}

@HiltViewModel
class FoodSearchViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val nutrition: NutritionRepository,
) : ViewModel() {
    val meal: MealType = savedState.get<String>("meal")?.let(MealType::valueOf) ?: MealType.SNACK
    private val date: LocalDate = savedState.get<String>("date")?.let(LocalDate::parse) ?: LocalDate.now()

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(FoodFilter.ALL)

    init {
        viewModelScope.launch { nutrition.seedLocalCatalogue() }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val results: StateFlow<List<FoodItem>> = combine(query.debounce(150), filter, ::Pair)
        .flatMapLatest { (q, f) -> nutrition.search(q, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(food: FoodItem, servings: Float, meal: MealType, onDone: () -> Unit) {
        viewModelScope.launch {
            nutrition.log(food, servings, meal, date)
            onDone()
        }
    }

    /** Validates synchronously; on success saves and shows it under "Buatan saya". */
    fun saveCustom(input: CustomFoodInput): Map<String, String> = when (val r = input.toFoodItem()) {
        is CustomFoodResult.Invalid -> r.errors
        is CustomFoodResult.Ok -> {
            viewModelScope.launch { nutrition.saveCustomFood(r.item) }
            query.value = r.item.name
            filter.value = FoodFilter.MINE
            emptyMap()
        }
    }
}

@Composable
fun FoodSearchRoute(onBack: () -> Unit, viewModel: FoodSearchViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    FoodSearchScreen(
        query = query,
        onQueryChange = { viewModel.query.value = it },
        filter = filter,
        onFilterChange = { viewModel.filter.value = it },
        results = results,
        targetMeal = viewModel.meal,
        onBack = onBack,
        onScan = {},
        onAdd = { food, servings, meal -> viewModel.add(food, servings, meal, onBack) },
        onSaveCustom = viewModel::saveCustom,
    )
}
