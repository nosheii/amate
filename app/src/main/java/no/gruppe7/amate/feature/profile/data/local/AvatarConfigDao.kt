package no.gruppe7.amate.feature.profile.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarConfigDao {
    // Lagrer og oppdaterer avatar
    @Upsert
    suspend fun upsert(config: AvatarConfigEntity)

    // Returnerer null dersom ikke valgt avatar
    @Query("SELECT * FROM avatar_config WHERE userId = :userId")
    fun observeAvatarConfig(userId: Long): Flow<AvatarConfigEntity?>

}