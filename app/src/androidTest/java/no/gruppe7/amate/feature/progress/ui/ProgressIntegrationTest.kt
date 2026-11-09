package no.gruppe7.amate.feature.progress.ui

import android.content.Context
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.home.ui.HomeShellTheme
import no.gruppe7.amate.feature.water.data.RoomWaterRepository
import no.gruppe7.amate.feature.steps.data.RoomStepsRepository
import no.gruppe7.amate.feature.steps.sensor.StepSensorDataSource
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private var database: AmateDatabase? = null
    @After fun tearDown() { database?.close() }

    private fun waitForText(tag: String, expected: String) {
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag(tag).assertTextEquals(expected) }.isSuccess
        }
    }

    @Test fun roomStepsAndStepsGoalUpdateWithoutChangingWaterGoal() = runBlocking<Unit> {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AmateDatabase::class.java).build()
        database = db
        val water = RoomWaterRepository(db.waterDao())
        val goals = RoomGoalRepository(db.goalDao())
        // Ekte Room og repository, men ingen fysisk sensor i testen.
        val sensor = object : StepSensorDataSource {
            override val stepEvents = emptyFlow<Int>()
            override val isSensorAvailable = false
        }
        val steps = RoomStepsRepository(db.stepsDao(), sensor)
        val today = LocalDate.now().toEpochDay()
        steps.incrementStep(1, 500, today)
        steps.incrementStep(1, 700, today - 1)
        steps.incrementStep(2, 900, today)
        goals.saveGoal(1, GoalType.WATER, 3000)
        goals.saveGoal(1, GoalType.STEPS, 1000)
        compose.setContent { HomeShellTheme { ProgressRoute(water, goals, {}, {}, stepsRepository = steps) } }
        waitForText("progress_steps_today_total", "500 skritt")
        waitForText("progress_steps_percent", "50 % av dagens mål")
        goals.saveGoal(1, GoalType.STEPS, 2000)
        waitForText("progress_steps_percent", "25 % av dagens mål")
        steps.incrementStep(1, 500, today)
        waitForText("progress_steps_percent", "50 % av dagens mål")
        org.junit.Assert.assertEquals(3000, goals.observeGoal(1, GoalType.WATER).first()?.targetValue)
        goals.deleteGoal(goals.observeGoal(1, GoalType.STEPS).first()!!)
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("progress_steps_missing_goal").assertExists() }.isSuccess
        }
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_steps_week_total"))
        val total = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("nb-NO")).format(1700)
        compose.onNodeWithTag("progress_steps_week_total").assertTextEquals("Totalt: $total skritt")
    }

    @Test fun roomEditsAndGoalChangesUpdateProgressWithoutASecondDatabase() = runBlocking<Unit> {
        // Egen database i minnet. Denne testen skal aldri skrive i emulatorens vanlige vannlogg.
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AmateDatabase::class.java).build()
        database = db
        val water = RoomWaterRepository(db.waterDao())
        val goals = RoomGoalRepository(db.goalDao())
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val timeToday = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val yesterday = today.minusDays(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val todayId = water.addWater(1, 500, timeToday)
        water.addWater(1, 750, yesterday)
        // En annen brukers registrering skal ikke telle i denne fremgangen.
        water.addWater(2, 900, timeToday)
        goals.saveGoal(1, GoalType.WATER, 2000)
        compose.setContent { HomeShellTheme { ProgressRoute(water, goals, {}, {}) } }
        waitForText("progress_today_total", "500 ml")
        waitForText("progress_today_percent", "25 % av dagens mål")

        val entry = water.observeEntries(1).first().first { it.id == todayId }
        water.update(entry.copy(amountMl = 1000))
        waitForText("progress_today_percent", "50 % av dagens mål")
        goals.saveGoal(1, GoalType.WATER, 1000)
        waitForText("progress_today_percent", "100 % av dagens mål")
        water.delete(entry)
        waitForText("progress_today_total", "0 ml")
        waitForText("progress_today_percent", "0 % av dagens mål")
        goals.deleteGoal(goals.observeGoal(1, GoalType.WATER).first()!!)
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("progress_missing_goal").assertExists() }.isSuccess
        }
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_week_total"))
        compose.onNodeWithTag("progress_week_total").assertTextEquals("Totalt: 750 ml")
    }
}
