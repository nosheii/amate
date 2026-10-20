package no.gruppe7.amate.feature.reminders.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.core.database.MIGRATION_1_2
import no.gruppe7.amate.feature.reminders.data.RoomReminderRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {
    private lateinit var database: AmateDatabase
    private lateinit var repository: RoomReminderRepository
    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AmateDatabase::class.java).build()
        repository = RoomReminderRepository(database.reminderDao())
    }
    @After fun tearDown() { database.close() }

    @Test fun defaultsAreOffAndNotWrittenUntilSave() = runBlocking {
        assertEquals(ReminderEntity(1), repository.observeSettings(1).first())
        assertNull(database.reminderDao().observeSettings(1).first())
    }

    @Test fun saveUpdatesSameUserWithoutAffectingOtherUsers() = runBlocking {
        repository.save(ReminderEntity(1, true, 5, 540, 1200))
        repository.save(ReminderEntity(2, true, 1, 600, 1100))
        repository.save(ReminderEntity(1, false, 2, 480, 1260))
        assertEquals(ReminderEntity(1, false, 2, 480, 1260), repository.observeSettings(1).first())
        assertEquals(ReminderEntity(2, true, 1, 600, 1100), repository.observeSettings(2).first())
    }

    @Test fun repositoryRejectsInvalidCountAndTime() = runBlocking {
        for (settings in listOf(ReminderEntity(1, remindersPerDay = 0),
            ReminderEntity(1, remindersPerDay = 6), ReminderEntity(1, startMinute = -1),
            ReminderEntity(1, endMinute = 1440), ReminderEntity(1, startMinute = 100, endMinute = 100))) {
            try {
                repository.save(settings)
                fail("Ugyldige innstillinger skulle blitt avvist")
            } catch (_: IllegalArgumentException) { /* Forventet: ingenting lagres. */ }
        }
        assertNull(database.reminderDao().observeSettings(1).first())
    }

    @Test fun settingsSurviveReopeningDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "reminders-persistence-${java.util.UUID.randomUUID()}.db"
        var persistent = Room.databaseBuilder(context, AmateDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2).build()
        try {
            persistent.reminderDao().save(ReminderEntity(1, true, 4, 600, 1200))
            persistent.close()
            persistent = Room.databaseBuilder(context, AmateDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2).build()
            assertEquals(ReminderEntity(1, true, 4, 600, 1200), persistent.reminderDao().observeSettings(1).first())
        } finally {
            persistent.close()
            context.deleteDatabase(name)
        }
    }
}
