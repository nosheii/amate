package no.gruppe7.amate.feature.weather.data.remote

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MetWeatherApiTest {
    private val now = Instant.parse("2026-10-27T12:05:00Z")

    @Test fun parser_choosesNearestTimeNotFirstRow() {
        val weather = MetWeatherParser.parse(json(), WeatherPlace.OSLO, now)
        assertEquals(6.5, weather.temperatureCelsius, 0.0)
        assertEquals("cloudy", weather.symbolCode)
        assertEquals(Instant.parse("2026-10-27T12:00:00Z"), weather.forecastAt)
        assertEquals(Instant.parse("2026-10-27T10:00:00Z"), weather.updatedAt)
    }

    @Test fun parser_acceptsMissingSymbolWithoutInventingWeather() {
        val weather = MetWeatherParser.parse(json(symbol = ""), WeatherPlace.OSLO, now)
        assertNull(weather.symbolCode)
        assertEquals(6.5, weather.temperatureCelsius, 0.0)
    }

    @Test fun parser_rejectsMissingTemperatureWrongUnitsAndInvalidJson() {
        for (body in listOf("not json", json().replace("celsius", "fahrenheit"),
            json().replace("air_temperature\":6.5", "missing_temperature\":6.5"))) {
            assertThrows(Exception::class.java) { MetWeatherParser.parse(body, WeatherPlace.OSLO, now) }
        }
    }

    @Test fun parser_rejectsAnOldForecastInsteadOfCallingItCurrent() {
        assertThrows(Exception::class.java) {
            MetWeatherParser.parse(json(), WeatherPlace.OSLO, now.plusSeconds(24 * 3600))
        }
    }

    @Test fun client_identifiesAppUsesHttpsAndClosesConnection() = runBlocking {
        val connection = FakeConnection(200, json())
        var requested: URL? = null
        val api = MetWeatherApiService(clock = Clock.fixed(now, ZoneOffset.UTC), openConnection = {
            requested = it
            connection
        })
        api.getWeather(WeatherPlace.OSLO)
        assertEquals("https", requested!!.protocol)
        assertEquals("lat=59.9139&lon=10.7522", requested!!.query)
        assertTrue(connection.getRequestProperty("User-Agent").contains("github.com/nosheii/amate"))
        assertEquals("application/json", connection.getRequestProperty("Accept"))
        assertEquals(10_000, connection.readTimeout)
        assertTrue(connection.useCaches)
        assertTrue(connection.closed)
    }

    @Test fun client_httpErrorDoesNotParseErrorPageAndStillCloses() = runBlocking {
        val connection = FakeConnection(500, "An HTML error page")
        val api = MetWeatherApiService(openConnection = { connection })
        try {
            api.getWeather(WeatherPlace.OSLO)
            fail("Expected HTTP error")
        } catch (_: IOException) {
            assertTrue(connection.closed)
            assertFalse(connection.bodyOpened)
        }
    }

    @Test fun client_rateLimitPreventsImmediateRetry() = runBlocking {
        var calls = 0
        val api = MetWeatherApiService(clock = Clock.fixed(now, ZoneOffset.UTC), openConnection = {
            calls++
            FakeConnection(429, "", "120")
        })
        repeat(2) {
            try { api.getWeather(WeatherPlace.OSLO); fail("Expected rate limit") } catch (_: IOException) { }
        }
        assertEquals(1, calls)
    }

    // Små svar med samme oppbygning som MET. De kjører uten internett og endrer aldri ekte værdata.
    private fun json(symbol: String = "cloudy") = """
        {"properties":{"meta":{"updated_at":"2026-10-27T10:00:00Z","units":{"air_temperature":"celsius"}},
        "timeseries":[
          {"time":"2026-10-27T09:00:00Z","data":{"instant":{"details":{"air_temperature":2.0}}}},
          {"time":"2026-10-27T12:00:00Z","data":{"instant":{"details":{"air_temperature":6.5}},
            "next_1_hours":{"summary":{"symbol_code":"$symbol"}}}}
        ]}}
    """.trimIndent()

    private class FakeConnection(private val status: Int, private val body: String,
        private val retryAfter: String? = null) : HttpURLConnection(URL("https://example.invalid")) {
        var closed = false
        var bodyOpened = false
        override fun connect() = Unit
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getHeaderField(name: String?) = if (name == "Retry-After") retryAfter else null
        override fun getInputStream(): ByteArrayInputStream {
            bodyOpened = true
            return ByteArrayInputStream(body.toByteArray())
        }
    }
}
