package no.gruppe7.amate.feature.weather.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import no.gruppe7.amate.R

@Composable
fun WeatherCard(state: WeatherUiState = WeatherUiState(), onRefresh: () -> Unit = {}) {
    val uriHandler = LocalUriHandler.current
    // Det viktigste står fremme. Resten kan åpnes i kortet uten en egen side eller popup.
    var detailsOpen by rememberSaveable { mutableStateOf(false) }
    val numbers = remember { NumberFormat.getNumberInstance(Locale.forLanguageTag("nb-NO")).apply {
        maximumFractionDigits = 1
    } }
    val time = remember(state.place) {
        DateTimeFormatter.ofPattern("dd.MM HH:mm", Locale.forLanguageTag("nb-NO")).withZone(state.place.zone)
    }
    val forecast = state.forecast
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("weather_card")) {
        // Sted, værtype og temperatur deler én rad. Oppdatering har en liten knapp helt til høyre.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (forecast != null) {
                val condition = conditionFor(forecast.symbolCode)
                Icon(when (condition) {
                    R.string.weather_clear -> if (forecast.symbolCode?.endsWith("_night") == true) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
                    R.string.weather_rain, R.string.weather_sleet -> Icons.Rounded.WaterDrop
                    R.string.weather_snow -> Icons.Rounded.Grain
                    R.string.weather_thunder -> Icons.Rounded.Thunderstorm
                    R.string.weather_unknown -> Icons.Rounded.CloudOff
                    else -> Icons.Rounded.Cloud
                }, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.weather_title, state.place.name), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                if (forecast != null) Text(stringResource(conditionFor(forecast.symbolCode)),
                    style = MaterialTheme.typography.bodySmall)
            }
            if (forecast != null) {
                Text(stringResource(R.string.weather_temperature, numbers.format(forecast.temperatureCelsius)),
                    style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("weather_temperature"))
            }
            IconButton(onClick = onRefresh, enabled = !state.isLoading, modifier = Modifier.testTag("weather_refresh")) {
                Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.weather_refresh))
            }
        }
        if (forecast != null) {
            // Varseltiden er alltid synlig. Et gammelt varsel skal ikke se ut som en måling akkurat nå.
            Text(stringResource(R.string.weather_forecast_time, time.format(forecast.forecastAt)),
                style = MaterialTheme.typography.bodySmall)
        }
        if (state.isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(Modifier.size(20.dp))
                Text(stringResource(R.string.weather_loading), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (state.loadFailed) {
            Text(stringResource(if (forecast == null) R.string.weather_load_error else R.string.weather_refresh_error),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("weather_error"))
            // Feil har fortsatt en tydelig tekstknapp, ikke bare et ikon man må gjette seg til.
            TextButton(onClick = onRefresh, enabled = !state.isLoading) {
                Text(stringResource(R.string.common_retry))
            }
        }
        // Kilde og lisens er fortsatt tilgjengelige uten å åpne detaljene.
        // Knappene beholder normal trykkflate selv om teksten og kortet er mindre.
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { uriHandler.openUri("https://api.met.no/weatherapi/locationforecast/2.0/documentation") }) {
                Text(stringResource(R.string.weather_source), style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = { uriHandler.openUri("https://creativecommons.org/licenses/by/4.0/") }) {
                Text("CC BY 4.0", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { detailsOpen = !detailsOpen }, modifier = Modifier.testTag("weather_details_toggle")) {
                Icon(if (detailsOpen) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = stringResource(if (detailsOpen) R.string.weather_hide_details else R.string.weather_show_details))
            }
        }
        AnimatedVisibility(visible = detailsOpen) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag("weather_details")) {
                if (forecast != null) Text(stringResource(R.string.weather_updated_time, time.format(forecast.updatedAt)),
                    style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.weather_source_note), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// MET sender korte koder. Vi oversetter bare værtypen, og lover ikke en detaljert timesprognose.
internal fun conditionFor(symbol: String?): Int {
    val code = symbol?.substringBefore('_').orEmpty()
    return when {
        "thunder" in code -> R.string.weather_thunder
        "sleet" in code -> R.string.weather_sleet
        "snow" in code -> R.string.weather_snow
        "rain" in code -> R.string.weather_rain
        code == "clearsky" -> R.string.weather_clear
        code == "fair" || code == "partlycloudy" -> R.string.weather_partly_cloudy
        code == "cloudy" -> R.string.weather_cloudy
        code == "fog" -> R.string.weather_fog
        else -> R.string.weather_unknown
    }
}
