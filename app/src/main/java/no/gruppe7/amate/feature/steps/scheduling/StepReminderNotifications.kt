package no.gruppe7.amate.feature.steps.scheduling

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import no.gruppe7.amate.MainActivity
import no.gruppe7.amate.R
import java.util.UUID

class StepReminderNotifications(context: Context) : StepReminderNotifier, no.gruppe7.amate.feature.reminders.ui.ReminderSettingsNotifier {
    private val context = context.applicationContext
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID,
            context.getString(R.string.steps_reminders_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = context.getString(R.string.steps_reminders_channel_description)
        })
    }

    override fun isAllowed() = notificationsAllowed(context)

    override fun show(userId: Long, stepsToday: Int) {
        post(tag(userId), NOTIFICATION_ID, context.getString(R.string.steps_reminders_notification_title),
            context.getString(R.string.steps_reminders_notification_body, stepsToday.toString()))
    }

    override fun showTestNotification(): Boolean = showTestNotification(0)

    fun showTestNotification(stepsToday: Int): Boolean = post(TEST_TAG, TEST_NOTIFICATION_ID,
        context.getString(R.string.steps_reminders_test_title),
        context.getString(R.string.steps_reminders_test_body, stepsToday.toString()))

    private fun post(tag: String, id: Int, title: String, body: String): Boolean {
        if (!isAllowed()) return false
        val openApp = PendingIntent.getActivity(context, 1,
            Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_STEPS
                putExtra(MainActivity.EXTRA_STEPS_REQUEST_ID, UUID.randomUUID().toString())
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_steps_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(tag, id, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun cancel(userId: Long) { manager.cancel(tag(userId), NOTIFICATION_ID) }

    companion object {
        const val CHANNEL_ID = "step_reminders"
        const val NOTIFICATION_ID = 4301
        const val TEST_NOTIFICATION_ID = 4302
        const val TEST_TAG = "step-reminder-test"
        fun tag(userId: Long) = "step-reminder-$userId"

        fun notificationsAllowed(context: Context): Boolean {
            val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val manager = context.getSystemService(NotificationManager::class.java)
            return granted && NotificationManagerCompat.from(context).areNotificationsEnabled() &&
                manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
        }
    }
}
