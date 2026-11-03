package no.gruppe7.amate.feature.weather.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AndroidLocationDataSource(context: Context) : LocationDataSource {
    private val appContext = context.applicationContext
    private val manager = ContextCompat.getSystemService(appContext, LocationManager::class.java)

    @SuppressLint("MissingPermission") // Tillatelsen sjekkes rett under, også om den er trukket tilbake.
    override suspend fun getCurrentLocation(): WeatherLocationResult = withContext(Dispatchers.Main.immediate) {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return@withContext WeatherLocationResult.PermissionRequired
        }
        val locationManager = manager ?: return@withContext WeatherLocationResult.Unavailable
        try {
            if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
                return@withContext WeatherLocationResult.ServicesDisabled
            }
            val networkEnabled = LocationManagerCompat.hasProvider(locationManager, LocationManager.NETWORK_PROVIDER)
                && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            // Fused bruker vanligvis nettverksposisjon med denne tillatelsen. Hvis nettverkskilden
            // er av (f.eks. i emulatoren), prøver nyere Android GPS-kilden i stedet. Android gir
            // oss fortsatt bare omtrentlige koordinater. Vi ber ikke om mer nøyaktig tilgang.
            val providers = weatherLocationProviders(Build.VERSION.SDK_INT, networkEnabled)
            val provider = providers.firstOrNull {
                LocationManagerCompat.hasProvider(locationManager, it) && locationManager.isProviderEnabled(it)
            } ?: return@withContext WeatherLocationResult.Unavailable

            // Vi venter maks 15 sekunder. null betyr at Android ikke fant en fersk posisjon,
            // ikke at brukeren er i Oslo eller på koordinatene 0,0.
            withTimeoutOrNull(15.seconds) {
                suspendCancellableCoroutine { continuation ->
                    val cancellation = CancellationSignal()
                    continuation.invokeOnCancellation { cancellation.cancel() }
                    LocationManagerCompat.getCurrentLocation(locationManager, provider, cancellation,
                        ContextCompat.getMainExecutor(appContext)) { location ->
                        if (continuation.isActive) {
                            val coordinates = location?.let {
                                runCatching { WeatherCoordinates(it.latitude, it.longitude) }.getOrNull()
                            }
                            continuation.resume(coordinates?.let(WeatherLocationResult::Found)
                                ?: WeatherLocationResult.Unavailable)
                        }
                    }
                }
            } ?: WeatherLocationResult.Unavailable
        } catch (_: SecurityException) {
            // Brukeren kan fjerne tillatelsen mens vi venter på et svar.
            WeatherLocationResult.PermissionRequired
        } catch (_: IllegalArgumentException) {
            // En posisjonskilde kan forsvinne eller være utilgjengelig på akkurat denne telefonen.
            WeatherLocationResult.Unavailable
        }
    }
}

// På eldre Android bruker vi nettverksposisjon, ikke en GPS-kilde som kan kreve finere tilgang.
// Den lille valglisten er egen funksjon så vi kan teste telefon/emulator uten ekte sensorer.
internal fun weatherLocationProviders(sdkInt: Int, networkEnabled: Boolean): List<String> = buildList {
    if (sdkInt >= 31 && !networkEnabled) add(LocationManager.GPS_PROVIDER)
    if (sdkInt >= 31) add("fused") // Standardnavnet fra Android 12; ingen ekstra Google-avhengighet.
    add(LocationManager.NETWORK_PROVIDER)
}
