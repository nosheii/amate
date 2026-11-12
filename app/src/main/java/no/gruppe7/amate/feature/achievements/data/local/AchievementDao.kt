package no.gruppe7.amate.feature.achievements.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    // Alle merkene brukeren har tatt. Flow sender ny liste når noe nytt deles ut.
    @Query("SELECT * FROM user_achievements WHERE userId = :userId ORDER BY earnedAtMillis")
    fun observeEarned(userId: Long): Flow<List<UserAchievementEntity>>

    // IGNORE: finnes raden fra før, beholdes den gamle datoen.
    // Da kan vi trygt sende inn alle oppnådde merker hver gang uten å flytte "tatt"-datoen.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(achievements: List<UserAchievementEntity>)
}
