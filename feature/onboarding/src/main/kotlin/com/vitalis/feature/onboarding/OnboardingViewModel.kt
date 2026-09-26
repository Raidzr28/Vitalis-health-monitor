package com.vitalis.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalis.data.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Goal,
    val draft: OnboardingDraft = OnboardingDraft(),
    /** Errors are only shown after the user tried to advance, not while they type. */
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val saveError: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val users: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun edit(transform: (OnboardingDraft) -> OnboardingDraft) = _state.update { it.copy(draft = transform(it.draft)) }

    fun next() = _state.update { s ->
        if (s.draft.errors(s.step, LocalDate.now()).isNotEmpty()) s.copy(showErrors = true)
        else s.copy(step = OnboardingStep.entries[s.step.ordinal + 1], showErrors = false)
    }

    /** @return false when already on the first step, so the caller can leave the screen. */
    fun back(): Boolean {
        val s = _state.value
        if (s.step.ordinal == 0 || s.saving) return false
        _state.update { it.copy(step = OnboardingStep.entries[s.step.ordinal - 1], showErrors = false) }
        return true
    }

    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            runCatching { users.completeOnboarding(s.draft.toProfile(), requireNotNull(s.draft.weightKg)) }
                .onSuccess { onDone() }
                .onFailure { _state.update { it.copy(saving = false, saveError = "Gagal menyimpan profil. Coba lagi.") } }
        }
    }
}
