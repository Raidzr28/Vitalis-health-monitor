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
import com.vitalis.data.UserRepository
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
class ProfileViewModel @Inject constructor(users: UserRepository) : ViewModel() {
    val state: StateFlow<ProfileUiState?> = combine(users.profile, users.gamification) { profile, game ->
        profile ?: return@combine null
        val level = LevelCurve.progressFor(game.totalXp)
        val joined = profile.createdAt.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id")))
        // ponytail: quests and badges stay sample until the gamification sprint persists them.
        ProfileUiState.Sample.copy(
            name = profile.displayName,
            joined = "Bergabung $joined",
            level = level.level,
            xpIntoLevel = level.xpIntoLevel.toInt(),
            xpForLevel = level.xpNeededForLevel.toInt(),
            streakDays = game.currentStreakDays,
            freezes = game.streakFreezesRemaining,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
fun ProfileRoute(onOpenSettings: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return Box(Modifier.fillMaxSize().background(VitalisColors.Ground))
    ProfileScreen(s, onOpenSettings)
}
