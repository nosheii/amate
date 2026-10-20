package no.gruppe7.amate.core.di

import android.content.Context
import androidx.room.Room
import no.gruppe7.amate.core.database.AmateDatabase
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.RoomGoalRepository
import no.gruppe7.amate.feature.water.data.RoomWaterRepository
import no.gruppe7.amate.feature.water.data.WaterRepository

// Her oppretter vi felles objekter som resten av appen trenger.
class AppContainer(context: Context) {

    // Lager databasen første gang den faktisk blir brukt.
    val database: AmateDatabase by lazy {
        Room.databaseBuilder(context.applicationContext, AmateDatabase::class.java, "amate_database",)
            // Under utvikling: når versjonen endres, sletter Room databasen og lager den på nytt
            // I stedet for at vi må skrive migreringer
            // Lokale dataer forsvinner når man åpner appen, men det er greit nå.
            // Må bare fjernes før vi leverer prosjektet.
            .fallbackToDestructiveMigration(true)
            .build()
    }

    // Gir enkel tilgang til WaterDao.
    val waterDao by lazy {
        database.waterDao()
    }

    // Lager repository-et og gir det tilgang til WaterDao.
    val waterRepository: WaterRepository by lazy {
        RoomWaterRepository(waterDao)
    }
    // Gir enkel tilgang til GoalDao.
    val goalDao by lazy {
        database.goalDao()
    }

    // Lager repository-et og gir det tilgang til GoalDao.
    val goalRepository: GoalRepository by lazy {
        RoomGoalRepository(goalDao)
    }

} // Denne avslutter AppContainer-klassen

