package no.gruppe7.amate.feature.onboarding.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.goals.data.local.validRange
import no.gruppe7.amate.feature.onboarding.data.ONBOARDING_NAME_MAX_LENGTH
import no.gruppe7.amate.feature.onboarding.data.OnboardingDraft
import no.gruppe7.amate.feature.onboarding.data.OnboardingRepository
import no.gruppe7.amate.feature.onboarding.data.OnboardingStatus
import no.gruppe7.amate.feature.profile.data.AvatarKeys

enum class OnboardingEntry { LOADING, SETUP, READY, ERROR }
enum class OnboardingStep { PROFILE, GOALS, PERMISSIONS }

data class OnboardingUiState(
    val entry: OnboardingEntry = OnboardingEntry.LOADING,
    val step: OnboardingStep = OnboardingStep.PROFILE,
    val name: String = "",
    val avatarKey: String = "",
    val waterInput: String = "",
    val stepsInput: String = "",
    val skipGoals: Boolean = false,
    val nameError: Boolean = false,
    val avatarError: Boolean = false,
    val waterError: Boolean = false,
    val stepsError: Boolean = false,
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
)

class OnboardingViewModel(
    private val repository: OnboardingRepository,
    private val saved: SavedStateHandle,
    private val userId: Long = 1,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()
    private var observer: Job? = null
    private var lastStatus: OnboardingStatus? = null

    init { retry() }

    fun retry() {
        observer?.cancel()
        _uiState.update { it.copy(entry = OnboardingEntry.LOADING) }
        observer = viewModelScope.launch {
            try {
                repository.observeStatus(userId).collectLatest { status ->
                    val previous = lastStatus
                    lastStatus = status
                    // Vent også på at selve lagringskallet lykkes, ikke bare at en Flow sender et nytt svar.
                    if (_uiState.value.isSaving) return@collectLatest
                    if (status.completed) {
                        // Rydd bare skjemautkastet. Ana Marias senere reset skal få en ny, tom runde.
                        showReady()
                    } else {
                        // Hvis profilen slettes mens oppstarten er åpen, må gamle felt også tømmes.
                        if ((previous?.hasProfile == true && !status.hasProfile) ||
                            (previous?.hasAvatar == true && !status.hasAvatar)) clearDraft()
                        _uiState.value = OnboardingUiState()
                        val draft = repository.loadDraft(userId)
                        _uiState.value = OnboardingUiState(
                            entry = OnboardingEntry.SETUP,
                            step = saved.get<String>("step")?.let { runCatching { OnboardingStep.valueOf(it) }.getOrNull() }
                                ?: OnboardingStep.PROFILE,
                            name = saved["name"] ?: draft.name,
                            avatarKey = saved["avatar"] ?: draft.avatarKey,
                            waterInput = saved["water"] ?: draft.waterGoal?.toString().orEmpty(),
                            stepsInput = saved["steps"] ?: draft.stepsGoal?.toString().orEmpty(),
                            skipGoals = saved["skipGoals"] ?: false,
                        )
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(entry = OnboardingEntry.ERROR) } }
        }
    }

    private fun clearDraft() {
        listOf("name", "avatar", "water", "steps", "step", "skipGoals").forEach { saved.remove<Any>(it) }
    }

    private fun showReady() {
        clearDraft()
        _uiState.value = OnboardingUiState(entry = OnboardingEntry.READY)
    }

    fun changeName(value: String) = edit { copy(name = value, nameError = false) }
    fun selectAvatar(key: String) = edit { copy(avatarKey = key, avatarError = false) }
    fun changeWater(value: String) = edit { copy(waterInput = value, waterError = false, skipGoals = false) }
    fun changeSteps(value: String) = edit { copy(stepsInput = value, stepsError = false, skipGoals = false) }

    fun next() {
        val state = _uiState.value
        if (state.entry != OnboardingEntry.SETUP || state.isSaving) return
        when (state.step) {
            OnboardingStep.PROFILE -> if (validateProfile()) edit { copy(step = OnboardingStep.GOALS) }
            OnboardingStep.GOALS -> if (validateGoals()) edit { copy(step = OnboardingStep.PERMISSIONS, skipGoals = false) }
            OnboardingStep.PERMISSIONS -> finish()
        }
    }

    fun skipGoals() = edit { copy(step = OnboardingStep.PERMISSIONS, skipGoals = true, waterError = false, stepsError = false) }
    fun back() = edit {
        copy(step = if (step == OnboardingStep.PERMISSIONS) OnboardingStep.GOALS else OnboardingStep.PROFILE)
    }

    private fun validateProfile(): Boolean {
        val state = _uiState.value
        val nameError = state.name.trim().isBlank() || state.name.trim().length > ONBOARDING_NAME_MAX_LENGTH
        val avatarError = state.avatarKey !in AvatarKeys.all
        _uiState.update { it.copy(nameError = nameError, avatarError = avatarError) }
        return !nameError && !avatarError
    }

    private fun validateGoals(): Boolean {
        val state = _uiState.value
        val waterError = !validGoal(state.waterInput, GoalType.WATER)
        val stepsError = !validGoal(state.stepsInput, GoalType.STEPS)
        _uiState.update { it.copy(waterError = waterError, stepsError = stepsError) }
        return !waterError && !stepsError
    }

    private fun finish() {
        if (!validateProfile()) { edit { copy(step = OnboardingStep.PROFILE) }; return }
        if (!_uiState.value.skipGoals && !validateGoals()) { edit { copy(step = OnboardingStep.GOALS) }; return }
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repository.complete(userId, OnboardingDraft(state.name.trim(), state.avatarKey,
                    waterGoal = if (state.skipGoals) null else state.waterInput.trim().toIntOrNull(),
                    stepsGoal = if (state.skipGoals) null else state.stepsInput.trim().toIntOrNull()))
                _uiState.update { it.copy(isSaving = false) }
                // Room/prefs kan ha svart mens vi ventet. Åpne hjem først når begge er ferdige.
                if (lastStatus?.completed == true) showReady()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    private fun edit(change: OnboardingUiState.() -> OnboardingUiState) {
        if (_uiState.value.isSaving || _uiState.value.entry != OnboardingEntry.SETUP) return
        _uiState.update { it.change().copy(saveFailed = false) }
        // SavedStateHandle tar vare på feltene også hvis Android må gjenopprette prosessen.
        val state = _uiState.value
        saved["name"] = state.name; saved["avatar"] = state.avatarKey
        saved["water"] = state.waterInput; saved["steps"] = state.stepsInput
        saved["step"] = state.step.name; saved["skipGoals"] = state.skipGoals
    }

    companion object {
        private fun validGoal(input: String, type: GoalType) = input.isBlank() ||
            input.trim().toIntOrNull()?.let { it in type.validRange } == true
        fun factory(repository: OnboardingRepository) = viewModelFactory {
            initializer { OnboardingViewModel(repository, createSavedStateHandle()) }
        }
    }
}
