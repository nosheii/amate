package no.gruppe7.amate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
    // Hver Entity blir en tabell: vannregistreringer, påminnelsesvalg og sendelogg.
    entities = [WaterEntryEntity::class, ReminderEntity::class, ReminderDeliveryEntity::class],
    // Databaseskjemaets versjon, ikke appens versjonsnummer. Må passe med migreringene.
    version = 3,
    exportSchema = false,
)
abstract class AmateDatabase : RoomDatabase() {

    // Gir resten av appen tilgang til funksjonene i WaterDao.
    abstract fun waterDao(): WaterDao

    // Tilgang til brukerens lagrede påminnelsesvalg.
    abstract fun reminderDao(): ReminderDao

    // Tilgang til huskelappen som hindrer gjentatte sendeforsøk for samme tidspunkt.
    abstract fun reminderDeliveryDao(): ReminderDeliveryDao
}
