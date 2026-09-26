package com.vitalis.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vitalis.core.designsystem.theme.VitalisTheme
import com.vitalis.data.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Onboarding is "done" exactly when a profile row exists — one source of truth, no separate flag. */
@HiltViewModel
class MainViewModel @Inject constructor(users: UserRepository) : ViewModel() {
    /** null while the first DB read is in flight; the splash stays up until then. */
    val hasProfile: StateFlow<Boolean?> = users.profile.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { viewModel.hasProfile.value == null }
        // Light paper ground → dark system-bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            VitalisTheme {
                val hasProfile by viewModel.hasProfile.collectAsStateWithLifecycle()
                // Decide the start destination once (key flips only null → loaded). Finishing onboarding
                // later flips hasProfile to true, which must not rebuild the NavHost mid-navigation.
                val startAtOnboarding = remember(hasProfile == null) { hasProfile?.not() }
                startAtOnboarding?.let { VitalisApp(it) }
            }
        }
    }
}
