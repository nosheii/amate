package no.gruppe7.amate.feature.steps.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
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
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(entity: DailyStepsEntity)

    @Query("UPDATE daily_steps SET stepCount = stepCount + :count WHERE userId = :userId AND epochDay = :epochDay")
    suspend fun addCount(userId: Long, epochDay: Long, count: Int)

    @Transaction
    suspend fun increment(userId: Long, epochDay: Long, count: Int) {
        require(userId > 0 && count > 0)
        insertIfMissing(DailyStepsEntity(userId, epochDay, 0))
        addCount(userId, epochDay, count)
    }

    // ser alle registrerte dager, starter ved den nyeste
    @Query("SELECT * FROM daily_steps WHERE userId = :userId ORDER BY epochDay DESC")
    fun getStepsHistory(userId: Long): Flow<List<DailyStepsEntity>>
}
