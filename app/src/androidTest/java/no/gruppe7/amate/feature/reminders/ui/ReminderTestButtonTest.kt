package no.gruppe7.amate.feature.reminders.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import no.gruppe7.amate.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderTestButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test fun blockedTestShowsExplanationAndLinkWithoutEditingSettings() {
        // Tester bare visningen med en simulert blokkering, ikke Androids faktiske tillatelsesdialog.
        // Tellerne under avslører hvis testknappen ved en feil prøver å redigere eller lagre valg.
        val result = mutableStateOf<Int?>(null)
        var edits = 0
        var openedSettings = 0
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ReminderSettingsScreen(
                        state = RemindersUiState(isLoading = false, notificationsAllowed = false),
                        expanded = true, onToggleExpanded = {}, onEnabledChange = { edits++ },
                        onCountChange = { edits++ }, onStartChange = { edits++ }, onEndChange = { edits++ },
                        onSave = { edits++ }, onCancel = {}, onRetry = {},
                        onOpenSystemSettings = { openedSettings++ },
                        onTestNotification = { result.value = R.string.reminders_test_blocked },
                        testMessageResId = result.value,
                    )
                }
            }
        }
        // performScrollTo finner knappen selv om den ligger utenfor det synlige området.
        compose.onNodeWithTag("reminder_test_notification").performScrollTo().performClick()
        compose.onNodeWithText("Testvarselet ble ikke sendt fordi varsler er blokkert. Åpne varselinnstillingene og tillat vannpåminnelser.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Åpne varselinnstillinger").performScrollTo().performClick()
        assertEquals(1, openedSettings)
        assertEquals(0, edits)
    }
}
