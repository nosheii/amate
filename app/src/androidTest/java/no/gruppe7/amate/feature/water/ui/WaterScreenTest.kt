package no.gruppe7.amate.feature.water.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.water.data.RoomWaterRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaterScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AmateDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        val repository = RoomWaterRepository(database.waterDao())
        // Bruker den ekte UI → ViewModel → Repository → Room-kjeden, med en testdatabase.
        compose.setContent { WaterRoute(repository) }
        waitForTotal("0 ml")
    }

    @After
    fun tearDown() { database.close() }

    private fun waitForTotal(text: String) {
        compose.onNodeWithTag("water_list").performScrollToNode(hasTestTag("water_total"))
        compose.waitUntil(5_000) {
            runCatching { compose.onNodeWithTag("water_total").assertTextEquals(text) }.isSuccess
        }
    }

    @Test
    fun addEditAndDelete_updatesRoomAndMeter() {
        compose.onNodeWithText("+250 ml").performClick()
        waitForTotal("250 ml")
        compose.onNodeWithContentDescription("Rediger registrering på 250 ml").performScrollTo().performClick()
        compose.onNodeWithTag("water_inline_editor").performScrollTo().assertIsDisplayed()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("500")
        compose.onNodeWithText("Lagre").performScrollTo().performClick()
        waitForTotal("500 ml")
        compose.onNodeWithContentDescription("Slett registrering på 500 ml").performScrollTo().performClick()
        compose.onNodeWithText("Slette registreringen?").assertIsDisplayed()
        compose.onNode(hasText("Slett") and hasAnyAncestor(isDialog())).performClick()
        waitForTotal("0 ml")
        compose.onNodeWithText("En ny start").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun inlineEdit_validatesInputAndCancelKeepsOriginalAmount() {
        compose.onNodeWithText("+250 ml").performClick()
        waitForTotal("250 ml")
        compose.onNodeWithContentDescription("Rediger registrering på 250 ml").performScrollTo().performClick()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText("Lagre").performScrollTo().performClick()
        compose.onNodeWithText("Skriv et helt antall mellom 1 og 10 000 ml.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Avbryt").performScrollTo().performClick()
        compose.onNodeWithTag("water_inline_editor").assertDoesNotExist()
        waitForTotal("250 ml")
    }

    @Test
    fun invalidCustomAmount_showsErrorAndCanBeCorrected() {
        compose.onNodeWithText("Legg til egen mengde").performScrollTo().performClick()
        compose.onNodeWithTag("water_inline_editor").performScrollTo().assertIsDisplayed()
        compose.onNode(isDialog()).assertDoesNotExist()
        // Avbryt skal lukke feltet uten å legge til en registrering.
        compose.onNodeWithText("Avbryt").performScrollTo().performClick()
        waitForTotal("0 ml")
        compose.onNodeWithText("Legg til egen mengde").performScrollTo().performClick()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText("Lagre").performScrollTo().performClick()
        compose.onNodeWithText("Skriv et helt antall mellom 1 og 10 000 ml.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("375")
        compose.onNodeWithText("Lagre").performScrollTo().performClick()
        waitForTotal("375 ml")
    }
}
