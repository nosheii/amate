package no.gruppe7.amate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import no.gruppe7.amate.feature.water.ui.WaterRoute
import no.gruppe7.amate.ui.theme.AmateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmateTheme {
                // Midlertidig startside for vann. Felles navigasjon kobles på senere.
                val repository = (application as AmateApplication).container.waterRepository
                WaterRoute(repository = repository)
            }
        }
    }
}
