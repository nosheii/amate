package no.gruppe7.amate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import no.gruppe7.amate.feature.water.data.local.WaterDao
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity

// Dette er hoveddatabasen til appen.
// Her registrerer vi tabellene og DAO-ene som Room skal kjenne til.
@Database(
    entities = [WaterEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AmateDatabase : RoomDatabase() {

    // Gir resten av appen tilgang til funksjonene i WaterDao.
    abstract fun waterDao(): WaterDao
}