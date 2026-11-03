package no.gruppe7.amate.feature.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.gruppe7.amate.R

@Composable
internal fun WeatherLocationControls(status: WeatherLocationStatus, onAction: () -> Unit) {
    // Kortet forklarer hvorfor vi trenger tilgang FØR Android sitt tillatelsesvindu åpnes.
    // Hvis posisjon mangler, skal det være umulig å forveksle Oslo med brukerens eget sted.
    val message = when (status) {
        WeatherLocationStatus.NEEDS_PERMISSION -> R.string.weather_location_intro
        WeatherLocationStatus.DENIED -> R.string.weather_location_denied
        WeatherLocationStatus.SETTINGS_REQUIRED -> R.string.weather_location_settings_required
        WeatherLocationStatus.LOCATING -> R.string.weather_location_loading
        WeatherLocationStatus.CURRENT -> R.string.weather_location_current
        WeatherLocationStatus.DISABLED -> R.string.weather_location_disabled
        WeatherLocationStatus.UNAVAILABLE -> R.string.weather_location_unavailable
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status == WeatherLocationStatus.LOCATING) CircularProgressIndicator(Modifier.size(20.dp))
        Text(stringResource(message), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag("weather_location_status"))
    }
    val action = when (status) {
        WeatherLocationStatus.CURRENT, WeatherLocationStatus.LOCATING -> null
        WeatherLocationStatus.SETTINGS_REQUIRED -> R.string.weather_location_open_permissions
        WeatherLocationStatus.DISABLED -> R.string.weather_location_enable_services
        WeatherLocationStatus.UNAVAILABLE -> R.string.weather_location_retry
        else -> R.string.weather_location_use_mine
    }
    if (action != null) TextButton(onClick = onAction, modifier = Modifier.testTag("weather_location_action")) {
        Text(stringResource(action))
    }
}
