package no.gruppe7.amate.core.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import no.gruppe7.amate.feature.goals.data.local.GoalDao
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity
import no.gruppe7.amate.feature.water.data.local.WaterDao
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity

// Dette er hoveddatabasen til appen.
// Her registrerer vi tabellene og DAO-ene som Room skal kjenne til.
@Database(
    entities = [
        WaterEntryEntity::class,
        GoalEntity::class,
        SelfCareTaskEntity::class,
    ],

    // Versjonen må økes hver gang tabellene endres.
    // Økt fra 1 til 2 fordi goals og self_care_tasks er nye.
    version = 2,
    exportSchema = false,
)
abstract class AmateDatabase : RoomDatabase() {

    // Gir resten av appen tilgang til funksjonen i WaterDao.
    abstract fun waterDao(): WaterDao

    // Gir resten av appen tilgang til funksjonene i GoalDao.
    abstract fun goalDao(): GoalDao
}
