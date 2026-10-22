package no.gruppe7.amate.feature.steps.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.gruppe7.amate.feature.steps.data.StepsRepository

// ViewModel for skrittskjermen henter data fra stepsrepository
class StepsViewModel(
    private val stepsRepository: StepsRepository,
) : ViewModel() {

    private val permissionGrantedState = MutableStateFlow(false)
    // combine slår sammen tre kilder, og hver gang
    // en av dem endrer seg bygges en ny StepsUiState
    val uiState: StateFlow<StepsUiState> = combine(
        stepsRepository.getStepsForToday(),
        stepsRepository.getStepsHistory(),
        permissionGrantedState,
    ) { stepsToday, historyList, permissionGranted ->
        StepsUiState(
            stepsToday = stepsToday,
            sensorAvailable = stepsRepository.isSensorAvailable,
            permissionGranted = permissionGranted,
            history = historyList,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StepsUiState(sensorAvailable = stepsRepository.isSensorAvailable),
    )

    fun onPermissionResult(isGranted: Boolean) {
        permissionGrantedState.value = isGranted
    }
    // legger til skritt manuelt kan bruker til feks testknapp
    fun addStep(count: Int = 1) {
        viewModelScope.launch {
            stepsRepository.incrementStep(userId = 0, count = count)
        }
    }

    class Factory(
        private val stepsRepository: StepsRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StepsViewModel(stepsRepository) as T
        }
    }
}
