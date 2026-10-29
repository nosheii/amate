package no.gruppe7.amate.feature.weather.ui

import no.gruppe7.amate.feature.weather.data.WeatherForecast
import no.gruppe7.amate.feature.weather.data.WeatherPlace

// Skjermen leser bare denne pakken med informasjon. null betyr at vi ikke har et værvarsel ennå.
data class WeatherUiState(
    val place: WeatherPlace = WeatherPlace.OSLO,
    val isLoading: Boolean = true,
    val forecast: WeatherForecast? = null,
    val loadFailed: Boolean = false,
)
