package no.gruppe7.amate.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderDao
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryEntity
import no.gruppe7.amate.feature.water.data.local.WaterDao
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

// Gjenskaper databaseversjonen som allerede finnes på telefonen før oppdateringen.
@Database(entities = [WaterEntryEntity::class], version = 1, exportSchema = false)
abstract class LegacyWaterDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao
}

// Versjonen fra forrige runde må også kunne oppgraderes uten å miste innstillingene.
@Database(entities = [WaterEntryEntity::class, ReminderEntity::class], version = 2, exportSchema = false)
abstract class LegacyReminderDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao
    abstract fun reminderDao(): ReminderDao
}

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test fun upgradeFrom1To3PreservesWaterAndAddsSettingsAndDeliveryLog() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-water-reminders-${java.util.UUID.randomUUID()}.db"
        val legacy = Room.databaseBuilder(context, LegacyWaterDatabase::class.java, name).build()
        var upgraded: AmateDatabase? = null
        try {
            val entry = WaterEntryEntity(userId = 1, amountMl = 375, loggedAtMillis = 1000)
            val id = legacy.waterDao().insert(entry)
            legacy.close()

            val current = Room.databaseBuilder(context, AmateDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
            upgraded = current
            // Å åpne databasen får også Room til å kontrollere hele det nye skjemaet.
            assertEquals(entry.copy(id = id), current.waterDao().observeEntries(1).first().single())
            val settings = ReminderEntity(1, true, 4, 540, 1200)
            current.reminderDao().save(settings)
            assertEquals(settings, current.reminderDao().observeSettings(1).first())
            assertEquals(375, current.waterDao().observeTotalForDay(1, 0, 2000).first())
            val log = ReminderDeliveryEntity(1, "2026-10-19", 600, 1, 1000)
            current.reminderDeliveryDao().save(log)
            assertEquals(log, current.reminderDeliveryDao().get(1, "2026-10-19"))
        } finally {
            legacy.close()
            upgraded?.close()
            // Fjerner bare den unike testdatabasen, aldri appens vannlogg.
            context.deleteDatabase(name)
        }
    }

    @Test fun upgradeFrom2To3PreservesSettingsAndWater() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-reminders-v2-${java.util.UUID.randomUUID()}.db"
        val legacy = Room.databaseBuilder(context, LegacyReminderDatabase::class.java, name).build()
        var upgraded: AmateDatabase? = null
        try {
            val settings = ReminderEntity(1, true, 4, 540, 1200)
            legacy.reminderDao().save(settings)
            legacy.waterDao().insert(WaterEntryEntity(userId = 1, amountMl = 250, loggedAtMillis = 1234))
            legacy.close()
            val current = Room.databaseBuilder(context, AmateDatabase::class.java, name)
                .addMigrations(MIGRATION_2_3).build()
            upgraded = current
            assertEquals(settings, current.reminderDao().getSettings(1))
            assertEquals(250, current.waterDao().observeEntries(1).first().single().amountMl)
            val log = ReminderDeliveryEntity(1, "2026-10-19", 700, 1, 5678)
            current.reminderDeliveryDao().save(log)
            assertEquals(log, current.reminderDeliveryDao().get(1, log.localDate))
        } finally {
            legacy.close()
            upgraded?.close()
            context.deleteDatabase(name)
        }
    }
}
