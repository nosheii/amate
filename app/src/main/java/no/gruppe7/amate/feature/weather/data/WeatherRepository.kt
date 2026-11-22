package no.gruppe7.amate.feature.weather.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import no.gruppe7.amate.feature.weather.data.remote.WeatherApiService

// ViewModel spør her, og slipper å vite noe om HTTP eller JSON.
// I testene bytter vi dette ut med et lite late-som-repository uten internett.
interface WeatherRepository {
    val resets: Flow<Int> get() = emptyFlow()
    suspend fun getWeather(place: WeatherPlace): WeatherForecast
}

class NetworkWeatherRepository(private val api: WeatherApiService) : WeatherRepository {
    private val resetVersion = MutableStateFlow(0)
    override val resets: Flow<Int> = resetVersion

    suspend fun clearUserData() {
        resetVersion.value += 1
        api.clearCache()
    }

    override suspend fun getWeather(place: WeatherPlace) = api.getWeather(place)
}
