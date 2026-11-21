package no.gruppe7.amate.feature.steps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.reminders.ui.ReminderSettingsNotifier
import no.gruppe7.amate.feature.reminders.ui.ReminderSettingsRoute
import no.gruppe7.amate.feature.steps.data.StepReminderRepository
import no.gruppe7.amate.feature.steps.data.StepReminderEditorRepository
import no.gruppe7.amate.feature.steps.scheduling.StepReminderNotifications

@Composable
fun StepReminderSettingsRoute(repository: StepReminderRepository, stepsToday: Int = 0) {
    val context = LocalContext.current
    val editor = remember(repository) { StepReminderEditorRepository(repository) }
    val notifications = remember(context) { StepReminderNotifications(context) }
    val actions = remember(notifications, stepsToday) {
        object : ReminderSettingsNotifier {
            override fun isAllowed() = notifications.isAllowed()
            override fun showTestNotification() = notifications.showTestNotification(stepsToday)
        }
    }
    ReminderSettingsRoute(editor, actions, modelKey = "step-reminders",
        titleRes = R.string.steps_reminders_channel_name,
        enabledRes = R.string.steps_reminders_enable,
        deliveryNoteRes = R.string.steps_reminders_delivery_note,
        blockedRes = R.string.steps_reminders_blocked, tagPrefix = "step_reminder")
}
