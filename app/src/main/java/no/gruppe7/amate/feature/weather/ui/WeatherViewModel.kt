package no.gruppe7.amate.feature.weather.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import no.gruppe7.amate.feature.weather.data.WeatherRepository
import no.gruppe7.amate.feature.weather.location.LocationDataSource
import no.gruppe7.amate.feature.weather.location.WeatherLocationResult

class WeatherViewModel(
    private val repository: WeatherRepository,
    private val place: WeatherPlace = WeatherPlace.OSLO,
    private val clock: Clock = Clock.systemUTC(),
    private val location: LocationDataSource? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WeatherUiState(place = place,
        isLoading = location == null,
        locationStatus = if (location == null) null else WeatherLocationStatus.NEEDS_PERMISSION))
    val uiState = _uiState.asStateFlow()
    private var request: Job? = null
    private var lastAttempt: Instant? = null
    private var locationRequest: Job? = null
    private var permissionGranted = false
    private var requestVersion = 0

    // I appen sjekker Route tillatelsen først. Da slipper vi å hente Oslo før vi vet
    // at brukeren allerede har gitt tilgang til sin egen posisjon.
    init { if (location == null) refresh() }

    fun onLocationPermission(granted: Boolean, hasAsked: Boolean = false, canAskAgain: Boolean = true) {
        if (location == null) return
        val changed = permissionGranted != granted
        permissionGranted = granted
        if (granted) {
            if (changed) locate() else refreshIfNeeded()
        } else {
            val status = when {
                !hasAsked -> WeatherLocationStatus.NEEDS_PERMISSION
                canAskAgain -> WeatherLocationStatus.DENIED
                else -> WeatherLocationStatus.SETTINGS_REQUIRED
            }
            if (changed || _uiState.value.locationStatus != status) {
                stopLocating()
                useFallback(status)
            } else refreshIfNeeded()
        }
    }

    private fun locate() {
        val source = location ?: return
        if (locationRequest?.isActive == true) return
        cancelWeatherRequest()
        lastAttempt = clock.instant()
        // Ikke vis været for det gamle stedet under tittelen «der du er».
        _uiState.update { it.copy(forecast = null, isLoading = false, loadFailed = false,
            locationStatus = WeatherLocationStatus.LOCATING) }
        locationRequest = viewModelScope.launch {
            val result = try {
                withTimeoutOrNull(15.seconds) { source.getCurrentLocation() }
                    ?: WeatherLocationResult.Unavailable
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                WeatherLocationResult.PermissionRequired
            } catch (_: Exception) {
                WeatherLocationResult.Unavailable
            }
            when (result) {
                is WeatherLocationResult.Found -> {
                    // Telefonens tidssone brukes i visningen. Vi gjetter ikke by/adresse
                    // og trenger dermed heller ikke sende posisjonen til en ekstra karttjeneste.
                    val here = WeatherPlace("din posisjon", result.coordinates.latitude,
                        result.coordinates.longitude, ZoneId.systemDefault(), isCurrentLocation = true)
                    _uiState.update { it.copy(place = here, locationStatus = WeatherLocationStatus.CURRENT) }
                    fetchWeather()
                }
                WeatherLocationResult.PermissionRequired -> {
                    permissionGranted = false
                    useFallback(WeatherLocationStatus.NEEDS_PERMISSION)
                }
                WeatherLocationResult.ServicesDisabled -> useFallback(WeatherLocationStatus.DISABLED)
                WeatherLocationResult.Unavailable -> useFallback(WeatherLocationStatus.UNAVAILABLE)
            }
        }
    }

    // Brukes etter et bevisst knappetrykk eller et besøk i posisjonsinnstillingene.
    // Da kan vi avbryte en treg Oslo-henting og prøve brukerens posisjon med en gang.
    fun requestCurrentLocation() {
        if (permissionGranted) locate()
    }

    // Route stopper en pågående posisjonsforespørsel når hjem forsvinner eller appen lukkes.
    // CancellationSignal i Android-delen fjerner også ventingen hos selve posisjonstjenesten.
    fun stopLocating() {
        if (locationRequest?.isActive != true) return
        locationRequest?.cancel()
        locationRequest = null
        lastAttempt = null
        _uiState.update { it.copy(place = place, forecast = null, isLoading = false,
            locationStatus = WeatherLocationStatus.UNAVAILABLE) }
    }

    private fun useFallback(status: WeatherLocationStatus) {
        cancelWeatherRequest()
        _uiState.update { it.copy(place = place, forecast = null, isLoading = false,
            loadFailed = false, locationStatus = status) }
        fetchWeather()
    }

    // Kalles når hjemskjermen vises igjen. Ingen bakgrunnsløkke henter vær hele tiden.
    // Vi venter litt mellom automatiske forsøk, også ved nettfeil.
    fun refreshIfNeeded() {
        val last = lastAttempt
        if (last == null || Duration.between(last, clock.instant()) >= Duration.ofMinutes(15)) refresh()
    }

    fun refresh() {
        // Flere raske trykk skal ikke starte flere like nettverkskall.
        if (request?.isActive == true || locationRequest?.isActive == true) return
        if (permissionGranted && location != null) locate() else fetchWeather()
    }

    private fun cancelWeatherRequest() {
        // Et sent svar fra Oslo skal ikke overskrive et nyere svar for brukerens posisjon.
        requestVersion++
        request?.cancel()
        request = null
    }

    private fun fetchWeather() {
        val version = ++requestVersion
        val selectedPlace = _uiState.value.place
        lastAttempt = clock.instant()
        _uiState.update { it.copy(isLoading = true, loadFailed = false) }
        request = viewModelScope.launch {
            try {
                val forecast = repository.getWeather(selectedPlace)
                if (version == requestVersion) _uiState.update { it.copy(forecast = forecast) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Jeg beholder forrige varsel hvis oppdateringen feiler. Kortet merker det tydelig.
                // Tekniske feilmeldinger fra nettet vises ikke rett til brukeren.
                if (version == requestVersion) _uiState.update { it.copy(loadFailed = true) }
            } finally {
                if (version == requestVersion) _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
