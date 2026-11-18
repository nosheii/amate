package no.gruppe7.amate.feature.weather.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import no.gruppe7.amate.feature.weather.data.remote.WeatherPlaceSearch

@Composable
internal fun WeatherPlaceDialog(onDismiss: () -> Unit, onSelect: (WeatherPlace) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherPlace>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val search = remember { WeatherPlaceSearch() }
    var request by remember { mutableStateOf<Job?>(null) }
    val uriHandler = LocalUriHandler.current
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weather_choose_place)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = query, onValueChange = {
                    request?.cancel()
                    query = it.take(100)
                    results = emptyList()
                    message = null
                    loading = false
                }, singleLine = true, label = { Text(stringResource(R.string.weather_place_query)) },
                    modifier = Modifier.fillMaxWidth().testTag("weather_place_query"))
                TextButton(enabled = query.trim().length >= 2 && !loading, onClick = {
                    loading = true
                    results = emptyList()
                    message = null
                    request = scope.launch {
                        try {
                            results = search.search(query)
                            if (results.isEmpty()) message = R.string.weather_place_empty
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            message = R.string.weather_place_error
                        } finally { if (currentCoroutineContext().isActive) loading = false }
                    }
                }, modifier = Modifier.testTag("weather_place_search")) {
                    Text(stringResource(R.string.weather_place_search))
                }
                if (loading) CircularProgressIndicator()
                message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
                results.forEach { place ->
                    TextButton(onClick = { onSelect(place) }, modifier = Modifier.fillMaxWidth()) {
                        Text(place.name)
                    }
                }
                Text(stringResource(R.string.weather_place_notice), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { uriHandler.openUri("https://open-meteo.com/en/docs/geocoding-api") }) {
                    Text(stringResource(R.string.weather_place_source), style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.weather_place_close)) }
        })
}
