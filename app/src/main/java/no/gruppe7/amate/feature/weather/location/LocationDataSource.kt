package no.gruppe7.amate.feature.weather.location

// Værdelen ber om ett svar, ikke en strøm som følger etter brukeren hele tiden.
// Et eget grensesnitt gjør også at testene slipper å bruke telefonens ekte posisjon.
fun interface LocationDataSource {
    suspend fun getCurrentLocation(): WeatherLocationResult
}

data class WeatherCoordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

// Ulike problemer får ulike beskjeder i kortet. Avslått posisjon er ikke en nettfeil.
sealed interface WeatherLocationResult {
    data class Found(val coordinates: WeatherCoordinates) : WeatherLocationResult
    data object PermissionRequired : WeatherLocationResult
    data object ServicesDisabled : WeatherLocationResult
    data object Unavailable : WeatherLocationResult
}
