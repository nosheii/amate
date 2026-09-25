package com.example.amate.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.amate.feature.home.ui.HomeScreen

/** TODO: Bytt plassholderen med NavHost og registrer alle destinasjonene. */
@Composable
fun AmateNavHost(modifier: Modifier = Modifier) {
    HomeScreen(modifier = modifier)
}
