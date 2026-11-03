package no.gruppe7.amate.feature.reminders.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.reminders.data.RoomReminderRepository
import no.gruppe7.amate.feature.reminders.scheduling.WaterReminderNotifications
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderTestNotificationTest {
    // Tillatelsen gis før testen, så vi kan sjekke knappen uten en systemdialog i veien.
    @get:Rule val permission: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 33) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else GrantPermissionRule.grant()
    // Gir testen en Compose-skjerm og verktøy til å finne, trykke og sjekke UI-elementer.
    @get:Rule val compose = createComposeRule()

    @Test fun buttonShowsRealNotificationWithoutSavingOrEnablingReminders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        val repository = RoomReminderRepository(database.reminderDao())
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            // Kobler den virkelige Route-en til testens egen database, ikke appens databasefil.
            compose.setContent {
                MaterialTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        ReminderSettingsRoute(repository)
                    }
                }
            }
            compose.onNodeWithTag("reminder_settings_toggle").performClick()
            // Venter på ferdig innlasting i stedet for å gjette hvor rask emulatoren er.
            compose.waitUntil(5_000) {
                runCatching { compose.onNodeWithTag("reminder_count_5").assertIsEnabled() }.isSuccess
            }
            // Et ulagret utkast skal være akkurat som før etter testen.
            compose.onNodeWithTag("reminder_count_5").performScrollTo().performClick()
            compose.onNodeWithTag("reminder_test_notification").performScrollTo().performClick()
            // Meldingen i UI-et alene er ikke bevis nok. Vi sjekker også Androids aktive varsler.
            compose.onNodeWithTag("reminder_test_result").performScrollTo().assertIsDisplayed()
            compose.waitUntil(5_000) {
                manager.activeNotifications.any { it.tag == WaterReminderNotifications.TEST_TAG }
            }
            compose.onNodeWithTag("reminder_enabled").performScrollTo().assertIsOff()
            compose.onNodeWithTag("reminder_count_5").performScrollTo().assertIsSelected()
            runBlocking {
                // Ingen innstillingsrad eller dagslogg skal være opprettet bare fordi vi testet varsler.
                assertNull(database.reminderDao().getSettings(1))
                assertNull(database.reminderDeliveryDao().get(1, LocalDate.now().toString()))
            }
        } finally {
            manager.cancel(WaterReminderNotifications.TEST_TAG, WaterReminderNotifications.TEST_NOTIFICATION_ID)
            database.close()
        }
    }
}
