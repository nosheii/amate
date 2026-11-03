package no.gruppe7.amate.feature.weather.data.remote

import android.util.Log
import android.net.http.HttpResponseCache
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import no.gruppe7.amate.feature.weather.data.WeatherForecast
import no.gruppe7.amate.feature.weather.data.WeatherPlace

interface WeatherApiService {
    suspend fun getWeather(place: WeatherPlace): WeatherForecast
}

// Android har allerede en HTTPS-klient. Vi bruker den for denne lille første versjonen.
// IO betyr at venting på internett ikke blokkerer knappene og tegningen av skjermen.
class MetWeatherApiService(
    private val cacheDirectory: File? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection },
) : WeatherApiService {
    private var retryAfter: Instant = Instant.MIN

    override suspend fun getWeather(place: WeatherPlace): WeatherForecast = withContext(dispatcher) {
        if (clock.instant().isBefore(retryAfter)) throw IOException("Weather service cooldown")
        // Android følger Expires og gjenbruker Last-Modified til betingede forespørsler.
        // Dette skjer på IO. Full cache eller manglende diskplass skal ikke krasje skjermen.
        if (cacheDirectory != null) runCatching {
            if (HttpResponseCache.getInstalled() == null) HttpResponseCache.install(cacheDirectory, 5L * 1024 * 1024)
        }
        require(place.latitude.isFinite() && place.latitude in -90.0..90.0)
        require(place.longitude.isFinite() && place.longitude in -180.0..180.0)
        // MET ber om maks fire desimaler. US hindrer norsk desimalkomma i nettadressen.
        val coordinates = String.format(Locale.US, "lat=%.4f&lon=%.4f", place.latitude, place.longitude)
        val connection = openConnection(URL("https://api.met.no/weatherapi/locationforecast/2.0/compact?$coordinates"))
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.instanceFollowRedirects = true
            connection.useCaches = true
            connection.setRequestProperty("Accept", "application/json")
            // MET må vite hvilken app som spør. Prosjektlenken er identifikasjon, ikke en API-nøkkel.
            connection.setRequestProperty("User-Agent", "Amate-MOB3000/1.0 github.com/nosheii/amate")
            // Vi setter ikke Accept-Encoding selv: Android håndterer gzip automatisk.
            val status = connection.responseCode
            if (status == 429 || status == 503) {
                // En ivrig retry-knapp skal heller ikke mase på en overbelastet tjeneste.
                val header = connection.getHeaderField("Retry-After")
                val until = header?.toLongOrNull()?.coerceIn(60, 86_400)?.let { clock.instant().plusSeconds(it) }
                    ?: runCatching { ZonedDateTime.parse(header, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }.getOrNull()
                retryAfter = maxOf(until ?: Instant.MIN, clock.instant().plusSeconds(60))
            }
            if (status !in 200..299) throw IOException("Weather HTTP $status")
            if (status == 203) Log.w("AmateWeather", "MET varsler at API-versjonen må oppdateres")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (cacheDirectory != null) runCatching { HttpResponseCache.getInstalled()?.flush() }
            ensureActive() // En lukket ViewModel skal ikke få et gammelt nettverkssvar tilbake.
            MetWeatherParser.parse(body, place, clock.instant())
        } finally {
            // Både feil og vellykkede forespørsler slipper nettverksressursene etterpå.
            connection.disconnect()
        }
    }
}
