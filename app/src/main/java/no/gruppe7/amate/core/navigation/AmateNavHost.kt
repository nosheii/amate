package no.gruppe7.amate.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import no.gruppe7.amate.feature.home.ui.HomeScreen

/** TODO: Bytt plassholderen med NavHost og registrer alle destinasjonene. */
@Composable
fun AmateNavHost(modifier: Modifier = Modifier) {
    HomeScreen(modifier = modifier)
}
