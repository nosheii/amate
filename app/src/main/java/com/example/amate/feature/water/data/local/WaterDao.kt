package com.example.amate.feature.water.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Her samler vi alt vi kan gjøre med vannregistreringene i databasen.
@Dao
interface WaterDao {

    // Legger til en ny vannregistrering og returnerer ID-en Room lager.
    @Insert
    suspend fun insert(entry: WaterEntryEntity): Long

    // Oppdaterer en registrering som allerede finnes.
    @Update
    suspend fun update(entry: WaterEntryEntity)

    // Sletter en registrering.
    @Delete
    suspend fun delete(entry: WaterEntryEntity)

    // Henter alle vannregistreringene til brukeren.
    // Nyeste registrering vises først.
    @Query(
        """
        SELECT * FROM water_entries
        WHERE userId = :userId
        ORDER BY loggedAtMillis DESC
        """
    )
    fun observeEntries(userId: Long): Flow<List<WaterEntryEntity>>

    // Regner ut hvor mye vann brukeren har drukket i løpet av én dag.
    // startMillis og endMillis forteller hvilken dag vi skal summere.
    @Query(
        """
        SELECT COALESCE(SUM(amountMl), 0)
        FROM water_entries
        WHERE userId = :userId
        AND loggedAtMillis >= :startMillis
        AND loggedAtMillis < :endMillis
        """
    )
    fun observeTotalForDay(
        userId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<Int>
}

