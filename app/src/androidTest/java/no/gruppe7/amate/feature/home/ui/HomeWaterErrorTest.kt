package no.gruppe7.amate.feature.home.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import no.gruppe7.amate.feature.water.ui.WaterUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeWaterErrorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun failedGoalReadIsNotShownAsMissingGoalAndWaterStillHasItsAmount() {
        var retries = 0
        compose.setContent {
            HomeShellTheme {
                HomeScreen(waterState = WaterUiState(isLoading = false, isGoalLoading = false,
                    goalLoadFailed = true, entries = listOf(WaterEntryEntity(1, 1, 250, 0))),
                    onRetryWater = { retries++ })
            }
        }
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_open_water"))
        compose.onNodeWithTag("home_water_total").assertTextEquals("250 ml")
        compose.onNodeWithText("Kunne ikke hente vannmålet. Prøv igjen.").assertIsDisplayed()
        compose.onNodeWithTag("home_goal").assertDoesNotExist()
        compose.onNodeWithTag("home_progress").assertDoesNotExist()
        compose.onNodeWithTag("home_goal_retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun failedWaterReadDoesNotPretendTheUserHasDrunkZero() {
        var retries = 0
        compose.setContent {
            HomeShellTheme {
                HomeScreen(waterState = WaterUiState(isLoading = false, loadFailed = true,
                    isGoalLoading = false, goalMl = 2000), onRetryWater = { retries++ })
            }
        }
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_open_water"))
        compose.onNodeWithTag("home_water_total").assertDoesNotExist()
        compose.onNodeWithTag("home_progress").assertDoesNotExist()
        compose.onNodeWithText("Kunne ikke hente vannregistreringene. Prøv igjen.").assertIsDisplayed()
        compose.onNodeWithTag("home_water_retry").performClick()
        assertEquals(1, retries)
    }
}
