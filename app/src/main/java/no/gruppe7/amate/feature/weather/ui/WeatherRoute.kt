package no.gruppe7.amate.feature.weather.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import no.gruppe7.amate.feature.weather.data.WeatherRepository
import no.gruppe7.amate.feature.weather.location.LocationDataSource

@Composable
fun WeatherRoute(repository: WeatherRepository, location: LocationDataSource) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    // Route er koblingen mellom datadelen og kortet. Kortet selv kjenner ikke repository-et.
    val factory = remember(repository, location) {
        viewModelFactory { initializer { WeatherViewModel(repository, location = location) } }
    }
    val model: WeatherViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    // Vi husker bare om forrige forespørsel ble avslått, ikke selve posisjonen.
    val preferences = remember(context) {
        context.getSharedPreferences("weather_permission", Context.MODE_PRIVATE)
    }
    var permissionInFlight by rememberSaveable { mutableStateOf(false) }
    var returningFromSettings by rememberSaveable { mutableStateOf(false) }
    val permission = Manifest.permission.ACCESS_COARSE_LOCATION
    fun updatePermission() {
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        val asked = preferences.getBoolean("denied", false)
        // Engangstillatelser kan utløpe. Etter et tidligere ja kan vi spørre på nytt,
        // i stedet for å anta at brukeren har blokkert tillatelsen for alltid.
        if (granted && asked) preferences.edit { putBoolean("denied", false) }
        val canAsk = !asked || activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } == true
        model.onLocationPermission(granted, hasAsked = asked, canAskAgain = canAsk)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        preferences.edit { putBoolean("denied", !granted) }
        permissionInFlight = false
        updatePermission()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        // Tillatelsen kan være endret i Android-innstillingene mens appen var borte.
        // Vi åpner aldri tillatelsesvinduet automatisk ved hvert besøk på hjem.
        if (!permissionInFlight) {
            updatePermission()
            if (returningFromSettings) {
                returningFromSettings = false
                // Ikke vent 15 minutter hvis brukeren nettopp slo på posisjon igjen.
                model.requestCurrentLocation()
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { model.stopLocating() }
    DisposableEffect(model) { onDispose { model.stopLocating() } }

    WeatherCard(state = state, onRefresh = model::refresh, onLocationAction = {
        when (state.locationStatus) {
            WeatherLocationStatus.SETTINGS_REQUIRED -> {
                returningFromSettings = true
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${context.packageName}".toUri()))
            }
            WeatherLocationStatus.DISABLED -> {
                returningFromSettings = true
                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            else -> {
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    // Også etter et mislykket forsøk kan knappen hente en ny posisjon.
                    model.onLocationPermission(true)
                    model.requestCurrentLocation()
                } else {
                    permissionInFlight = true
                    launcher.launch(permission)
                }
            }
        }
    })
}
