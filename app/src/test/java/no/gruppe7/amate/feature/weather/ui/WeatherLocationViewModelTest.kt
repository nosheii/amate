package no.gruppe7.amate.feature.weather.ui

import androidx.lifecycle.ViewModelStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import no.gruppe7.amate.feature.weather.data.WeatherForecast
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import no.gruppe7.amate.feature.weather.data.WeatherRepository
import no.gruppe7.amate.feature.weather.location.LocationDataSource
import no.gruppe7.amate.feature.weather.location.WeatherCoordinates
import no.gruppe7.amate.feature.weather.location.WeatherLocationResult
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherLocationViewModelTest {
    private val store = ViewModelStore()
    private val clock = MutableClock()
    private val weather = FakeWeather()
    private val location = FakeLocation()

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model() = WeatherViewModel(weather, clock = clock, location = location)
        .also { store.put("location-weather", it) }

    @Test fun beforePermissionCheck_doesNotAccessLocationOrFetchTheWrongCity() = runTest {
        model()
        runCurrent()
        assertEquals(0, location.calls)
        assertTrue(weather.places.isEmpty())
    }

    @Test fun noPermission_usesClearlyMarkedOsloWithoutReadingLocation() = runTest {
        val model = model()
        model.onLocationPermission(false)
        runCurrent()
        assertEquals(0, location.calls)
        assertEquals(WeatherPlace.OSLO, weather.places.single())
        assertEquals(WeatherLocationStatus.NEEDS_PERMISSION, model.uiState.value.locationStatus)
        assertFalse(model.uiState.value.place.isCurrentLocation)
    }

    @Test fun grant_usesActualCoordinatesAndPhoneTimezone_notOslo() = runTest {
        val model = model()
        model.onLocationPermission(true)
        assertEquals(WeatherLocationStatus.LOCATING, model.uiState.value.locationStatus)
        runCurrent()
        val place = weather.places.single()
        assertEquals(60.39, place.latitude, 0.0)
        assertEquals(5.32, place.longitude, 0.0)
        assertEquals(ZoneId.systemDefault(), place.zone)
        assertTrue(place.isCurrentLocation)
        assertEquals(place, model.uiState.value.forecast!!.place)
        assertEquals(WeatherLocationStatus.CURRENT, model.uiState.value.locationStatus)
    }

    @Test fun deniedAndPermanentlyDenied_doNotReadPosition_andHaveDifferentActions() = runTest {
        val model = model()
        model.onLocationPermission(false, hasAsked = true, canAskAgain = true)
        runCurrent()
        assertEquals(WeatherLocationStatus.DENIED, model.uiState.value.locationStatus)
        model.onLocationPermission(false, hasAsked = true, canAskAgain = false)
        runCurrent()
        assertEquals(WeatherLocationStatus.SETTINGS_REQUIRED, model.uiState.value.locationStatus)
        assertEquals(0, location.calls)
        assertEquals(WeatherPlace.OSLO, model.uiState.value.forecast!!.place)
    }

    @Test fun locationServicesOff_hasItsOwnMessageAndFallsBackToOslo() = runTest {
        location.result = WeatherLocationResult.ServicesDisabled
        val model = model()
        model.onLocationPermission(true)
        runCurrent()
        assertEquals(WeatherLocationStatus.DISABLED, model.uiState.value.locationStatus)
        assertEquals(WeatherPlace.OSLO, model.uiState.value.forecast!!.place)
        assertFalse(model.uiState.value.loadFailed)
    }

    @Test fun unavailablePosition_doesNotBecomeZeroCoordinatesOrPretendToBeCurrent() = runTest {
        location.result = WeatherLocationResult.Unavailable
        val model = model()
        model.onLocationPermission(true)
        runCurrent()
        assertEquals(WeatherLocationStatus.UNAVAILABLE, model.uiState.value.locationStatus)
        assertEquals(listOf(WeatherPlace.OSLO), weather.places)
    }

    @Test fun noPositionResponse_timesOutAndCancelsTheLocationRequest() = runTest {
        location.waitForever = true
        val model = model()
        model.onLocationPermission(true)
        runCurrent()
        advanceTimeBy(15_001)
        runCurrent()
        assertTrue(location.finished)
        assertEquals(WeatherLocationStatus.UNAVAILABLE, model.uiState.value.locationStatus)
        assertEquals(WeatherPlace.OSLO, model.uiState.value.forecast!!.place)
    }

    @Test fun leavingHome_stopsWaiting_withoutStartingAnotherWeatherCallInBackground() = runTest {
        location.waitForever = true
        val model = model()
        model.onLocationPermission(true)
        runCurrent()
        model.stopLocating()
        runCurrent()
        assertTrue(location.finished)
        assertTrue(weather.places.isEmpty())
        // Når hjem åpnes igjen, kan Android få et nytt forsøk.
        location.waitForever = false
        model.onLocationPermission(true)
        runCurrent()
        assertEquals(2, location.calls)
        assertTrue(model.uiState.value.place.isCurrentLocation)
    }

    @Test fun permissionRemoved_clearsTheOldPositionAndUsesFallback() = runTest {
        val model = model()
        model.onLocationPermission(true)
        runCurrent()
        model.onLocationPermission(false, hasAsked = true, canAskAgain = false)
        runCurrent()
        assertEquals(1, location.calls)
        assertEquals(WeatherLocationStatus.SETTINGS_REQUIRED, model.uiState.value.locationStatus)
        assertEquals(WeatherPlace.OSLO, model.uiState.value.forecast!!.place)
    }

    @Test fun permissionRemovedDuringReading_doesNotCrash() = runTest {
        location.result = WeatherLocationResult.PermissionRequired
        val model = model()
        model.onLocationPermission(true)
        runCurrent()
        assertEquals(WeatherLocationStatus.NEEDS_PERMISSION, model.uiState.value.locationStatus)
        assertEquals(WeatherPlace.OSLO, model.uiState.value.forecast!!.place)
    }

    @Test fun automaticRefresh_isThrottled_andManualRefreshFindsTheNewPlace() = runTest {
        val model = model()
        model.onLocationPermission(true)
        runCurrent()
        repeat(5) { model.onLocationPermission(true) }
        runCurrent()
        assertEquals(1, location.calls)
        location.result = WeatherLocationResult.Found(WeatherCoordinates(63.43, 10.40))
        model.refresh()
        runCurrent()
        assertEquals(2, location.calls)
        assertEquals(63.43, weather.places.last().latitude, 0.0)
        clock.now = clock.now.plusSeconds(15 * 60)
        model.onLocationPermission(true)
        runCurrent()
        assertEquals(3, location.calls)
    }

    @Test fun lateOsloResponse_cannotOverwriteWeatherForTheGrantedPosition() = runTest {
        // Etterligner et nettverkskall som bruker litt tid på å oppdage at det er avbrutt.
        val gate = CompletableDeferred<Unit>()
        weather.osloGate = gate
        val model = model()
        model.onLocationPermission(false)
        runCurrent()
        model.onLocationPermission(true)
        runCurrent()
        assertTrue(model.uiState.value.forecast!!.place.isCurrentLocation)
        gate.complete(Unit)
        runCurrent()
        assertTrue(model.uiState.value.forecast!!.place.isCurrentLocation)
        assertFalse(model.uiState.value.isLoading)
    }

    @Test fun changingPlace_clearsTheOldForecastWhileLocating() = runTest {
        val model = model()
        model.onLocationPermission(false)
        runCurrent()
        assertNotNull(model.uiState.value.forecast)
        location.waitForever = true
        model.onLocationPermission(true)
        runCurrent()
        assertNull(model.uiState.value.forecast)
        assertEquals(WeatherLocationStatus.LOCATING, model.uiState.value.locationStatus)
    }

    @Test fun returningFromLocationSettings_retriesImmediatelyEvenWhileFallbackWeatherIsLoading() = runTest {
        val gate = CompletableDeferred<Unit>()
        weather.osloGate = gate
        location.result = WeatherLocationResult.ServicesDisabled
        val model = model()
        model.onLocationPermission(true)
        runCurrent()
        assertTrue(model.uiState.value.isLoading)
        assertEquals(WeatherLocationStatus.DISABLED, model.uiState.value.locationStatus)
        location.result = WeatherLocationResult.Found(WeatherCoordinates(60.39, 5.32))
        model.requestCurrentLocation()
        runCurrent()
        assertTrue(model.uiState.value.forecast!!.place.isCurrentLocation)
        gate.complete(Unit)
        runCurrent()
        assertTrue(model.uiState.value.forecast!!.place.isCurrentLocation)
    }

    @Test fun invalidCoordinates_areRejectedButRealZeroCoordinatesAreAllowed() {
        assertEquals(0.0, WeatherCoordinates(0.0, 0.0).latitude, 0.0)
        listOf(Double.NaN, Double.POSITIVE_INFINITY, 91.0).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { WeatherCoordinates(invalid, 0.0) }
        }
        assertThrows(IllegalArgumentException::class.java) { WeatherCoordinates(0.0, -181.0) }
    }

    private class FakeLocation : LocationDataSource {
        var calls = 0
        var waitForever = false
        var finished = false
        var result: WeatherLocationResult = WeatherLocationResult.Found(WeatherCoordinates(60.39, 5.32))
        override suspend fun getCurrentLocation(): WeatherLocationResult {
            calls++
            try {
                if (waitForever) awaitCancellation()
                return result
            } finally { finished = true }
        }
    }

    private class FakeWeather : WeatherRepository {
        val places = mutableListOf<WeatherPlace>()
        var osloGate: CompletableDeferred<Unit>? = null
        override suspend fun getWeather(place: WeatherPlace): WeatherForecast {
            places += place
            if (place == WeatherPlace.OSLO) osloGate?.let { withContext(NonCancellable) { it.await() } }
            return WeatherForecast(place, 6.5, "cloudy", Instant.parse("2026-10-27T12:00:00Z"),
                Instant.parse("2026-10-27T11:00:00Z"))
        }
    }

    private class MutableClock(var now: Instant = Instant.parse("2026-10-27T12:00:00Z")) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }
}
