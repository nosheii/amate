package no.gruppe7.amate.feature.steps.ui

import no.gruppe7.amate.feature.steps.data.local.DailyStepsEntity

data class StepsUiState(
    val stepsToday: Int = 0,
    val sensorAvailable: Boolean = true,
    val permissionGranted: Boolean = false,
    val history: List<DailyStepsEntity> = emptyList(),
)
