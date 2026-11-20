package no.gruppe7.amate.feature.weather.data

import android.content.Context
import androidx.core.content.edit
import java.time.ZoneId

// Bare det aktivt valgte stedet lagres, aldri historikk eller telefonens posisjon.
interface WeatherPlaceStore {
    fun read(): WeatherPlace?
    fun save(place: WeatherPlace?)
}

class LocalWeatherPlaceStore(context: Context) : WeatherPlaceStore {
    private val preferences = context.applicationContext.getSharedPreferences("weather_place", Context.MODE_PRIVATE)

    override fun read(): WeatherPlace? = runCatching {
        val name = preferences.getString("name", null) ?: return null
        val latitude = preferences.getString("latitude", null)!!.toDouble()
        val longitude = preferences.getString("longitude", null)!!.toDouble()
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        WeatherPlace(name, latitude, longitude, ZoneId.of(preferences.getString("zone", null)))
    }.getOrNull()

    override fun save(place: WeatherPlace?) {
        preferences.edit {
            clear()
            if (place != null) {
                putString("name", place.name)
                putString("latitude", place.latitude.toString())
                putString("longitude", place.longitude.toString())
                putString("zone", place.zone.id)
            }
        }
    }
}
