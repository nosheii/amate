package no.gruppe7.amate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import no.gruppe7.amate.core.navigation.AmateShell
import no.gruppe7.amate.feature.water.ui.WaterRoute
import no.gruppe7.amate.feature.reminders.ui.ReminderSettingsRoute
import no.gruppe7.amate.ui.theme.AmateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmateTheme {
                val container = (application as AmateApplication).container
                // Starter på hjemmeskallet. Forsiden viser foreløpig ingen databasedata.
                // Bare når vi åpner vann eller innstillinger bruker vi de eksisterende Route-ene.
                AmateShell(
                    waterContent = { WaterRoute(repository = container.waterRepository) },
                    settingsContent = { ReminderSettingsRoute(container.reminderRepository) },
                )
            }
        }
    }
}
