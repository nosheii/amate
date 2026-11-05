package no.gruppe7.amate.feature.steps.ui

import no.gruppe7.amate.feature.steps.data.local.DailyStepsEntity

// Alt skrittskjermen trenger å vise
data class StepsUiState(
    val stepsToday: Int = 0,
    val sensorAvailable: Boolean = true,
    val permissionGranted: Boolean = false,
    val history: List<DailyStepsEntity> = emptyList(),
    val stepGoal: Int? = null,
    val isLoading: Boolean = false,
) {
    // Fremdrift i forhold til dagsmål (om satt)
    val progress: Float?
        get() = stepGoal?.takeIf { it > 0 }?.let {
            (stepsToday.toFloat() / it).coerceIn(0f, 1f)
        }
}
