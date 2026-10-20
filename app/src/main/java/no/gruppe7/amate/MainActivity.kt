package no.gruppe7.amate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import no.gruppe7.amate.feature.water.ui.WaterRoute
import no.gruppe7.amate.feature.reminders.ui.ReminderSettingsRoute
import no.gruppe7.amate.ui.theme.AmateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AmateTheme {
                // Midlertidig startside for vann. Felles navigasjon kobles på senere.
                val container = (application as AmateApplication).container
                // Vannskjermen får sitt repository. Påminnelsesfeltet sendes inn som en egen UI-del.
                // Dermed kan vi senere flytte innstillingene uten å bygge om vannregistreringen.
                WaterRoute(
                    repository = container.waterRepository,
                    reminderSettings = { ReminderSettingsRoute(container.reminderRepository) },
                )
            }
        }
    }
}
