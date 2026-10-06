package com.example.amate.feature.steps.ui

data class StepsUiState(
    val stepsToday: Int = 0,
    val sensorAvailable: Boolean = true,
    val permissionGranted: Boolean = false,
)
