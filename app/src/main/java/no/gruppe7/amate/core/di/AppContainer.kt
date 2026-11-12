package no.gruppe7.amate.core.di

import android.content.Context
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import no.gruppe7.amate.feature.weather.data.NetworkWeatherRepository
import no.gruppe7.amate.feature.weather.data.WeatherRepository
import no.gruppe7.amate.feature.weather.data.remote.MetWeatherApiService
import no.gruppe7.amate.feature.weather.location.AndroidLocationDataSource
import no.gruppe7.amate.feature.weather.location.LocationDataSource
import androidx.room.Room
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.profile.data.ProfileRepository
import no.gruppe7.amate.feature.profile.data.RoomProfileRepository
import no.gruppe7.amate.feature.onboarding.data.AndroidOnboardingCompletionStore
import no.gruppe7.amate.feature.onboarding.data.OnboardingRepository
import no.gruppe7.amate.feature.onboarding.data.RoomOnboardingRepository
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
import no.gruppe7.amate.feature.tasks.data.RoomTaskRepository
import no.gruppe7.amate.feature.tasks.data.TaskRepository
import no.gruppe7.amate.feature.steps.data.RoomStepsRepository
import no.gruppe7.amate.feature.steps.data.StepsRepository
import no.gruppe7.amate.feature.steps.sensor.AndroidStepSensorDataSource
import no.gruppe7.amate.feature.steps.sensor.StepSensorDataSource
import no.gruppe7.amate.feature.achievements.data.AchievementRepository
import no.gruppe7.amate.feature.achievements.data.RoomAchievementRepository

// Her oppretter vi felles objekter som resten av appen trenger.
// I stedet for at hver skjerm bygger sin egen database, henter de ting herfra.
class AppContainer(
    context: Context,
    externalScope: CoroutineScope? = null,
) {

    // Bare en engangsforespørsel når værkortet trenger posisjonen. Starter ingen sporing her.
    val weatherLocation: LocationDataSource by lazy { AndroidLocationDataSource(context) }

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

    //Profil og avatar dleer database med resten av appen, men har sin egen
    // DAO per tabell og ett felles repository som håndterer begge.
    val userProfileDao by lazy { database.userProfileDao() }
    val avatarConfigDao by lazy { database.avatarConfigDao() }
    val profileRepository: ProfileRepository by lazy {
        RoomProfileRepository(userProfileDao, avatarConfigDao)
    }

    // Oppstarten gjenbruker profil og mål. Ingen ny database eller ny bruker-ID er nødvendig.
    val onboardingRepository: OnboardingRepository by lazy {
        RoomOnboardingRepository(database, profileRepository, goalRepository, AndroidOnboardingCompletionStore(context))
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

    // Gjøremål deler database med resten, men har egen DAO og eget repository
    val taskDao by lazy {database.selfCareTaskDao()}

    val taskRepository: TaskRepository by lazy { RoomTaskRepository(taskDao) }

    // Merker leser fra vann, skritt og gjøremål, men lagrer bare hva som er tatt.
    val achievementDao by lazy { database.achievementDao() }
    val achievementRepository: AchievementRepository by lazy { RoomAchievementRepository(achievementDao) }

    private val defaultScope by lazy {
        externalScope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // Skritt-sensor og skritt-repository
    val stepSensorDataSource: StepSensorDataSource by lazy {
        AndroidStepSensorDataSource(context)
    }
    val stepsDao by lazy { database.stepsDao() }
    val stepsRepository: StepsRepository by lazy {
        RoomStepsRepository(stepsDao, stepSensorDataSource, defaultScope)
    }

    // Android-delen lager kanalen, sjekker tillatelse og viser/fjerner varsler.
    val waterReminderNotifications by lazy { WaterReminderNotifications(context) }
    // Bestiller én gjentakende jobb hos Android når lagrede påminnelser er på.
    val reminderScheduler by lazy { WorkManagerReminderScheduler(context, waterReminderNotifications) }
    // get() lager en ny Runner når den etterspørres. Da får den også en fersk standardklokke,
    // slik at telefonens nåværende tidssone brukes hvis brukeren har byttet tidssone.
    val waterReminderRunner get() = WaterReminderRunner(database, waterReminderNotifications)
}
