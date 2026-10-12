package no.gruppe7.amate.feature.steps.data.local

/** TODO: Legg til Room Entity med sammensatt nokkel for bruker og dato. */
data class DailyStepsEntity(
    val userId: Long = 0,
    val epochDay: Long = 0,
    val stepCount: Int = 0,
)
