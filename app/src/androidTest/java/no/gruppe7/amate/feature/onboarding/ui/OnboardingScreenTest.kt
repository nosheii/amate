package no.gruppe7.amate.feature.onboarding.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.feature.home.ui.HomeShellTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allFiveAvatarsAndNameCanBeSelected() {
        val current = mutableStateOf(OnboardingUiState(entry = OnboardingEntry.SETUP))
        compose.setContent { HomeShellTheme {
            OnboardingScreen(current.value, {}, {}, {}, {},
                { current.value = current.value.copy(name = it) }, { current.value = current.value.copy(avatarKey = it) }, {}, {})
        } }
        compose.onNodeWithTag("onboarding_name").performTextReplacement("Nora")
        listOf("katt", "bjørn", "hund", "frosk", "kanin").forEach { key ->
            compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("profile_avatar_$key"))
            compose.onNodeWithTag("profile_avatar_$key").performClick().assertIsSelected()
        }
        compose.runOnIdle { assertEquals("Nora", current.value.name); assertEquals("kanin", current.value.avatarKey) }
    }

    @Test fun profileErrorsAreVisibleAndNextIsReachable() {
        var next = 0
        compose.setContent { HomeShellTheme {
            OnboardingScreen(OnboardingUiState(entry = OnboardingEntry.SETUP, nameError = true, avatarError = true),
                { next++ }, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("onboarding_name_error").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("onboarding_avatar_error"))
        compose.onNodeWithTag("onboarding_avatar_error").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("onboarding_next"))
        compose.onNodeWithTag("onboarding_next").performClick()
        compose.runOnIdle { assertEquals(1, next) }
    }

    @Test fun goalsAreOptionalAndSkipIsASeparateAction() {
        var skipped = 0
        compose.setContent { HomeShellTheme {
            OnboardingScreen(OnboardingUiState(entry = OnboardingEntry.SETUP, step = OnboardingStep.GOALS),
                {}, {}, { skipped++ }, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("onboarding_water_goal").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_steps_goal").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("onboarding_skip_goals"))
        compose.onNodeWithTag("onboarding_skip_goals").performClick()
        compose.runOnIdle { assertEquals(1, skipped) }
    }

    @Test fun permissionRefusalDoesNotDisableFinishAndSaveFailureKeepsRetry() {
        var finished = 0
        compose.setContent { HomeShellTheme {
            OnboardingScreen(OnboardingUiState(entry = OnboardingEntry.SETUP, step = OnboardingStep.PERMISSIONS, saveFailed = true),
                { finished++ }, {}, {}, {}, {}, {}, {}, {},
                permissionContent = { Text("Alle tilganger er avslått") })
        } }
        compose.onNodeWithText("Alle tilganger er avslått").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_save_error").assertIsDisplayed()
        compose.onNodeWithTag("onboarding_next").assertTextEquals("Start AMATE").performClick()
        compose.runOnIdle { assertEquals(1, finished) }
    }

    @Test fun savingDisablesNavigationAndStartupErrorHasRetry() {
        val state = mutableStateOf(OnboardingUiState(entry = OnboardingEntry.SETUP, step = OnboardingStep.PERMISSIONS, isSaving = true))
        var retries = 0
        compose.setContent { HomeShellTheme {
            OnboardingScreen(state.value, {}, {}, {}, { retries++ }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("onboarding_next").assertIsNotEnabled()
        compose.onNodeWithTag("onboarding_back").assertIsNotEnabled()
        compose.runOnIdle { state.value = state.value.copy(entry = OnboardingEntry.ERROR) }
        compose.onNodeWithTag("onboarding_retry").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
