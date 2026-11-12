package no.gruppe7.amate.feature.onboarding.ui

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.MainActivity
import no.gruppe7.amate.feature.onboarding.OnboardingTestProfile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingActivityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var profile: OnboardingTestProfile

    @Before fun setUp() = runBlocking<Unit> {
        profile = OnboardingTestProfile(context)
        profile.capture()
        profile.clearProfile()
    }
    @After fun tearDown() = runBlocking<Unit> { scenario?.close(); profile.restore() }

    private fun waitFor(tag: String) = compose.waitUntil(15000) {
        runCatching { compose.onNodeWithTag(tag).assertIsDisplayed() }.isSuccess
    }

    private fun finishWithoutGoalsOrPermissions() {
        waitFor("onboarding_name")
        compose.onNodeWithTag("onboarding_name").performTextReplacement("Nora")
        compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("profile_avatar_kanin"))
        compose.onNodeWithTag("profile_avatar_kanin").performClick()
        compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("onboarding_next"))
        compose.onNodeWithTag("onboarding_next").performClick()
        compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("onboarding_skip_goals"))
        compose.onNodeWithTag("onboarding_skip_goals").performClick()
        // Ingen av de tre tillatelsesknappene trykkes. Fullfør skal likevel fungere.
        compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("onboarding_next"))
        compose.onNodeWithTag("onboarding_next").performClick()
    }

    @Test fun freshUserCanFinishWithoutPermissionsAndRecreationKeepsProfile() = runBlocking<Unit> {
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
        finishWithoutGoalsOrPermissions()
        waitFor("home_settings")
        assertEquals("Nora", profile.container.profileRepository.observeProfile(1).first()?.displayName)
        assertEquals("kanin", profile.container.profileRepository.observeAvatar(1).first()?.avatarKey)
        scenario!!.recreate()
        waitFor("home_settings")
        compose.onNodeWithTag("onboarding_name").assertDoesNotExist()
    }

    @Test fun waterNotificationWaitsUntilProfileIsReady() {
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_WATER
            putExtra(MainActivity.EXTRA_WATER_REQUEST_ID, "onboarding-water-test")
        })
        waitFor("onboarding_name")
        compose.onNodeWithTag("water_total").assertDoesNotExist()
        finishWithoutGoalsOrPermissions()
        waitFor("water_total")
    }

    @Test fun deletingProfileReturnsToSetupWithoutLeavingTheApp() = runBlocking<Unit> {
        profile.ready()
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java))
        waitFor("home_settings")
        profile.clearProfile()
        waitFor("onboarding_name")
        compose.onNodeWithTag("onboarding_list").performScrollToNode(hasTestTag("profile_avatar_katt"))
        compose.onNodeWithTag("profile_avatar_katt").assertIsNotSelected()
    }
}
