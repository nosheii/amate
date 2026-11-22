package no.gruppe7.amate.feature.achievements.data

import kotlinx.coroutines.flow.Flow
import no.gruppe7.amate.feature.achievements.data.local.AchievementDao
import no.gruppe7.amate.feature.achievements.data.local.UserAchievementEntity

// Det ViewModel-en får bruke. Skjuler Room for resten av appen.
interface AchievementRepository {

    // Merkene brukeren har tatt, med dato.
    fun observeEarned(userId: Long): Flow<List<UserAchievementEntity>>

    // Deler ut merker. Merker som allerede er tatt, beholder sin opprinnelige dato.
    suspend fun award(userId: Long, codes: Set<String>, earnedAtMillis: Long)
}

class RoomAchievementRepository(
    private val achievementDao: AchievementDao,
) : AchievementRepository {

    override fun observeEarned(userId: Long) = achievementDao.observeEarned(userId)

    override suspend fun award(userId: Long, codes: Set<String>, earnedAtMillis: Long) {
        if (codes.isEmpty()) return
        achievementDao.insertAll(codes.map { UserAchievementEntity(userId, it, earnedAtMillis) })
    }
}
