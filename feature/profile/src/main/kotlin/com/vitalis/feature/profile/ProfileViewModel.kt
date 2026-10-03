package com.vitalis.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.core.domain.gamification.LevelCurve
import com.vitalis.core.domain.gamification.WeeklyQuests
import com.vitalis.data.GamificationRepository
import java.time.LocalDate
import com.vitalis.data.UserRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProfileViewModel @Inject constructor(users: UserRepository, private val gamification: GamificationRepository) : ViewModel() {
    init {
        // Creates this week's quests if Monday has passed, back-fills badges, counts finished days.
        viewModelScope.launch {
            gamification.refreshQuests()
            gamification.refreshBadges()
        }
    }

    private val today = LocalDate.now()
    private val week = WeeklyQuests.weekStart(today)

    val state: StateFlow<ProfileUiState?> = combine(
        users.profile, users.gamification, gamification.badges, gamification.observeQuests(week),
    ) { profile, game, badges, quests ->
        profile ?: return@combine null
        val level = LevelCurve.progressFor(game.totalXp)
        val joined = profile.createdAt.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id")))
        ProfileUiState.Sample.copy(
            name = profile.displayName,
            joined = "Bergabung $joined",
            level = level.level,
            xpIntoLevel = level.xpIntoLevel.toInt(),
            xpForLevel = level.xpNeededForLevel.toInt(),
            streakDays = game.currentStreakDays,
            freezes = game.streakFreezesRemaining,
            badgeCount = badges.count { it.unlocked },
            badgeTotal = badges.size,
            badges = profileBadges(badges),
            quests = quests.map(::questUi),
            questsEndLabel = questsEndLabel(today, week),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ProfileRoute(onOpenSettings: () -> Unit, onOpenXpHistory: () -> Unit, onOpenBadges: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return Box(Modifier.fillMaxSize().background(VitalisColors.Ground))
    ProfileScreen(s, onOpenSettings, onOpenXpHistory, onOpenBadges)
}
