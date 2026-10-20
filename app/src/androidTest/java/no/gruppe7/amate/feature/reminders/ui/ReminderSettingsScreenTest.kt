package no.gruppe7.amate.feature.reminders.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.reminders.data.RoomReminderRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderSettingsScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AmateDatabase
    private lateinit var repository: RoomReminderRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        repository = RoomReminderRepository(database.reminderDao())
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { ReminderSettingsRoute(repository) }
            }
        }
        compose.onNodeWithTag("reminder_settings_toggle").performClick()
        // UI-testene bruker ekte Room, så vi venter på den første listen fra databasen.
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("reminder_count_3").assertIsEnabled().assertIsSelected() }.isSuccess
        }
    }
    @After fun tearDown() { database.close() }

    @Test fun countAndTimesAreSavedAndShownAfterReopeningPanel() {
        compose.onNodeWithTag("reminder_count_4").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_start").performScrollTo().performTextReplacement("09:30")
        compose.onNodeWithTag("reminder_end").performScrollTo().performTextReplacement("20:00")
        compose.onNodeWithTag("reminder_save").performScrollTo().performClick()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("reminder_saved").assertIsDisplayed() }.isSuccess
        }
        val stored = runBlocking { repository.observeSettings(1).first() }
        assertEquals(4, stored.remindersPerDay)
        assertEquals(570, stored.startMinute)
        assertEquals(1200, stored.endMinute)

        compose.onNodeWithTag("reminder_settings_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_settings_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_count_4").performScrollTo().assertIsSelected()
    }

    @Test fun invalidTimeIsRejectedAndCancelDiscardsDraft() {
        compose.onNodeWithTag("reminder_start").performScrollTo().performTextReplacement("22:00")
        compose.onNodeWithTag("reminder_end").performScrollTo().performTextReplacement("07:00")
        compose.onNodeWithTag("reminder_save").performScrollTo().performClick()
        compose.onNodeWithText("Bruk gyldige klokkeslett i TT:MM. Fra må være tidligere enn Til, innenfor samme dag.")
            .performScrollTo().assertIsDisplayed()
        assertNull(runBlocking { database.reminderDao().observeSettings(1).first() })
        compose.onNodeWithText("Avbryt").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_settings_fields").assertDoesNotExist()
        compose.onNodeWithTag("reminder_settings_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_count_3").performScrollTo().assertIsSelected()
    }
}
