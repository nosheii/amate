package no.gruppe7.amate.feature.weather.data

import no.gruppe7.amate.feature.weather.data.remote.WeatherApiService

// ViewModel spør her, og slipper å vite noe om HTTP eller JSON.
// I testene bytter vi dette ut med et lite late-som-repository uten internett.
interface WeatherRepository {
    suspend fun getWeather(place: WeatherPlace): WeatherForecast
}

class NetworkWeatherRepository(private val api: WeatherApiService) : WeatherRepository {
    override suspend fun getWeather(place: WeatherPlace) = api.getWeather(place)
}
