package no.gruppe7.amate.feature.weather.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import no.gruppe7.amate.feature.weather.data.remote.WeatherApiService
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkWeatherRepositoryTest {
    @Test fun deletion_signalsRetainedModels_andWaitsForCacheCleanup() = runTest {
        val cleared = CompletableDeferred<Unit>()
        var clearing = false
        val api = object : WeatherApiService {
            override suspend fun getWeather(place: WeatherPlace): WeatherForecast = error("Unused")
            override suspend fun clearCache() {
                clearing = true
                cleared.await()
            }
        }
        val repository = NetworkWeatherRepository(api)
        val reset = async { repository.resets.first { it > 0 } }
        val deletion = async { repository.clearUserData() }
        runCurrent()
        assertEquals(1, reset.await())
        assertTrue(clearing)
        assertFalse(deletion.isCompleted)
        cleared.complete(Unit)
        deletion.await()
    }
}
