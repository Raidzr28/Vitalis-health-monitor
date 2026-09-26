package com.vitalis.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vitalis.core.designsystem.component.MainTab
import com.vitalis.core.designsystem.component.VitalisTabBar
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.model.GpsSignalQuality
import com.vitalis.core.model.MealType
import com.vitalis.core.model.SportType
import com.vitalis.feature.dashboard.TodayRoute
import com.vitalis.feature.diary.DiaryRoute
import com.vitalis.feature.diary.FoodSearchRoute
import com.vitalis.feature.onboarding.OnboardingRoute
import com.vitalis.feature.profile.ProfileRoute
import com.vitalis.feature.progress.ProgressScreen
import com.vitalis.feature.progress.ProgressUiState
import com.vitalis.feature.tracking.ActivitySummaryScreen
import com.vitalis.feature.tracking.LiveTrackingScreen
import com.vitalis.feature.tracking.LiveUiState
import com.vitalis.feature.tracking.SportSelectScreen
import com.vitalis.feature.tracking.SummaryUiState
import java.time.LocalDate
import kotlinx.coroutines.delay

private object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val DIARY = "diary"
    const val SEARCH = "search/{meal}/{date}"
    const val SPORT = "sport"
    const val LIVE = "live"
    const val SUMMARY = "summary"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    fun search(meal: MealType, date: LocalDate = LocalDate.now()) = "search/${meal.name}/$date"
}

private val tabRoutes = mapOf(
    MainTab.Today to Routes.TODAY,
    MainTab.Diary to Routes.DIARY,
    MainTab.Track to Routes.SPORT,
    MainTab.Progress to Routes.PROGRESS,
    MainTab.Profile to Routes.PROFILE,
)

@Composable
fun VitalisApp(startAtOnboarding: Boolean) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val currentTab = tabRoutes.entries.firstOrNull { it.value == route }?.key

    // ponytail: tracking + progress still run on sample state until the GPS and body sprints.
    var progress by remember { mutableStateOf(ProgressUiState.Sample) }
    var sport by rememberSaveable { mutableStateOf(SportType.RUNNING) }
    var live by remember { mutableStateOf(LiveUiState.Sample) }

    Box(Modifier.fillMaxSize().background(VitalisColors.Ground)) {
        NavHost(nav, startDestination = if (startAtOnboarding) Routes.ONBOARDING else Routes.TODAY) {
            composable(Routes.ONBOARDING) {
                OnboardingRoute(onFinished = { nav.navigate(Routes.TODAY) { popUpTo(Routes.ONBOARDING) { inclusive = true } } })
            }
            composable(Routes.TODAY) {
                TodayRoute(
                    onOpenDiary = { nav.navigateTab(Routes.DIARY) },
                    onAddFood = { nav.navigate(Routes.search(it)) },
                    onOpenProfile = { nav.navigateTab(Routes.PROFILE) },
                )
            }
            composable(Routes.DIARY) {
                DiaryRoute(onAddFood = { meal, date -> nav.navigate(Routes.search(meal, date)) })
            }
            composable(Routes.SEARCH) { FoodSearchRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.SPORT) {
                SportSelectScreen(
                    selected = sport,
                    lastUsed = emptyMap(),
                    gps = GpsSignalQuality.EXCELLENT, gpsAccuracyM = 4,
                    onSelect = { sport = it },
                    onStart = { live = LiveUiState.Sample.copy(sport = sport); nav.navigate(Routes.LIVE) },
                )
            }
            composable(Routes.LIVE) {
                // ponytail: local ticker stands in for the tracking foreground service (spec §8.2).
                LaunchedEffect(live.isPaused) {
                    while (!live.isPaused) {
                        delay(1000)
                        live = live.copy(movingSeconds = live.movingSeconds + 1)
                    }
                }
                LiveTrackingScreen(
                    state = live,
                    onTogglePause = { live = live.copy(isPaused = !live.isPaused) },
                    onLap = {},
                    onStop = { nav.navigate(Routes.SUMMARY) { popUpTo(Routes.SPORT) } },
                    onLock = {},
                )
            }
            composable(Routes.SUMMARY) {
                val done = { nav.navigate(Routes.TODAY) { popUpTo(Routes.TODAY) { inclusive = true } } }
                ActivitySummaryScreen(
                    state = SummaryUiState.Sample.copy(movingSeconds = live.movingSeconds),
                    onBack = { nav.popBackStack() },
                    onShare = {},
                    onSave = { done() },
                    onDiscard = { done() },
                )
            }
            composable(Routes.PROGRESS) { ProgressScreen(progress) { progress = progress.copy(range = it) } }
            composable(Routes.PROFILE) { ProfileRoute(onOpenSettings = {}) }
        }

        if (currentTab != null) {
            VitalisTabBar(currentTab, onSelect = { nav.navigateTab(tabRoutes.getValue(it)) }, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(Routes.TODAY) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
