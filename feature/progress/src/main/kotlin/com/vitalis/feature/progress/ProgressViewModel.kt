package com.vitalis.feature.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.vitalis.core.designsystem.theme.VitalisColors
import com.vitalis.data.ProgressData
import com.vitalis.data.ProgressRepository
import com.vitalis.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val progress: ProgressRepository,
    private val users: UserRepository,
) : ViewModel() {
    private val range = MutableStateFlow(ProgressRange.Month)
    private val data = MutableStateFlow<ProgressData?>(null)

    val state: StateFlow<ProgressUiState?> = combine(data, range) { d, r -> d?.let { progressUiState(it, r, LocalDate.now()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A snapshot, not a live query: the tab reloads whenever it comes back into view. */
    fun reload() {
        viewModelScope.launch { data.value = progress.load() }
    }

    fun setRange(r: ProgressRange) { range.value = r }

    fun logWeight(kg: Float) {
        viewModelScope.launch {
            users.logWeight(kg)
            data.value = progress.load()
        }
    }
}

@Composable
fun ProgressRoute(vm: ProgressViewModel = hiltViewModel()) {
    LifecycleResumeEffect(Unit) {
        vm.reload()
        onPauseOrDispose {}
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = state ?: return Box(Modifier.fillMaxSize().background(VitalisColors.Ground))
    ProgressScreen(s, vm::setRange, vm::logWeight)
}
