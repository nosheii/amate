package no.gruppe7.amate.feature.reminders.ui

interface ReminderSettingsNotifier {
    fun isAllowed(): Boolean
    fun showTestNotification(): Boolean
}
