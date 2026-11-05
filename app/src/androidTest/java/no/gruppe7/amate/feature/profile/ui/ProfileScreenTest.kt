package no.gruppe7.amate.feature.profile.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.ui.theme.AmateTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allFiveAvatarsCanBeSelectedAndShownInPreview() {
        var state by mutableStateOf(ProfileUiState(isLoading = false, avatarKey = "katt"))
        compose.setContent {
            AmateTheme {
                ProfileScreen(state, {}, {}, { state = state.copy(avatarKey = it) }, {})
            }
        }

        // Sjekker både selve valget og at bildet over følger samme lagrede nøkkel.
        listOf("katt" to "Katt", "bjørn" to "Bjørn", "hund" to "Hund", "frosk" to "Frosk", "kanin" to "Kanin")
            .forEach { (key, name) ->
                compose.onNodeWithTag("profile_avatar_$key").performScrollTo().performClick().assertIsSelected()
                compose.onNodeWithTag("profile_avatar_preview").performScrollTo()
                    .assertContentDescriptionEquals("Valgt avatar: $name")
            }
    }

    @Test
    fun oldAvatarKeyShowsFallbackWithoutChangingSavedChoice() {
        var selectionCalls = 0
        compose.setContent {
            AmateTheme {
                ProfileScreen(ProfileUiState(isLoading = false, avatarKey = "elefant"), {}, {}, { selectionCalls++ }, {})
            }
        }

        // Elefant var en gammel plassholder, ikke en av de fem tegningene.
        compose.onNodeWithTag("profile_avatar_preview").assertContentDescriptionEquals("Valgt avatar: Katt")
        compose.onNodeWithTag("profile_avatar_katt").performScrollTo().assertIsSelected()
        compose.runOnIdle { assertEquals(0, selectionCalls) }
    }

    @Test
    fun fieldsAndSaveRemainReachableOnShortScreen() {
        var state by mutableStateOf(ProfileUiState(isLoading = false, avatarKey = "hund"))
        var saved: ProfileUiState? = null
        compose.setContent {
            AmateTheme {
                // En lav testflate fanger opp om avatarene skyver Lagre helt ut av skjermen.
                Box(Modifier.height(400.dp)) {
                    ProfileScreen(
                        uiState = state,
                        onAgeChanged = { state = state.copy(age = it) },
                        onNameChanged = { state = state.copy(displayName = it) },
                        onAvatarSelected = { state = state.copy(avatarKey = it) },
                        onSaveClick = { saved = state },
                    )
                }
            }
        }

        compose.onNodeWithTag("profile_name").performScrollTo().performTextReplacement("Testprofil")
        compose.onNodeWithTag("profile_save").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals("Testprofil", saved?.displayName)
            assertEquals("hund", saved?.avatarKey)
        }
    }
}
