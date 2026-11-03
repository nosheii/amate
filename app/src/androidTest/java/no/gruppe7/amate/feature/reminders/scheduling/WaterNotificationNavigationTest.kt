package no.gruppe7.amate.feature.reminders.scheduling

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import no.gruppe7.amate.MainActivity
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WaterNotificationNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val permission: GrantPermissionRule = if (Build.VERSION.SDK_INT >= 33) {
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
    } else GrantPermissionRule.grant()

    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        // Rydder bare testvarselet. Testen registrerer ikke vann eller endrer innstillinger.
        context.getSystemService(NotificationManager::class.java)
            .cancel(WaterReminderNotifications.TEST_TAG, WaterReminderNotifications.TEST_NOTIFICATION_ID)
    }

    private fun waitForWater() {
        compose.waitUntil(10_000) {
            runCatching { compose.onNodeWithTag("water_total").assertIsDisplayed() }.isSuccess
        }
    }

    @Test
    fun coldStartOpensWaterAndOldRequestDoesNotReappearAfterRecreation() {
        // Starter uten en åpen MainActivity, med samme handling som vannvarselet bruker.
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_WATER
            putExtra(MainActivity.EXTRA_WATER_REQUEST_ID, "cold-start-test")
        })
        waitForWater()
        compose.onNodeWithContentDescription("Tilbake til hjem").performClick()
        compose.onNodeWithTag("home_tab_profile").performClick()
        scenario!!.recreate()
        // Det gamle varseltrykket er brukt opp. En rotasjon skal ikke overstyre valgt fane.
        compose.onNodeWithTag("home_tab_profile").assertIsSelected()
    }

    @Test
    fun realNotificationOpensWaterFromAnotherTabWithoutCreatingAnotherActivity() {
        // ActivityScenario følger Intent-typen den startet med. Samme ACTION gjør at den også
        // kjenner igjen Activity-en etter onNewIntent. Vi går til Profil før det ekte varseltrykket.
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_WATER
            putExtra(MainActivity.EXTRA_WATER_REQUEST_ID, "warm-start-test")
        })
        waitForWater()
        compose.onNodeWithTag("home_tab_profile").performClick()
        var original: MainActivity? = null
        scenario!!.onActivity { original = it }

        val notifier = WaterReminderNotifications(context)
        assertTrue(notifier.showTestNotification())
        val manager = context.getSystemService(NotificationManager::class.java)
        compose.waitUntil(5_000) {
            manager.activeNotifications.any { it.tag == WaterReminderNotifications.TEST_TAG }
        }
        val openWater = manager.activeNotifications.single {
            it.tag == WaterReminderNotifications.TEST_TAG
        }.notification.contentIntent
        // Utfører den virkelige PendingIntent-en fra varselet, ikke en egen testsnarvei.
        openWater.send()
        waitForWater()
        scenario!!.onActivity { assertSame(original, it) }

        // Et nytt trykk skal også virke etter at den første beskjeden er håndtert.
        compose.onNodeWithTag("home_tab_profile").performClick()
        openWater.send()
        waitForWater()
        scenario!!.onActivity { assertSame(original, it) }
    }
}
