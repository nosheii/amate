package no.gruppe7.amate.core.di

import android.content.Context
import androidx.room.Room
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.core.database.MIGRATION_1_2
import no.gruppe7.amate.core.database.MIGRATION_2_3
import no.gruppe7.amate.feature.reminders.scheduling.WaterReminderNotifications
import no.gruppe7.amate.feature.reminders.scheduling.WaterReminderRunner
import no.gruppe7.amate.feature.reminders.scheduling.WorkManagerReminderScheduler
import no.gruppe7.amate.feature.reminders.data.ReminderRepository
import no.gruppe7.amate.feature.reminders.data.RoomReminderRepository
import no.gruppe7.amate.feature.water.data.RoomWaterRepository
import no.gruppe7.amate.feature.water.data.WaterRepository

// Her oppretter vi felles objekter som resten av appen trenger.
// I stedet for at hver skjerm bygger sin egen database, henter de ting herfra.
class AppContainer(context: Context) {

    // Lager databasen første gang den faktisk blir brukt.
    // by lazy husker resultatet, så senere bruk får den samme databaseinstansen.
    // amate_database er filnavnet på telefonen. addMigrations oppgraderer den gamle filen
    // i stedet for å slette brukerens data når tabellene endres.
    val database: AmateDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AmateDatabase::class.java,
            "amate_database",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }

    // Gir enkel tilgang til WaterDao.
    val waterDao by lazy { database.waterDao() }

    // Begge funksjonene bruker den samme databasen, men hver sin DAO og repository.
    val waterRepository: WaterRepository by lazy { RoomWaterRepository(waterDao) }
    val reminderRepository: ReminderRepository by lazy {
        RoomReminderRepository(database.reminderDao())
    }

    // Android-delen lager kanalen, sjekker tillatelse og viser/fjerner varsler.
    val waterReminderNotifications by lazy { WaterReminderNotifications(context) }
    // Bestiller én gjentakende jobb hos Android når lagrede påminnelser er på.
    val reminderScheduler by lazy { WorkManagerReminderScheduler(context, waterReminderNotifications) }
    // get() lager en ny Runner når den etterspørres. Da får den også en fersk standardklokke,
    // slik at telefonens nåværende tidssone brukes hvis brukeren har byttet tidssone.
    val waterReminderRunner get() = WaterReminderRunner(database, waterReminderNotifications)
}
