package no.gruppe7.amate.feature.reminders.scheduling

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Her sjekker vi ekte Android-varsler. Det er noe annet enn å teste bakgrunnsplanens tidspunkt.
@RunWith(AndroidJUnit4::class)
class WaterReminderNotificationsTest {
    // Gir testappen tilgang, uten å trykke gjennom en systemdialog i hver test.
    @get:Rule val permission: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 33) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        GrantPermissionRule.grant() // Eldre Android har ingen runtime-tillatelse for varsler.
    }

    @Test fun realNotificationUsesChannelAndAppIntentWithoutStacking() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)
        val notifier = WaterReminderNotifications(context)
        val userId = 987654323L
        try {
            assertTrue(notifier.isAllowed())
            notifier.show(userId)
            notifier.show(userId)
            // NotificationManager bruker Binder: gir Android litt tid til å publisere.
            val deadline = android.os.SystemClock.elapsedRealtime() + 5_000
            var posted = manager.activeNotifications.filter { it.tag == WaterReminderNotifications.tag(userId) }
            while (posted.isEmpty() && android.os.SystemClock.elapsedRealtime() < deadline) {
                android.os.SystemClock.sleep(50)
                posted = manager.activeNotifications.filter { it.tag == WaterReminderNotifications.tag(userId) }
            }
            assertEquals(1, posted.size)
            assertEquals(WaterReminderNotifications.CHANNEL_ID, posted.single().notification.channelId)
            assertNotNull(posted.single().notification.contentIntent)
            assertEquals(context.packageName, posted.single().notification.contentIntent.creatorPackage)
            notifier.cancel(userId)
            while (manager.activeNotifications.any { it.tag == WaterReminderNotifications.tag(userId) } &&
                android.os.SystemClock.elapsedRealtime() < deadline) {
                android.os.SystemClock.sleep(50)
            }
            assertTrue(manager.activeNotifications.none { it.tag == WaterReminderNotifications.tag(userId) })
        } finally {
            notifier.cancel(userId)
        }
    }

    @Test fun testNotificationDoesNotReplaceRegularReminderOrStack() {
        // Lager ett vanlig varsel og to testtrykk. Vi forventer ett varsel av hver type, ikke tre.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(NotificationManager::class.java)
        val notifier = WaterReminderNotifications(context)
        val userId = 987654324L
        try {
            notifier.show(userId)
            assertTrue(notifier.showTestNotification())
            assertTrue(notifier.showTestNotification())
            val deadline = android.os.SystemClock.elapsedRealtime() + 5_000
            while (manager.activeNotifications.none { it.tag == WaterReminderNotifications.TEST_TAG } &&
                android.os.SystemClock.elapsedRealtime() < deadline) {
                android.os.SystemClock.sleep(50)
            }
            val active = manager.activeNotifications
            assertEquals(1, active.count { it.tag == WaterReminderNotifications.tag(userId) })
            val test = active.single { it.tag == WaterReminderNotifications.TEST_TAG }
            assertEquals(WaterReminderNotifications.TEST_NOTIFICATION_ID, test.id)
            assertEquals(context.getString(no.gruppe7.amate.R.string.reminders_test_notification_title),
                test.notification.extras.getString(android.app.Notification.EXTRA_TITLE))
        } finally {
            // Rydder bare bort varslene som denne testen har opprettet.
            notifier.cancel(userId)
            manager.cancel(WaterReminderNotifications.TEST_TAG, WaterReminderNotifications.TEST_NOTIFICATION_ID)
        }
    }
}
