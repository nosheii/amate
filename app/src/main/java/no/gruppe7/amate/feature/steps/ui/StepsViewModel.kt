package no.gruppe7.amate.feature.steps.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.steps.data.StepsRepository

// ViewModel for skrittskjermen henter data fra stepsRepository og goalRepository
class StepsViewModel(
    private val stepsRepository: StepsRepository,
    private val goalRepository: GoalRepository? = null,
    private val userId: Long = 1,
) : ViewModel() {

    private val permissionGrantedState = MutableStateFlow(false)

    // combine slår sammen kilder, og hver gang en av dem endrer seg bygges en ny StepsUiState
    val uiState: StateFlow<StepsUiState> = combine(
        stepsRepository.getStepsForToday(userId),
        stepsRepository.getStepsHistory(userId),
        goalRepository?.observeGoal(userId, GoalType.STEPS) ?: MutableStateFlow(null),
        permissionGrantedState,
    ) { stepsToday, historyList, goalEntity, permissionGranted ->
        val stepGoal = goalEntity?.takeIf { it.isActive }?.targetValue?.takeIf { it > 0 }
        StepsUiState(
            stepsToday = stepsToday,
            sensorAvailable = stepsRepository.isSensorAvailable,
            permissionGranted = permissionGranted,
            history = historyList,
            stepGoal = stepGoal,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StepsUiState(sensorAvailable = stepsRepository.isSensorAvailable),
    )

    fun onPermissionResult(isGranted: Boolean) {
        permissionGrantedState.value = isGranted
        stepsRepository.setSensorPermission(isGranted)
    }
    // legger til skritt manuelt kan bruker til feks testknapp
    fun addStep(count: Int = 1) {
        viewModelScope.launch {
            stepsRepository.incrementStep(userId = userId, count = count)
        }
    }

    class Factory(
        private val stepsRepository: StepsRepository,
        private val goalRepository: GoalRepository? = null,
        private val userId: Long = 1,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StepsViewModel(stepsRepository, goalRepository, userId) as T
        }
    }
}

@Composable
fun rememberStepsViewModel(
    stepsRepository: StepsRepository,
    goalRepository: GoalRepository? = null,
    userId: Long = 1,
): StepsViewModel {
    return viewModel(
        factory = StepsViewModel.Factory(stepsRepository, goalRepository, userId)
    )
}
