package com.example.amate.core.di

import android.content.Context
import androidx.room.Room
import com.example.amate.core.database.AmateDatabase
import com.example.amate.feature.water.data.RoomWaterRepository
import com.example.amate.feature.water.data.WaterRepository

// Her oppretter vi felles objekter som resten av appen trenger.
class AppContainer(context: Context) {

    // Lager databasen første gang den faktisk blir brukt.
    val database: AmateDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AmateDatabase::class.java,
            "amate_database",
        ).build()
    }



    // Gir enkel tilgang til WaterDao.
val waterDao by lazy {
    database.waterDao()
}

// Lager repository-et og gir det tilgang til WaterDao.
val waterRepository: WaterRepository by lazy {
    RoomWaterRepository(waterDao)
}

} // Denne avslutter AppContainer-klassen