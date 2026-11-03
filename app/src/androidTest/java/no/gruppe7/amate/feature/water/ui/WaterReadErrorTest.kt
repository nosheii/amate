package no.gruppe7.amate.feature.water.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaterReadErrorTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun readErrorStaysInListEvenWhenEditorIsOpen() {
        // En lesefeil må ikke forsvinne bare fordi vi åpnet feltet før feilen oppstod.
        compose.setContent {
            WaterScreen(
                state = WaterUiState(isLoading = false, isGoalLoading = false, loadFailed = true,
                    isEditorOpen = true, errorMessageResId = R.string.water_load_error),
                onQuickAdd = {}, onAddCustom = {}, onEdit = {}, onDelete = {}, onRetry = {},
                onDismissError = {}, onAmountChange = {}, onSaveEdit = {}, onCancelEdit = {},
            )
        }
        compose.onNodeWithTag("water_list").performScrollToNode(hasTestTag("water_list_error"))
        compose.onNodeWithTag("water_list_error").assertIsDisplayed()
        compose.onAllNodesWithText("Kunne ikke hente vannregistreringene. Prøv igjen.").assertCountEquals(1)
        compose.onNodeWithTag("water_list").performScrollToNode(hasTestTag("water_inline_editor"))
        // Editorens feilmelding skal ikke være en ekstra kopi av lesefeilen.
        compose.onAllNodesWithText("Kunne ikke hente vannregistreringene. Prøv igjen.").assertCountEquals(1)
    }

    @Test
    fun quickAddWriteErrorIsStillVisibleWhenNoEditorIsOpen() {
        compose.setContent {
            WaterScreen(
                state = WaterUiState(isLoading = false, isGoalLoading = false,
                    errorMessageResId = R.string.water_save_error),
                onQuickAdd = {}, onAddCustom = {}, onEdit = {}, onDelete = {}, onRetry = {},
                onDismissError = {}, onAmountChange = {}, onSaveEdit = {}, onCancelEdit = {},
            )
        }
        compose.onNodeWithTag("water_list").performScrollToNode(hasTestTag("water_list_error"))
        compose.onNodeWithText("Endringen ble ikke lagret. Prøv igjen.").assertIsDisplayed()
        compose.onAllNodesWithText("Endringen ble ikke lagret. Prøv igjen.").assertCountEquals(1)
    }
}
