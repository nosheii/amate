package no.gruppe7.amate.feature.progress.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import no.gruppe7.amate.feature.home.ui.HomeShellTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressScreenTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.of(2026, 11, 9)

    private fun state(amount: Long = 500, goal: Int? = 2000) = ProgressUiState(
        date = date,
        days = (6L downTo 0L).map { WaterProgressDay(date.minusDays(it), if (it == 0L) amount else 0L) },
        isLoading = false, isGoalLoading = false, goalMl = goal,
    )

    @Test fun showsTodayPercentageAndWeekFromTheSameData() {
        compose.setContent { HomeShellTheme { ProgressScreen(state(), {}, {}, {}) } }
        compose.onNodeWithTag("progress_today_total").assertTextEquals("500 ml")
        compose.onNodeWithTag("progress_today_percent").assertTextEquals("25 % av dagens mål")
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_week_total"))
        compose.onNodeWithTag("progress_history").assertIsDisplayed()
        compose.onNodeWithTag("progress_week_total").assertTextEquals("Totalt: 500 ml")
        compose.onNodeWithTag("progress_week_average").assertTextEquals("Gjennomsnitt: 71 ml per dag")
    }

    @Test fun noGoalStillShowsWaterAndBothShortcutsWork() {
        var openedGoals = 0
        var openedWater = 0
        compose.setContent {
            HomeShellTheme { ProgressScreen(state(goal = null), {}, { openedGoals++ }, { openedWater++ }) }
        }
        compose.onNodeWithTag("progress_today_total").assertTextEquals("500 ml")
        compose.onNodeWithTag("progress_missing_goal").assertIsDisplayed()
        compose.onNodeWithTag("progress_today_meter").assertDoesNotExist()
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_open_goals"))
        compose.onNodeWithTag("progress_open_goals").performClick()
        compose.onNodeWithTag("progress_open_water").performClick()
        compose.runOnIdle { assertEquals(1, openedGoals); assertEquals(1, openedWater) }
    }

    @Test fun waterFailureDoesNotDrawAFalseEmptyWeek() {
        var retries = 0
        compose.setContent {
            HomeShellTheme { ProgressScreen(state().copy(waterLoadFailed = true), { retries++ }, {}, {}) }
        }
        compose.onNodeWithTag("progress_today_total").assertDoesNotExist()
        compose.onNodeWithTag("progress_today_percent").assertDoesNotExist()
        compose.onNodeWithTag("progress_history").assertDoesNotExist()
        compose.onNodeWithTag("progress_empty").assertDoesNotExist()
        compose.onNodeWithTag("progress_water_retry").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun goalFailureIsNotPresentedAsMissingGoal() {
        compose.setContent {
            HomeShellTheme { ProgressScreen(state(goal = null).copy(goalLoadFailed = true), {}, {}, {}) }
        }
        compose.onNodeWithTag("progress_goal_retry").assertIsDisplayed()
        compose.onNodeWithTag("progress_missing_goal").assertDoesNotExist()
        compose.onNodeWithTag("progress_today_percent").assertDoesNotExist()
        compose.onNodeWithTag("progress_today_total").assertTextEquals("500 ml")
    }

    @Test fun emptyWeekHasAnExplanationRatherThanSampleData() {
        compose.setContent { HomeShellTheme { ProgressScreen(state(amount = 0, goal = null), {}, {}, {}) } }
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_empty"))
        compose.onNodeWithTag("progress_empty").assertIsDisplayed()
        compose.onNodeWithTag("progress_week_total").assertTextEquals("Totalt: 0 ml")
    }

    private fun stepsState() = StepsProgressUiState(
        date = date, days = (6L downTo 0L).map { StepsProgressDay(date.minusDays(it), if (it == 0L) 500 else 0) },
        isLoading = false, isGoalLoading = false, goal = 1000,
    )

    @Test fun waterAndStepsAreVisibleTogetherWithoutSwitchingTabs() {
        compose.setContent { HomeShellTheme { ProgressScreen(state().copy(steps = stepsState()), {}, {}, {}) } }
        compose.onNodeWithTag("progress_today_total").assertTextEquals("500 ml")
        compose.onNodeWithTag("progress_steps_today_total").assertTextEquals("500 skritt")
        compose.onNodeWithTag("progress_steps_percent").assertTextEquals("50 % av dagens mål")
        compose.onNodeWithTag("progress_today_percent").assertTextEquals("25 % av dagens mål")
        compose.onNodeWithTag("progress_tab_water").assertDoesNotExist()
        compose.onNodeWithTag("progress_tab_steps").assertDoesNotExist()
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_steps_history"))
        compose.onNodeWithTag("progress_history").assertIsDisplayed()
        compose.onNodeWithTag("progress_steps_history").assertIsDisplayed()
    }

    @Test fun stepsErrorIsNotZeroAndDoesNotHideWater() {
        compose.setContent { HomeShellTheme { ProgressScreen(state().copy(steps = stepsState().copy(loadFailed = true)), {}, {}, {}) } }
        compose.onNodeWithTag("progress_steps_retry").assertIsDisplayed()
        compose.onNodeWithTag("progress_steps_today_total").assertDoesNotExist()
        compose.onNodeWithTag("progress_steps_history").assertDoesNotExist()
        compose.onNodeWithTag("progress_today_total").assertTextEquals("500 ml")
    }

    @Test fun stepsGoalErrorIsDifferentFromMissingGoal() {
        compose.setContent { HomeShellTheme { ProgressScreen(state().copy(steps = stepsState().copy(goal = null, goalLoadFailed = true)), {}, {}, {}) } }
        compose.onNodeWithTag("progress_steps_goal_retry").assertIsDisplayed()
        compose.onNodeWithTag("progress_steps_missing_goal").assertDoesNotExist()
        compose.onNodeWithTag("progress_steps_percent").assertDoesNotExist()
        compose.onNodeWithTag("progress_steps_today_total").assertTextEquals("500 skritt")
    }

    @Test fun stepsWithoutGoalStillHaveBothShortcutsAndHistory() {
        var openedSteps = 0
        var openedGoals = 0
        compose.setContent { HomeShellTheme {
            ProgressScreen(state().copy(steps = stepsState().copy(goal = null)), {}, { openedGoals++ }, {},
                onOpenSteps = { openedSteps++ })
        } }
        compose.onNodeWithTag("progress_steps_missing_goal").assertIsDisplayed()
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_open_steps"))
        compose.onNodeWithTag("progress_open_steps").performClick()
        compose.onNodeWithTag("progress_open_goals").performClick()
        compose.runOnIdle { assertEquals(1, openedSteps); assertEquals(1, openedGoals) }
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_steps_week_total"))
        compose.onNodeWithTag("progress_steps_week_total").assertTextEquals("Totalt: 500 skritt")
    }

    @Test fun overGoalShowsActualPercentageWithoutChangingHistoryMeaning() {
        compose.setContent { HomeShellTheme { ProgressScreen(state(amount = 2500), {}, {}, {}) } }
        compose.onNodeWithTag("progress_today_percent").assertTextEquals("125 % av dagens mål")
        compose.onNodeWithText("Dagens mål er nådd.").assertIsDisplayed()
    }

    @Test fun dashedLinesShowEachCurrentGoalAndUpdateWhenGoalChanges() {
        val current = mutableStateOf(state().copy(steps = stepsState()))
        compose.setContent { HomeShellTheme { ProgressScreen(current.value, {}, {}, {}) } }
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_steps_goal_line"))
        val numbers = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("nb-NO"))
        compose.onNodeWithTag("progress_water_goal_line")
            .assertContentDescriptionEquals("Dagens mål: ${numbers.format(2000)} ml")
        compose.onNodeWithTag("progress_steps_goal_line")
            .assertContentDescriptionEquals("Dagens mål: ${numbers.format(1000)} skritt")
        compose.runOnIdle { current.value = current.value.copy(goalMl = 3000) }
        compose.onNodeWithTag("progress_water_goal_line")
            .assertContentDescriptionEquals("Dagens mål: ${numbers.format(3000)} ml")
        compose.runOnIdle { current.value = current.value.copy(goalMl = null, steps = stepsState().copy(goal = null)) }
        compose.onNodeWithTag("progress_water_goal_line").assertDoesNotExist()
        compose.onNodeWithTag("progress_steps_goal_line").assertDoesNotExist()
    }

    @Test fun staleGoalsDoNotDrawLinesWhileLoadingOrAfterFailure() {
        compose.setContent { HomeShellTheme {
            ProgressScreen(state().copy(isGoalLoading = true,
                steps = stepsState().copy(goalLoadFailed = true)), {}, {}, {})
        } }
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_steps_history"))
        compose.onNodeWithTag("progress_history").assertExists()
        compose.onNodeWithTag("progress_steps_history").assertExists()
        compose.onNodeWithTag("progress_water_goal_line").assertDoesNotExist()
        compose.onNodeWithTag("progress_steps_goal_line").assertDoesNotExist()
    }

    @Test fun emptyWeekStillShowsTheGoalReferenceWhenGoalExists() {
        compose.setContent { HomeShellTheme { ProgressScreen(state(amount = 0), {}, {}, {}) } }
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_water_goal_line"))
        compose.onNodeWithTag("progress_water_goal_line").assertIsDisplayed()
        compose.onNodeWithTag("progress_week_total").assertTextEquals("Totalt: 0 ml")
    }
}
