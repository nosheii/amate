package no.gruppe7.amate.feature.weather.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import org.json.JSONObject

// Stedsoppslag skjer bare ved et bevisst søk. Telefonens posisjon sendes ikke hit.
class WeatherPlaceSearch {
    suspend fun search(query: String): List<WeatherPlace> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim().take(100), "UTF-8")
        val connection = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=nb&format=json")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != 200) throw IOException("Place search failed")
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            ensureActive()
            val results = JSONObject(json).optJSONArray("results")
            buildList {
                if (results != null) for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    // Ufullstendige treff kan ikke brukes som et pålitelig værsted.
                    val place = runCatching {
                        val lat = item.getDouble("latitude")
                        val lon = item.getDouble("longitude")
                        require(lat.isFinite() && lat in -90.0..90.0 && lon.isFinite() && lon in -180.0..180.0)
                        val label = listOf(item.getString("name"), item.optString("admin1"), item.optString("country"))
                            .filter { it.isNotBlank() }.distinct().joinToString(", ")
                        WeatherPlace(label, lat, lon, ZoneId.of(item.getString("timezone")))
                    }.getOrNull()
                    if (place != null) add(place)
                }
            }
        } finally { connection.disconnect() }
    }
}
