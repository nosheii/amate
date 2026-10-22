package no.gruppe7.amate.feature.steps.data.local

import androidx.room.Entity

/** TODO: Legg til Room Entity med sammensatt nokkel for bruker og dato. */
@Entity(
    tableName = "daily_steps",
    primaryKeys = ["userId", "epochDay"],
)
data class DailyStepsEntity(
    val userId: Long = 0,
    val epochDay: Long = 0,
    val stepCount: Int = 0,
)
