package no.gruppe7.amate.feature.home.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.R
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.core.navigation.AmateShell
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.feature.onboarding.data.OnboardingCompletionStore
import no.gruppe7.amate.feature.onboarding.data.OnboardingDraft
import no.gruppe7.amate.feature.onboarding.data.RoomOnboardingRepository
import no.gruppe7.amate.feature.profile.data.RoomProfileRepository
import no.gruppe7.amate.feature.profile.ui.ProfileRoute
import no.gruppe7.amate.feature.profile.ui.ProfileViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeAvatarIntegrationTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "avatar-test-${UUID.randomUUID()}"
    private lateinit var database: AmateDatabase
    private lateinit var profiles: RoomProfileRepository
    private lateinit var onboarding: RoomOnboardingRepository

    @Before fun setUp() {
        database = Room.databaseBuilder(context, AmateDatabase::class.java, databaseName).build()
        profiles = RoomProfileRepository(database.userProfileDao(), database.avatarConfigDao())
        val completion = object : OnboardingCompletionStore {
            val completed = MutableStateFlow(false)
            override fun observeCompleted(userId: Long) = completed
            override suspend fun setCompleted(userId: Long, completed: Boolean) { this.completed.value = completed }
        }
        // Samme lagringsvei som når brukeren fullfører førstegangsoppsettet.
        onboarding = RoomOnboardingRepository(database, profiles, RoomGoalRepository(database.goalDao()), completion)
        runBlocking { onboarding.complete(1L, OnboardingDraft("Testbruker", "kanin")) }
        compose.setContent {
            val avatar by remember { profiles.observeAvatar(1L) }.collectAsStateWithLifecycle(initialValue = null)
            AmateShell(
                avatarKey = avatar?.avatarKey.orEmpty(),
                waterContent = {}, settingsContent = {}, weatherContent = {},
                profileContent = {
                    val model: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(profiles))
                    ProfileRoute(model)
                },
            )
        }
    }

    @After fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun expectAvatar(name: String) {
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("home_avatar")
                .assertContentDescriptionEquals("Valgt avatar: $name") }.isSuccess
        }
        compose.onNodeWithTag("home_avatar").assertIsDisplayed()
    }

    @Test fun onboardingChoiceAppearsOnHome_andProfileSaveUpdatesIt() {
        expectAvatar("Kanin")
        compose.onNodeWithTag("home_tab_profile").performClick()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("profile_avatar_preview")
                .assertContentDescriptionEquals("Valgt avatar: Kanin") }.isSuccess
        }
        compose.onNodeWithTag("profile_avatar_bjørn").performScrollTo().performClick()
        compose.onNodeWithTag("profile_save").performScrollTo().performClick()
        compose.onNodeWithTag("home_tab_home").performClick()
        expectAvatar("Bjørn")
        // Ny repository-instans leser lagret valg, ikke et utkast i ProfileViewModel.
        runBlocking {
            val reloaded = RoomProfileRepository(database.userProfileDao(), database.avatarConfigDao())
            assertEquals("bjørn", reloaded.observeAvatar(1L).first()?.avatarKey)
        }
    }

    @Test fun unsavedProfileChoiceDoesNotReplaceSavedOnboardingAvatar() {
        expectAvatar("Kanin")
        compose.onNodeWithTag("home_tab_profile").performClick()
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("profile_avatar_preview")
                .assertContentDescriptionEquals("Valgt avatar: Kanin") }.isSuccess
        }
        compose.onNodeWithTag("profile_avatar_hund").performScrollTo().performClick()
        compose.onNodeWithTag("home_tab_home").performClick()
        expectAvatar("Kanin")
        runBlocking { assertEquals("kanin", profiles.observeAvatar(1L).first()?.avatarKey) }
    }

    @Test fun invalidProfileNameKeepsOnboardingCompleteAndCanBeCorrected() {
        expectAvatar("Kanin")
        compose.onNodeWithTag("home_tab_profile").performClick()
        val name = compose.onNodeWithTag("profile_name")
        compose.waitUntil(5_000) { runCatching { name.assertTextContains("Testbruker") }.isSuccess }
        val save = compose.onNodeWithTag("profile_save")
        val errorText = context.getString(R.string.profile_name_error)

        for (invalidName in listOf("   ", "a".repeat(41))) {
            name.performScrollTo().performTextReplacement(invalidName)
            save.performScrollTo().performClick()
            compose.onNodeWithText(errorText).assertIsDisplayed()
            runBlocking {
                assertEquals("Testbruker", profiles.observeProfile(1L).first()?.displayName)
                assertTrue(onboarding.observeStatus(1L).first().completed)
            }
        }

        name.performScrollTo().performTextReplacement("  Nytt navn  ")
        save.performScrollTo().performClick()
        val savedLabel = context.getString(R.string.profile_saved)
        compose.waitUntil(5_000) { runCatching { save.assertTextContains(savedLabel) }.isSuccess }
        compose.onNodeWithText(errorText).assertDoesNotExist()
        runBlocking {
            assertEquals("Nytt navn", profiles.observeProfile(1L).first()?.displayName)
            assertTrue(onboarding.observeStatus(1L).first().completed)
        }
    }
}
