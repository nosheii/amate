package no.gruppe7.amate.feature.steps.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.home.ui.HomeShellTheme
import no.gruppe7.amate.feature.reminders.data.RoomReminderRepository
import no.gruppe7.amate.feature.reminders.ui.ReminderSettingsRoute
import no.gruppe7.amate.feature.steps.data.RoomStepReminderRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepReminderSettingsIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: AmateDatabase

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AmateDatabase::class.java).build()
        val water = RoomReminderRepository(db.reminderDao())
        val steps = RoomStepReminderRepository(db.stepReminderDao())
        compose.setContent {
            HomeShellTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    ReminderSettingsRoute(water)
                    StepReminderSettingsRoute(steps, stepsToday = 321)
                }
            }
        }
    }
    @After fun cleanup() { db.close() }

    @Test fun savingStepSettingsDoesNotEditWaterModelOrWaterTable() {
        compose.onNodeWithTag("step_reminder_settings_toggle").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("step_reminder_count_4").performScrollTo().performClick()
        compose.onNodeWithTag("step_reminder_start").performScrollTo().performTextReplacement("09:00")
        compose.onNodeWithTag("step_reminder_end").performScrollTo().performTextReplacement("18:00")
        compose.onNodeWithTag("step_reminder_save").performScrollTo().performClick()
        compose.waitUntil(5000) {
            runBlocking { db.stepReminderDao().getSettings(1)?.remindersPerDay == 4 }
        }
        compose.onNodeWithTag("step_reminder_settings_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_settings_toggle").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_start").performScrollTo().assertTextContains("08:00")
        compose.onNodeWithTag("reminder_count_2").performScrollTo().performClick()
        compose.onNodeWithTag("reminder_save").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { db.reminderDao().getSettings(1)?.remindersPerDay == 2 } }
        runBlocking {
            val saved = db.stepReminderDao().getSettings(1)!!
            assertEquals(4, saved.remindersPerDay)
            assertEquals(540, saved.startMinute)
            assertEquals(1080, saved.endMinute)
        }
    }
}
