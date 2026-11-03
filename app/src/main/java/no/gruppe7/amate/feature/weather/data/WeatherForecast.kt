package no.gruppe7.amate.feature.weather.data

import java.time.Instant
import java.time.ZoneId

// Oslo er et tydelig merket standardsted hvis brukeren ikke kan/vil dele posisjon.
// Vi lager ingen Room-tabell med posisjonshistorikk. Værklienten har vanlig HTTP-cache.
data class WeatherPlace(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val zone: ZoneId,
    val isCurrentLocation: Boolean = false,
) {
    companion object {
        val OSLO = WeatherPlace("Oslo", 59.9139, 10.7522, ZoneId.of("Europe/Oslo"))
    }
}

// Dette er et VARSEL for et tidspunkt, ikke en temperatur målt av telefonen.
data class WeatherForecast(
    val place: WeatherPlace,
    val temperatureCelsius: Double,
    val symbolCode: String?,
    val forecastAt: Instant,
    val updatedAt: Instant,
)
