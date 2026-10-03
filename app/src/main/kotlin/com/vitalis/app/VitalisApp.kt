package com.vitalis.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.vitalis.core.model.MealType
import com.vitalis.core.model.SportType
import com.vitalis.feature.dashboard.TodayRoute
import com.vitalis.feature.diary.DiaryRoute
import com.vitalis.feature.diary.FoodSearchRoute
import com.vitalis.feature.onboarding.OnboardingRoute
import com.vitalis.feature.profile.BadgesRoute
import com.vitalis.feature.profile.CelebrationOverlay
import com.vitalis.feature.profile.ProfileRoute
import com.vitalis.feature.profile.XpHistoryRoute
import com.vitalis.feature.progress.ProgressRoute
import com.vitalis.feature.tracking.ActivitySummaryRoute
import com.vitalis.feature.tracking.LiveTrackingRoute
import com.vitalis.feature.tracking.LocationPermissionRoute
import com.vitalis.feature.tracking.SportSelectRoute
import java.time.LocalDate

private object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val DIARY = "diary"
    const val SEARCH = "search/{meal}/{date}"
    const val SPORT = "sport"
    const val LIVE = "live"
    const val SUMMARY = "summary"
    const val LOCATION = "location-permission/{sport}"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val XP_HISTORY = "xp-history"
    const val BADGES = "badges"
    fun search(meal: MealType, date: LocalDate = LocalDate.now()) = "search/${meal.name}/$date"
    fun location(sport: SportType) = "location-permission/${sport.name}"
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
                SportSelectRoute(
                    onOpenLive = { nav.navigate(Routes.LIVE) { launchSingleTop = true } },
                    onNeedPermission = { nav.navigate(Routes.location(it)) { launchSingleTop = true } },
                )
            }
            composable(Routes.LOCATION) { entry ->
                val sport = entry.arguments?.getString("sport")?.let { name -> SportType.entries.find { it.name == name } } ?: SportType.RUNNING
                LocationPermissionRoute(
                    sport = sport,
                    onGranted = { nav.navigate(Routes.LIVE) { popUpTo(Routes.SPORT); launchSingleTop = true } },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.LIVE) {
                LiveTrackingRoute(
                    onStopped = { nav.navigate(Routes.SUMMARY) { popUpTo(Routes.SPORT) } },
                    onBack = { nav.navigateTab(Routes.TODAY) },
                )
            }
            composable(Routes.SUMMARY) {
                ActivitySummaryRoute(
                    onBack = { nav.popBackStack() },
                    onDone = { nav.navigate(Routes.TODAY) { popUpTo(Routes.TODAY) { inclusive = true } } },
                )
            }
            composable(Routes.PROGRESS) { ProgressRoute() }
            composable(Routes.PROFILE) { ProfileRoute(onOpenSettings = {}, onOpenXpHistory = { nav.navigate(Routes.XP_HISTORY) }, onOpenBadges = { nav.navigate(Routes.BADGES) }) }
            composable(Routes.BADGES) { BadgesRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.XP_HISTORY) { XpHistoryRoute(onBack = { nav.popBackStack() }) }
        }

        if (currentTab != null) {
            VitalisTabBar(currentTab, onSelect = { nav.navigateTab(tabRoutes.getValue(it)) }, modifier = Modifier.align(Alignment.BottomCenter))
        }

        // Above everything, tab bar included: level-ups and streak milestones from any screen.
        CelebrationOverlay()
    }
}

private fun NavHostController.navigateTab(route: String) = navigate(route) {
    popUpTo(Routes.TODAY) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
