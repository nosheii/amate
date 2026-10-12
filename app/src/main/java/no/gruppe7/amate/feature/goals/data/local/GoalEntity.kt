package no.gruppe7.amate.feature.goals.data.local

/** TODO: Legg til Room Entity og en tydelig maltype. */
data class GoalEntity(
    val id: Long = 0,
    val userId: Long = 0,
    val type: String = "",
    val targetValue: Int = 0,
    val isActive: Boolean = true,
)
