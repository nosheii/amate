package no.gruppe7.amate.feature.reminders.scheduling

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Kjøres på Android fordi vi bruker ekte Room, men varsling og klokke er styrt av testen.
@RunWith(AndroidJUnit4::class)
class WaterReminderRunnerTest {
    private lateinit var database: AmateDatabase
    private val notifier = FakeWaterNotifier()
    private val zone = ZoneId.of("Europe/Oslo")
    // Clock.fixed gjør at «nå» alltid er samme øyeblikk i denne testen.
    private fun clock(time: String = "2026-10-19T14:00:00Z") = Clock.fixed(Instant.parse(time), zone)
    private fun runner(time: String = "2026-10-19T14:00:00Z") = WaterReminderRunner(database, notifier, clock(time))

    @Before fun setUp() {
        // En egen database i minnet for hver test. Appens virkelige vannlogg blir ikke brukt.
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AmateDatabase::class.java).build()
    }
    @After fun tearDown() { database.close() }
    private suspend fun enable(userId: Long = 1) {
        database.reminderDao().save(ReminderEntity(userId, true, 3, 480, 1200))
    }

    @Test fun noSettingsDisabledOrDeniedPermissionDoesNotNotify() = runBlocking {
        assertFalse(runner().run(1))
        database.reminderDao().save(ReminderEntity(1))
        assertFalse(runner().run(1))
        enable()
        notifier.allowed = false
        assertFalse(runner().run(1))
        assertNull(database.reminderDeliveryDao().get(1, "2026-10-19"))
        assertEquals(0, notifier.sent)
        notifier.allowed = true
        assertTrue(runner().run(1))
        assertEquals(1, notifier.sent)
    }

    @Test fun concurrentChecksAndNewRunnerOnlySendOneNotification() = runBlocking {
        enable()
        // Seks samtidige forsøk utfordrer transaksjonen: bare ett skal få sende.
        // awaitAll venter på resultatene fra alle seks før vi teller dem.
        val results = (1..6).map { async { runner().run(1) } }.awaitAll()
        assertEquals(1, results.count { it })
        assertFalse(runner().run(1))
        assertEquals(1, notifier.sent)
        assertEquals(1, database.reminderDeliveryDao().get(1, "2026-10-19")?.sentCount)
    }

    @Test fun changedSettingsAreReadAndDoNotResetDailyCount() = runBlocking {
        enable()
        assertTrue(runner().run(1))
        database.reminderDao().save(ReminderEntity(1, true, 1, 480, 1200))
        assertFalse(runner("2026-10-19T16:30:00Z").run(1))
        enable()
        assertTrue(runner("2026-10-19T16:30:00Z").run(1))
        database.reminderDao().save(ReminderEntity(1, false))
        assertFalse(runner("2026-10-19T17:00:00Z").run(1))
        assertEquals(2, notifier.sent)
    }

    @Test fun lateCheckOutsideWindowIsSkippedAndNextDayCanSend() = runBlocking {
        enable()
        assertFalse(runner("2026-10-19T18:00:00Z").run(1)) // 20:00 lokalt = slutt.
        assertTrue(runner("2026-10-19T14:00:00Z").run(1))
        assertTrue(runner("2026-10-20T14:00:00Z").run(1))
        enable(2)
        assertTrue(runner().run(2)) // En annen bruker har sin egen teller.
        assertEquals(3, notifier.sent)
    }

    @Test fun deliveryLogSurvivesDatabaseReopening() = runBlocking {
        // Akkurat her bruker vi en unik fil i stedet for minne, for å etterligne en omstart.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "water-delivery-${java.util.UUID.randomUUID()}.db"
        var disk = Room.databaseBuilder(context, AmateDatabase::class.java, name).build()
        try {
            disk.reminderDao().save(ReminderEntity(1, true, 3, 480, 1200))
            assertTrue(WaterReminderRunner(disk, notifier, clock()).run(1))
            disk.close()
            disk = Room.databaseBuilder(context, AmateDatabase::class.java, name).build()
            assertFalse(WaterReminderRunner(disk, notifier, clock()).run(1))
            assertEquals(1, notifier.sent)
        } finally {
            // finally rydder opp også hvis en assert feiler. Vi sletter bare den unike testfilen.
            disk.close()
            context.deleteDatabase(name)
        }
    }
}

// Testene sjekker sending uten å lage ekte varsler på brukerens telefon.
class FakeWaterNotifier : WaterReminderNotifier {
    var allowed = true
    var sent = 0
    var cancelled = 0
    override fun isAllowed() = allowed
    override fun show(userId: Long) { sent++ }
    override fun cancel(userId: Long) { cancelled++ }
}
