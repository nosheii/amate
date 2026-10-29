package no.gruppe7.amate.feature.weather.data.remote

import java.time.Duration
import java.time.Instant
import no.gruppe7.amate.feature.weather.data.WeatherForecast
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import org.json.JSONObject

internal object MetWeatherParser {
    fun parse(body: String, place: WeatherPlace, now: Instant): WeatherForecast {
        val properties = JSONObject(body).getJSONObject("properties")
        val meta = properties.getJSONObject("meta")
        require(meta.getJSONObject("units").getString("air_temperature") == "celsius")
        val times = properties.getJSONArray("timeseries")
        // API-et gir mange timer. Jeg velger timen nærmest nå, ikke bare første rad.
        // Da kan samme mellomlagrede svar brukes senere uten å vise gårsdagens temperatur.
        val closest = (0 until times.length()).map { times.getJSONObject(it) }
            .minByOrNull { Duration.between(now, Instant.parse(it.getString("time"))).abs() }
            ?: error("No forecast")
        val forecastAt = Instant.parse(closest.getString("time"))
        require(Duration.between(now, forecastAt).abs() <= Duration.ofMinutes(90)) { "Outdated forecast" }
        val data = closest.getJSONObject("data")
        val temperature = data.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature")
        require(temperature.isFinite() && temperature in -100.0..65.0)
        // Et manglende værsymbol betyr ukjent værtype, ikke automatisk sol!
        val symbol = data.optJSONObject("next_1_hours")?.optJSONObject("summary")
            ?.optString("symbol_code")?.takeIf { it.isNotBlank() }
        return WeatherForecast(place, temperature, symbol, forecastAt, Instant.parse(meta.getString("updated_at")))
    }
}
