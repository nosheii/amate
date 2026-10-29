package no.gruppe7.amate.feature.weather.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import no.gruppe7.amate.feature.weather.data.WeatherRepository

@Composable
fun WeatherRoute(repository: WeatherRepository) {
    // Route er koblingen mellom datadelen og kortet. Kortet selv kjenner ikke repository-et.
    val factory = remember(repository) { viewModelFactory { initializer { WeatherViewModel(repository) } } }
    val model: WeatherViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshIfNeeded() }
    WeatherCard(state = state, onRefresh = model::refresh)
}
