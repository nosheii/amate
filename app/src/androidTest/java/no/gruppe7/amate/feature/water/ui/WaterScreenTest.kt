package no.gruppe7.amate.feature.water.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.water.data.RoomWaterRepository
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.feature.water.data.WaterRepository
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaterScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var database: AmateDatabase
    @Volatile private var failWrites = false

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        val roomRepository = RoomWaterRepository(database.waterDao())
        // Bare testens skriver kan feile med vilje. Den ekte appens database røres ikke.
        val repository = object : WaterRepository by roomRepository {
            override suspend fun addWater(userId: Long, amountMl: Int, loggedAtMillis: Long): Long {
                check(!failWrites)
                return roomRepository.addWater(userId, amountMl, loggedAtMillis)
            }
            override suspend fun update(entry: WaterEntryEntity) {
                check(!failWrites)
                roomRepository.update(entry)
            }
            override suspend fun delete(entry: WaterEntryEntity) {
                check(!failWrites)
                roomRepository.delete(entry)
            }
        }
        // Bruker den ekte UI → ViewModel → Repository → Room-kjeden, med en testdatabase.
        compose.setContent { WaterRoute(repository, RoomGoalRepository(database.goalDao())) }
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

    private fun waitForWriteError() {
        // Skrivingen går via en coroutine. Jeg venter på resultatet, ikke et bestemt antall millisekunder.
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Endringen ble ikke lagret. Prøv igjen.").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Endringen ble ikke lagret. Prøv igjen.").performScrollTo().assertIsDisplayed()
    }

    private fun editorButton(text: String) {
        // Feltet glir og skroller automatisk. Jeg utløser knappens Compose-handling direkte,
        // så testen ikke trykker på en gammel posisjon midt i animasjonen.
        compose.onNodeWithText(text).performScrollTo()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    @Test
    fun addEditAndDelete_updatesRoomAndMeter() {
        compose.onNodeWithText("+250 ml").performClick()
        waitForTotal("250 ml")
        compose.onNodeWithContentDescription("Rediger registrering på 250 ml").performScrollTo().performClick()
        compose.onNodeWithTag("water_inline_editor").performScrollTo().assertIsDisplayed()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("500")
        editorButton("Lagre")
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
        editorButton("Lagre")
        compose.onNodeWithText("Skriv et helt antall mellom 1 og 10 000 ml.")
            .performScrollTo().assertIsDisplayed()
        editorButton("Avbryt")
        compose.onNodeWithTag("water_inline_editor").assertDoesNotExist()
        waitForTotal("250 ml")
    }

    @Test
    fun invalidCustomAmount_showsErrorAndCanBeCorrected() {
        compose.onNodeWithText("Legg til egen mengde").performScrollTo().performClick()
        compose.onNodeWithTag("water_inline_editor").performScrollTo().assertIsDisplayed()
        compose.onNode(isDialog()).assertDoesNotExist()
        // Avbryt skal lukke feltet uten å legge til en registrering.
        editorButton("Avbryt")
        waitForTotal("0 ml")
        compose.onNodeWithText("Legg til egen mengde").performScrollTo().performClick()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("0")
        editorButton("Lagre")
        compose.onNodeWithText("Skriv et helt antall mellom 1 og 10 000 ml.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("375")
        editorButton("Lagre")
        waitForTotal("375 ml")
    }

    @Test
    fun editSaveFailureIsOnlyShownInEditorAndRetryWorks() {
        compose.onNodeWithText("+250 ml").performClick()
        waitForTotal("250 ml")
        compose.onNodeWithContentDescription("Rediger registrering på 250 ml").performScrollTo().performClick()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("500")
        failWrites = true
        editorButton("Lagre")
        waitForWriteError()
        compose.onAllNodesWithText("Endringen ble ikke lagret. Prøv igjen.").assertCountEquals(1)
        // Sjekker også toppen av listen. LazyColumn kan ellers skjule en duplikat utenfor skjermen.
        compose.onNodeWithTag("water_list").performScrollToNode(hasTestTag("water_total"))
        compose.onNodeWithTag("water_list_error").assertDoesNotExist()
        failWrites = false
        editorButton("Lagre")
        waitForTotal("500 ml")
    }

    @Test
    fun customSaveFailureIsOnlyShownInEditorAndCancelClearsIt() {
        compose.onNodeWithText("Legg til egen mengde").performScrollTo().performClick()
        compose.onNodeWithTag("water_amount_input").performScrollTo().performTextReplacement("375")
        failWrites = true
        editorButton("Lagre")
        waitForWriteError()
        compose.onAllNodesWithText("Endringen ble ikke lagret. Prøv igjen.").assertCountEquals(1)
        editorButton("Avbryt")
        waitForTotal("0 ml")
        compose.onNodeWithTag("water_list_error").assertDoesNotExist()
    }

    @Test
    fun failedDeleteOnlyShowsErrorInConfirmationDialog() {
        compose.onNodeWithText("+250 ml").performClick()
        waitForTotal("250 ml")
        compose.onNodeWithContentDescription("Slett registrering på 250 ml").performScrollTo().performClick()
        failWrites = true
        compose.onNode(hasText("Slett") and hasAnyAncestor(isDialog())).performClick()
        compose.onNode(hasText("Endringen ble ikke lagret. Prøv igjen.") and hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
        compose.onAllNodesWithText("Endringen ble ikke lagret. Prøv igjen.").assertCountEquals(1)
        compose.onNodeWithTag("water_list_error").assertDoesNotExist()
        compose.onNodeWithText("Avbryt").performClick()
        waitForTotal("250 ml")
        compose.onNodeWithTag("water_list_error").assertDoesNotExist()
    }
}
