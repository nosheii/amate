package no.gruppe7.amate.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import no.gruppe7.amate.feature.steps.data.local.StepReminderEntity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepDatabaseMigrationTest {
    @Test fun upgradeFrom6And7PreservesProfileAvatarStepsWaterAndSettings() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (version in listOf(6, 7)) {
            val name = "step-migration-$version-${java.util.UUID.randomUUID()}"
            val legacy: RoomDatabase = if (version == 6)
                Room.databaseBuilder(context, LegacyAvatarDatabase::class.java, name).build()
            else Room.databaseBuilder(context, LegacyStepDatabase::class.java, name).build()
            var current: AmateDatabase? = null
            try {
                val sql = legacy.openHelper.writableDatabase
                sql.execSQL("INSERT INTO user_profile (userId, displayName, age) VALUES (1, 'Nora', 24)")
                sql.execSQL("INSERT INTO avatar_config (userId, avatarKey) VALUES (1, 'bjørn')")
                sql.execSQL("INSERT INTO daily_steps VALUES (1, 20000, 2345)")
                sql.execSQL("INSERT INTO water_entries (userId, amountMl, loggedAtMillis) VALUES (1, 375, 1000)")
                sql.execSQL("INSERT INTO water_reminder_settings VALUES (1, 1, 2, 480, 1200)")
                if (version == 7) sql.execSQL("INSERT INTO step_reminder_settings VALUES (1, 1, 4, 540, 1260)")
                legacy.close()
                val upgraded = Room.databaseBuilder(context, AmateDatabase::class.java, name)
                    .addMigrations(*ALL_DATABASE_MIGRATIONS).build()
                current = upgraded
                assertEquals("Nora", upgraded.userProfileDao().observeProfile(1).first()!!.displayName)
                assertEquals("bjørn", upgraded.avatarConfigDao().observeAvatarConfig(1).first()!!.avatarKey)
                assertEquals(2345, upgraded.stepsDao().getStepsForDayOneShot(1, 20000)!!.stepCount)
                assertEquals(375, upgraded.waterDao().observeEntries(1).first().single().amountMl)
                assertEquals(2, upgraded.reminderDao().getSettings(1)!!.remindersPerDay)
                if (version == 7) assertEquals(4, upgraded.stepReminderDao().getSettings(1)!!.remindersPerDay)
                else assertNull(upgraded.stepReminderDao().getSettings(1))
                upgraded.stepReminderDao().save(StepReminderEntity(2, true))
                assertNotNull(upgraded.stepReminderDao().getSettings(2))
                assertNull(upgraded.stepReminderDeliveryDao().get(1, "2026-11-18"))
            } finally {
                legacy.close()
                current?.close()
                context.deleteDatabase(name)
            }
        }
    }
}
