package no.gruppe7.amate.feature.goals.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.goals.data.local.validRange

// ViewModel-en sitter mellom skjermen og repository-et.
// Skjermen sender kanppetrykk hit, og vi oppdaterer uiState som skjermen tegner.
// ViewModel overlever rotasjon av skjermen og det brukeren skriver på skjermen.
@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModel(
    private val repository: GoalRepository,
    // Fast lokal bruker frem til profilfunksjonen er koblet på (likt som vann)
    private val userId: Long = 1, // fast lokal bruker til profilfunksjonen er koblet på, som i vann
) : ViewModel() {

    // Økes ved "Prøv igjen". flatMapLatest under starter da Flow-en fra Room på nytt.
    private val reload = MutableStateFlow(0) // økes ved "Prøv igjen" så Room-flowen starter på nytt
    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reload
                .flatMapLatest {
                    repository.observeGoals(userId)
                        .onStart {
                            _uiState.update {
                                it.copy(
                                    isLoading = true,
                                    loadFailed = false
                                )
                            }
                        }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loadFailed = true,
                                    errorMessageResId = R.string.goals_load_error
                                )
                            }
                        }
                }
                .collectLatest { goals ->
                    _uiState.update {
                        it.copy(
                            goals = goals,
                            isLoading = false,
                            loadFailed = false
                        )
                    }
                }
        }
    }

    fun retry() {
        dismissError()
        reload.value += 1
    }

    fun dismissError() = _uiState.update { it.copy(errorMessageResId = null) }

    // Åpner feltet for én måltype, forhåndsutfylt med dagens verdi hvis den finnes.
    fun openEditor(type: GoalType) {
        if (_uiState.value.isSaving) return
        val current = _uiState.value.goalFor(type)
        _uiState.update {
            it.copy(
                editingType = type,
                valueInput = current?.targetValue?.toString().orEmpty(),
                inputErrorResId = null
            )
        }
    }

    fun changeValue(input: String) =
        _uiState.update { it.copy(valueInput = input, inputErrorResId = null) }

    fun closeEditor() {
        if (!_uiState.value.isSaving) _uiState.update {
            it.copy(
                editingType = null,
                inputErrorResId = null
            )
        }
    }

    fun saveEditor() {
        val state = _uiState.value
        val type = state.editingType ?: return
        val value = state.valueInput.trim().toIntOrNull()
        if (value == null || value !in type.validRange) {
            _uiState.update { it.copy(inputErrorResId = invalidMessageFor(type)) }
            return
        }
        write {
            repository.saveGoal(userId, type, value)
            _uiState.update { it.copy(editingType = null) }
        }
    }

    fun deleteGoal(goal: GoalEntity) = write { repository.deleteGoal(goal) }

    // GJENBRUK: samme hjelper som i WaterViewModel. Kopier den rett inn i TasksViewModel.
    // Setter isSaving, kjører handlingen, viser feil uten å miste det brukeren skrev.
    private fun write(action: suspend () -> Unit) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessageResId = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessageResId = R.string.goals_save_error) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    companion object {
        fun factory(repository: GoalRepository) = viewModelFactory {
            initializer { GoalsViewModel(repository) }
        }

        fun invalidMessageFor(type: GoalType) = when (type) {
            GoalType.WATER -> R.string.goals_invalid_water
            GoalType.STEPS -> R.string.goals_invalid_steps
        }
    }
}

