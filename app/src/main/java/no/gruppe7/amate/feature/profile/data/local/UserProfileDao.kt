package no.gruppe7.amate.feature.profile.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Upsert
    suspend fun upsert(profile: UserProfileEntity): Long

    @Query("SELECT * FROM user_profile WHERE userId = :userId")
    fun observeProfile(userId: Long): Flow<UserProfileEntity?>
}
