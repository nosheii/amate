package no.gruppe7.amate.feature.tasks.data.local

/** TODO: Legg til Room Entity og felt for planlegging/fullforing. */
data class SelfCareTaskEntity(
    val id: Long = 0,
    val userId: Long = 0,
    val title: String = "",
    val isCompleted: Boolean = false,
)
