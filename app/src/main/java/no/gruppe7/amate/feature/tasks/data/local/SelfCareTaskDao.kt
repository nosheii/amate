package no.gruppe7.amate.feature.tasks.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import no.gruppe7.amate.feature.goals.data.local.GoalEntity

// Her samles alt vi kan gjøre med gjoremålene i databasen
// Room skriver inn selve koden bak disse funksjonene for oss
@Dao
interface SelfCareTaskDao {

    @Insert
    suspend fun insert(task: SelfCareTaskEntity): Long

    @Update
    suspend fun update(task: SelfCareTaskEntity)

    @Delete
    suspend fun delete(task: SelfCareTaskEntity)

    @Query(
        """
           SELECT * FROM self_care_tasks
           WHERE userId = :userId
           AND scheduledForMillis >= :startMillis
           AND scheduledForMillis < :endMillis
           ORDER BY isCompleted ASC, id ASC
        """
    )
    fun observeTasksForDay(
        userId: Long,
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<SelfCareTaskEntity>>
}

