package no.gruppe7.amate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import no.gruppe7.amate.feature.goals.data.local.GoalDao
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity
import no.gruppe7.amate.feature.water.data.local.WaterDao
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderDao
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryEntity
import no.gruppe7.amate.feature.reminders.data.local.ReminderDeliveryDao

// Dette er hoveddatabasen til appen.
// Her registrerer vi tabellene og DAO-ene som Room skal kjenne til.
// Vi skriver ikke databaseklassen ferdig selv. Room/KSP lager implementasjonen ved bygging.
@Database(
    // Både mål og vannpåminnelser bruker samme database. Ingen gamle tabeller fjernes.
    entities = [
        WaterEntryEntity::class,
        ReminderEntity::class,
        ReminderDeliveryEntity::class,
        GoalEntity::class,
        SelfCareTaskEntity::class,
    ],

    // main var på 3. Den samlede versjonen med mål og gjøremål blir derfor 4.
    version = 4,
    exportSchema = false,
)
abstract class AmateDatabase : RoomDatabase() {

    // Gir resten av appen tilgang til funksjonen i WaterDao.
    abstract fun waterDao(): WaterDao

    // Gir resten av appen tilgang til funksjonene i GoalDao.
    abstract fun goalDao(): GoalDao

    // Tilgang til brukerens lagrede påminnelsesvalg.
    abstract fun reminderDao(): ReminderDao

    // Tilgang til huskelappen som hindrer gjentatte sendeforsøk for samme tidspunkt.
    abstract fun reminderDeliveryDao(): ReminderDeliveryDao
}
