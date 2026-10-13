package no.gruppe7.amate.feature.achievements.data.local

/** TODO: Legg til Room Entity og stabil achievement-kode. */
data class AchievementEntity(
    val id: Long = 0,
    val code: String = "",
    val title: String = "",
    val threshold: Int = 0,
)
