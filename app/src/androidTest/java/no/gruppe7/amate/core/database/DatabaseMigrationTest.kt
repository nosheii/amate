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
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalDao
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity
import no.gruppe7.amate.feature.water.data.local.WaterDao
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

// Akkurat versjon 2 fra Najmas branch: mål/gjøremål, men ingen påminnelsestabeller.
// At denne har samme versjonsnummer som klassen over er grunnen til den direkte 2 → 4-veien.
@Database(entities = [WaterEntryEntity::class, GoalEntity::class, SelfCareTaskEntity::class],
    version = 2, exportSchema = false)
abstract class LegacyGoalsDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao
    abstract fun goalDao(): GoalDao
}

// Versjon 3 som ligger på main før denne sammenslåingen.
@Database(entities = [WaterEntryEntity::class, ReminderEntity::class, ReminderDeliveryEntity::class],
    version = 3, exportSchema = false)
abstract class LegacyMainDatabase : RoomDatabase() {
    abstract fun waterDao(): WaterDao
    abstract fun reminderDao(): ReminderDao
    abstract fun reminderDeliveryDao(): no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryDao
}

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @Test fun upgradeFrom1To4PreservesWaterAndAddsAllTables() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-water-reminders-${java.util.UUID.randomUUID()}.db"
        val legacy = Room.databaseBuilder(context, LegacyWaterDatabase::class.java, name).build()
        var upgraded: AmateDatabase? = null
        try {
            val entry = WaterEntryEntity(userId = 1, amountMl = 375, loggedAtMillis = 1000)
            val id = legacy.waterDao().insert(entry)
            legacy.close()

            val current = Room.databaseBuilder(context, AmateDatabase::class.java, name)
                .addMigrations(*ALL_DATABASE_MIGRATIONS).build()
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
            assertTrue(current.goalDao().observeActiveGoals(1).first().isEmpty())
        } finally {
            legacy.close()
            upgraded?.close()
            // Fjerner bare den unike testdatabasen, aldri appens vannlogg.
            context.deleteDatabase(name)
        }
    }

    @Test fun upgradeFromReminderVersion2To4PreservesSettingsAndWater() = runBlocking {
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
                .addMigrations(*ALL_DATABASE_MIGRATIONS).build()
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

    @Test fun upgradeFromGoalsVersion2To4PreservesGoalsTasksAndWater() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-goals-v2-${java.util.UUID.randomUUID()}.db"
        val legacy = Room.databaseBuilder(context, LegacyGoalsDatabase::class.java, name).build()
        var upgraded: AmateDatabase? = null
        try {
            val water = WaterEntryEntity(userId = 1, amountMl = 350, loggedAtMillis = 1234)
            val waterId = legacy.waterDao().insert(water)
            val goal = GoalEntity(userId = 1, type = GoalType.WATER, targetValue = 2500)
            val goalId = legacy.goalDao().upsert(goal)
            // Bruker 2 skal beholde sitt eget vannmål gjennom oppgraderingen.
            legacy.goalDao().upsert(GoalEntity(userId = 2, type = GoalType.WATER, targetValue = 1800))
            // Gjøremål har ikke DAO ennå. Vi legger inn testdata med SQL for å sjekke bevaring.
            legacy.openHelper.writableDatabase.execSQL(
                "INSERT INTO self_care_tasks (id, userId, title, scheduledForMillis, isCompleted, completedAtMillis) " +
                    "VALUES (31, 1, 'En liten tur', 1000, 1, 2000), (32, 1, 'En pause', 3000, 0, NULL)"
            )
            legacy.close()
            val current = Room.databaseBuilder(context, AmateDatabase::class.java, name)
                .addMigrations(*ALL_DATABASE_MIGRATIONS).build()
            upgraded = current
            // Room validerer alle fem tabeller og målindeksen når databasen åpnes.
            assertEquals(water.copy(id = waterId), current.waterDao().observeEntries(1).first().single())
            assertEquals(goal.copy(id = goalId), current.goalDao().findGoal(1, GoalType.WATER))
            assertEquals(1800, current.goalDao().findGoal(2, GoalType.WATER)?.targetValue)
            current.openHelper.readableDatabase.query("SELECT * FROM self_care_tasks ORDER BY id").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(31L, cursor.getLong(cursor.getColumnIndexOrThrow("id")))
                assertEquals("En liten tur", cursor.getString(cursor.getColumnIndexOrThrow("title")))
                assertEquals(2000L, cursor.getLong(cursor.getColumnIndexOrThrow("completedAtMillis")))
                assertTrue(cursor.moveToNext())
                assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("completedAtMillis")))
                assertEquals(2, cursor.count)
            }
            // Også de nye påminnelsestabellene skal kunne brukes etter oppgraderingen.
            val settings = ReminderEntity(1, true, 4, 540, 1200)
            current.reminderDao().save(settings)
            assertEquals(settings, current.reminderDao().getSettings(1))
            val log = ReminderDeliveryEntity(1, "2026-10-20", 600, 1, 5678)
            current.reminderDeliveryDao().save(log)
            assertEquals(log, current.reminderDeliveryDao().get(1, log.localDate))
        } finally {
            legacy.close()
            upgraded?.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun upgradeFromMainVersion3To4PreservesWaterSettingsAndDeliveryLog() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-main-v3-${java.util.UUID.randomUUID()}.db"
        val legacy = Room.databaseBuilder(context, LegacyMainDatabase::class.java, name).build()
        var upgraded: AmateDatabase? = null
        try {
            val water = WaterEntryEntity(userId = 1, amountMl = 400, loggedAtMillis = 1234)
            val waterId = legacy.waterDao().insert(water)
            val settings = ReminderEntity(1, true, 4, 540, 1200)
            val log = ReminderDeliveryEntity(1, "2026-10-20", 700, 2, 5678)
            legacy.reminderDao().save(settings)
            legacy.reminderDeliveryDao().save(log)
            legacy.close()
            val current = Room.databaseBuilder(context, AmateDatabase::class.java, name)
                .addMigrations(*ALL_DATABASE_MIGRATIONS).build()
            upgraded = current
            assertEquals(water.copy(id = waterId), current.waterDao().observeEntries(1).first().single())
            assertEquals(settings, current.reminderDao().getSettings(1))
            assertEquals(log, current.reminderDeliveryDao().get(1, log.localDate))
            // Skriver og redigerer mål gjennom Najmas repository for å prøve hele koblingen.
            val goals = RoomGoalRepository(current.goalDao())
            val id = goals.saveGoal(1, GoalType.WATER, 2500)
            assertEquals(id, goals.saveGoal(1, GoalType.WATER, 3000))
            assertEquals(3000, goals.observeGoal(1, GoalType.WATER).first()?.targetValue)
            assertEquals(1, goals.observeGoals(1).first().size)
            // Gjøremålstabellen skal også finnes på installasjoner som bare hadde vann/påminnelser.
            current.openHelper.readableDatabase.query("SELECT COUNT(*) FROM self_care_tasks").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        } finally {
            legacy.close()
            upgraded?.close()
            context.deleteDatabase(name)
        }
    }
}
