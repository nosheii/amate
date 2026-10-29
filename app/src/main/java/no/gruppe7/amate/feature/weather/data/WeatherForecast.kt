package no.gruppe7.amate.feature.weather.data

import java.time.Instant
import java.time.ZoneId

// Første runde bruker ett fast sted. Vi spør ikke om telefonens posisjon.
// Når stedvelgeren kommer, kan vi sende inn et annet sted gjennom samme kobling.
data class WeatherPlace(val name: String, val latitude: Double, val longitude: Double, val zone: ZoneId) {
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
