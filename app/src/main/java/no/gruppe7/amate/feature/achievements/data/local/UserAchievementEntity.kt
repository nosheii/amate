package no.gruppe7.amate.feature.achievements.data.local

/** TODO: Bruk bruker-ID og achievement-ID som sammensatt primarnokkel. */
data class UserAchievementEntity(
    val userId: Long = 0,
    val achievementId: Long = 0,
    val earnedAtMillis: Long = 0,
)
