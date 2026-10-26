package no.gruppe7.amate.feature.home.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.core.navigation.AmateShell
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Before
    fun setUp() {
        // Testinnhold i stedet for ekte Route-er: hjemskallet skal ikke trenge Room.
        compose.setContent {
            AmateShell(
                waterContent = { Text("Vannside for test") },
                settingsContent = { Text("Påminnelser for test") },
            )
        }
    }

    @Test
    fun homeShowsPlaceholdersInsteadOfPretendUserData() {
        compose.onNodeWithText("Hei, fint å se deg!").assertIsDisplayed()
        compose.onNodeWithText("Vannkortet viser dagens registreringer. De andre kortene kommer senere.").assertIsDisplayed()
        compose.onNodeWithTag("home_tab_home").assertIsSelected()
    }

    @Test
    fun waterShortcutOpensWaterAndBackReturnsHome() {
        // Skroller i listen, så knappen kan finnes også på små emulatorer.
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_open_water"))
        compose.onNodeWithTag("home_open_water").performClick()
        compose.onNodeWithText("Vannside for test").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tilbake til hjem").performClick()
        compose.onNodeWithTag("home_settings").assertIsDisplayed()
    }

    @Test
    fun settingsIconOpensSeparateSettingsPage() {
        compose.onNodeWithTag("home_settings").performClick()
        compose.onNodeWithText("Innstillinger").assertIsDisplayed()
        compose.onNodeWithText("Påminnelser for test").assertIsDisplayed()
        compose.onNodeWithText("Vannside for test").assertDoesNotExist()
    }

    @Test
    fun bottomTabsOpenPlaceholderPages() {
        compose.onNodeWithTag("home_tab_progress").performClick()
        compose.onNodeWithTag("home_tab_progress").assertIsSelected()
        compose.onNodeWithText("Her kommer det mer etter hvert. Foreløpig er dette bare et skall, uten personlige data.")
            .assertIsDisplayed()
        compose.onNodeWithTag("home_tab_profile").performClick()
        compose.onNodeWithTag("home_tab_profile").assertIsSelected()
        compose.onNodeWithTag("home_tab_home").performClick()
        compose.onNodeWithTag("home_settings").assertIsDisplayed()
    }
}
