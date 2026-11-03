package no.gruppe7.amate.feature.home.ui

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.R
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.core.navigation.AmateShell
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.water.data.RoomWaterRepository
import no.gruppe7.amate.feature.water.ui.WaterRoute
import no.gruppe7.amate.feature.water.ui.rememberWaterViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeWaterIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AmateDatabase
    private lateinit var goals: RoomGoalRepository
    private val numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag("nb-NO"))

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        goals = RoomGoalRepository(database.goalDao())
        val water = RoomWaterRepository(database.waterDao())
        compose.setContent {
            // Akkurat samme kobling som i MainActivity, men med en egen midlertidig Room-database.
            val model = rememberWaterViewModel(water, goals)
            val state by model.uiState.collectAsStateWithLifecycle()
            AmateShell(
                waterState = state, onRetryWater = model::retry,
                waterContent = { WaterRoute(water, goals) }, settingsContent = { Text("Testinnstillinger") },
            )
        }
    }

    @After
    fun tearDown() { database.close() }

    private fun homeCard() {
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_open_water"))
    }

    private fun waitForText(tag: String, text: String) {
        if (tag == "water_total" || tag == "water_goal") {
            compose.onNodeWithTag("water_list").performScrollToNode(hasTestTag(tag))
        }
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag(tag).assertTextEquals(text) }.isSuccess
        }
    }

    private fun expectedGoal(value: Int): String = ApplicationProvider.getApplicationContext<Context>()
        .getString(R.string.water_goal_value, numbers.format(value))

    @Test
    fun addEditAndDeleteAreReflectedOnHomeWithoutAnotherDatabaseCopy() {
        homeCard()
        waitForText("home_water_total", "0 ml")
        compose.onNodeWithTag("home_open_water").performClick()
        compose.onNodeWithText("+250 ml").performClick()
        waitForText("water_total", "250 ml")
        compose.onNodeWithContentDescription("Tilbake til hjem").performClick()
        homeCard()
        waitForText("home_water_total", "250 ml")

        compose.onNodeWithTag("home_open_water").performClick()
        compose.onNodeWithContentDescription("Rediger registrering på 250 ml").performScrollTo().performClick()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("500")
        compose.onNodeWithText("Lagre").performScrollTo()
        compose.onNodeWithText("Lagre").performSemanticsAction(SemanticsActions.OnClick) { it() }
        waitForText("water_total", "500 ml")
        compose.onNodeWithContentDescription("Tilbake til hjem").performClick()
        homeCard()
        waitForText("home_water_total", "500 ml")

        compose.onNodeWithTag("home_open_water").performClick()
        compose.onNodeWithContentDescription("Slett registrering på 500 ml").performScrollTo().performClick()
        compose.onNode(hasText("Slett") and hasAnyAncestor(isDialog())).performClick()
        waitForText("water_total", "0 ml")
        compose.onNodeWithContentDescription("Tilbake til hjem").performClick()
        homeCard()
        waitForText("home_water_total", "0 ml")
    }

    @Test
    fun goalsSavedChangedAndDeletedThroughNajmasRepositoryUpdateBothScreens() {
        homeCard()
        waitForText("home_water_total", "0 ml")
        waitForText("home_goal", "Mål ikke satt")
        compose.onNodeWithTag("home_progress").assertDoesNotExist()
        runBlocking { goals.saveGoal(1, GoalType.WATER, 2000) }
        waitForText("home_goal", expectedGoal(2000))
        compose.onNodeWithTag("home_progress").assertIsDisplayed()

        compose.onNodeWithTag("home_open_water").performClick()
        waitForText("water_goal", expectedGoal(2000))
        compose.onNodeWithText("+250 ml").performClick()
        waitForText("water_total", "250 ml")
        runBlocking { goals.saveGoal(1, GoalType.WATER, 3000) }
        waitForText("water_goal", expectedGoal(3000))
        compose.onNodeWithContentDescription("Tilbake til hjem").performClick()
        homeCard()
        waitForText("home_goal", expectedGoal(3000))
        waitForText("home_water_total", "250 ml")

        runBlocking { goals.deleteGoal(database.goalDao().findGoal(1, GoalType.WATER)!!) }
        waitForText("home_goal", "Mål ikke satt")
        compose.onNodeWithTag("home_progress").assertDoesNotExist()
        compose.onNodeWithTag("home_open_water").performClick()
        waitForText("water_goal", "Mål ikke satt")
        waitForText("water_total", "250 ml")
        compose.onNodeWithTag("water_progress").assertDoesNotExist()
    }
}
