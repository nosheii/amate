package no.gruppe7.amate.feature.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import no.gruppe7.amate.feature.weather.data.WeatherRepository

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val place: WeatherPlace = WeatherPlace.OSLO,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState(place = place))
    val uiState = _uiState.asStateFlow()
    private var request: Job? = null
    private var lastAttempt: Instant? = null

    init { refresh() }

    // Kalles når hjemskjermen vises igjen. Ingen bakgrunnsløkke henter vær hele tiden.
    // Vi venter litt mellom automatiske forsøk, også ved nettfeil.
    fun refreshIfNeeded() {
        val last = lastAttempt
        if (last == null || Duration.between(last, clock.instant()) >= Duration.ofMinutes(15)) refresh()
    }

    fun refresh() {
        // Flere raske trykk skal ikke starte flere like nettverkskall.
        if (request?.isActive == true) return
        lastAttempt = clock.instant()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        request = viewModelScope.launch {
            try {
                val forecast = repository.getWeather(place)
                _uiState.update { it.copy(forecast = forecast) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Jeg beholder forrige varsel hvis oppdateringen feiler. Kortet merker det tydelig.
                // Tekniske feilmeldinger fra nettet vises ikke rett til brukeren.
                _uiState.update { it.copy(loadFailed = true) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
