package no.gruppe7.amate.feature.weather.ui

import androidx.lifecycle.ViewModelStore
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.weather.data.WeatherForecast
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import no.gruppe7.amate.feature.weather.data.WeatherRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherViewModelTest {
    private val store = ViewModelStore()
    private val clock = MutableClock(Instant.parse("2026-10-27T12:00:00Z"))
    private val repository = FakeWeatherRepository()

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model() = WeatherViewModel(repository, WeatherPlace.OSLO, clock).also { store.put("weather", it) }

    @Test fun startsLoading_thenShowsForecastForSelectedPlace() = runTest {
        val model = model()
        assertTrue(model.uiState.value.isLoading)
        assertNull(model.uiState.value.forecast)
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(WeatherPlace.OSLO, repository.lastPlace)
        assertEquals(6.5, model.uiState.value.forecast!!.temperatureCelsius, 0.0)
    }

    @Test fun failure_canBeRetriedWithoutRestartingApp() = runTest {
        repository.fail = true
        val model = model()
        runCurrent()
        assertTrue(model.uiState.value.loadFailed)
        assertNull(model.uiState.value.forecast)
        repository.fail = false
        model.refresh()
        runCurrent()
        assertFalse(model.uiState.value.loadFailed)
        assertNotNull(model.uiState.value.forecast)
    }

    @Test fun failedRefresh_keepsPreviousForecastAndMarksFailure() = runTest {
        val model = model()
        runCurrent()
        val previous = model.uiState.value.forecast
        repository.fail = true
        model.refresh()
        assertEquals(previous, model.uiState.value.forecast)
        runCurrent()
        assertEquals(previous, model.uiState.value.forecast)
        assertTrue(model.uiState.value.loadFailed)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun repeatedTaps_doNotStartDuplicateRequests() = runTest {
        repository.gate = CompletableDeferred()
        val model = model()
        runCurrent()
        repeat(5) { model.refresh() }
        runCurrent()
        assertEquals(1, repository.calls)
        repository.gate!!.complete(Unit)
        runCurrent()
        assertNotNull(model.uiState.value.forecast)
    }

    @Test fun returningHome_doesNotRequestAgainUntilFifteenMinutes() = runTest {
        val model = model()
        runCurrent()
        model.refreshIfNeeded()
        runCurrent()
        assertEquals(1, repository.calls)
        clock.now = clock.now.plusSeconds(15 * 60)
        model.refreshIfNeeded()
        runCurrent()
        assertEquals(2, repository.calls)
    }

    @Test fun failedAutomaticRequests_areAlsoSpacedApart() = runTest {
        repository.fail = true
        val model = model()
        runCurrent()
        repeat(5) { model.refreshIfNeeded() }
        runCurrent()
        assertEquals(1, repository.calls)
    }

    @Test fun clearingModel_cancelsWithoutShowingNetworkError() = runTest {
        repository.gate = CompletableDeferred()
        val model = model()
        runCurrent()
        store.clear()
        runCurrent()
        assertFalse(model.uiState.value.loadFailed)
        assertNull(model.uiState.value.forecast)
    }

    @Test fun symbols_doNotTurnMissingOrUnknownWeatherIntoSunshine() {
        assertEquals(R.string.weather_unknown, conditionFor(null))
        assertEquals(R.string.weather_unknown, conditionFor("new_code"))
        assertEquals(R.string.weather_clear, conditionFor("clearsky_night"))
        assertEquals(R.string.weather_thunder, conditionFor("heavyrainandthunder"))
        assertEquals(R.string.weather_sleet, conditionFor("lightsleetshowers_day"))
        assertEquals(R.string.weather_snow, conditionFor("snow"))
    }

    // Ingen ekte nettverk i disse testene. Vi bestemmer selv når svaret lykkes eller feiler.
    private class FakeWeatherRepository : WeatherRepository {
        var calls = 0
        var fail = false
        var gate: CompletableDeferred<Unit>? = null
        var lastPlace: WeatherPlace? = null
        override suspend fun getWeather(place: WeatherPlace): WeatherForecast {
            calls++
            lastPlace = place
            gate?.await()
            if (fail) throw IOException("Offline for test")
            return WeatherForecast(place, 6.5, "cloudy", Instant.parse("2026-10-27T12:00:00Z"),
                Instant.parse("2026-10-27T11:00:00Z"))
        }
    }

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }
}
