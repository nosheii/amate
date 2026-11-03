package no.gruppe7.amate.core.di

import android.content.Context
import java.io.File
import no.gruppe7.amate.feature.weather.data.NetworkWeatherRepository
import no.gruppe7.amate.feature.weather.data.WeatherRepository
import no.gruppe7.amate.feature.weather.data.remote.MetWeatherApiService
import androidx.room.Room
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.core.database.ALL_DATABASE_MIGRATIONS
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

    val weatherRepository: WeatherRepository by lazy {
        // HTTP-cache er ikke en Room-tabell. Den lagrer nettverkssvar som kan slettes trygt.
        // Selve nettklienten ordner mellomlagringen på IO når den får en forespørsel.
        NetworkWeatherRepository(MetWeatherApiService(
            cacheDirectory = File(context.applicationContext.cacheDir, "weather_http"),
        ))
    }

    // Lager databasen første gang den faktisk blir brukt.
    // by lazy husker resultatet, så senere bruk får den samme databaseinstansen.
    // amate_database er filnavnet på telefonen. addMigrations oppgraderer den gamle filen
    // i stedet for å slette brukerens data når tabellene endres.
    // Hele oppgraderingslisten er felles for appen og migreringstestene.
    val database: AmateDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AmateDatabase::class.java,
            "amate_database",
        ).addMigrations(*ALL_DATABASE_MIGRATIONS).build()
    }

    // Gir enkel tilgang til WaterDao.
    val waterDao by lazy { database.waterDao() }

    // Begge funksjonene bruker den samme databasen, men hver sin DAO og repository.
    val waterRepository: WaterRepository by lazy { RoomWaterRepository(waterDao) }
    val reminderRepository: ReminderRepository by lazy {
        RoomReminderRepository(database.reminderDao())
    }

    // Mål får sin egen DAO og sitt eget repository, men deler database med vann/påminnelser.
    val goalDao by lazy { database.goalDao() }
    val goalRepository: GoalRepository by lazy { RoomGoalRepository(goalDao) }

    // Android-delen lager kanalen, sjekker tillatelse og viser/fjerner varsler.
    val waterReminderNotifications by lazy { WaterReminderNotifications(context) }
    // Bestiller én gjentakende jobb hos Android når lagrede påminnelser er på.
    val reminderScheduler by lazy { WorkManagerReminderScheduler(context, waterReminderNotifications) }
    // get() lager en ny Runner når den etterspørres. Da får den også en fersk standardklokke,
    // slik at telefonens nåværende tidssone brukes hvis brukeren har byttet tidssone.
    val waterReminderRunner get() = WaterReminderRunner(database, waterReminderNotifications)
}
