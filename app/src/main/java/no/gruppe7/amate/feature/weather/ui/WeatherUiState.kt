package no.gruppe7.amate.feature.weather.ui

data class WeatherUiState(
    val isLoading: Boolean = false,
    val locationPermissionGranted: Boolean = false,
)
