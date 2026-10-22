package no.gruppe7.amate.feature.steps.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface StepsDao {
    // observerer step raden med en gang og deretter på nytt hver gang den endres.
    // For å holde den oppdatert
    @Query("SELECT * FROM daily_steps WHERE userId = :userId AND epochDay = :epochDay")
    fun getStepsForDay(userId: Long, epochDay: Long): Flow<DailyStepsEntity?>
    // leser step raden en gang
    @Query("SELECT * FROM daily_steps WHERE userId = :userId AND epochDay = :epochDay")
    suspend fun getStepsForDayOneShot(userId: Long, epochDay: Long): DailyStepsEntity?
    // Setter inn raden, eller oppdaterer den hvis en rad med samme primærnøkkel allerede finnes.
    @Upsert
    suspend fun upsertSteps(entity: DailyStepsEntity)
    // ser alle registrerte dager, starter ved den nyeste
    @Query("SELECT * FROM daily_steps WHERE userId = :userId ORDER BY epochDay DESC")
    fun getStepsHistory(userId: Long): Flow<List<DailyStepsEntity>>
}
