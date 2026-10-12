package com.example.amate.feature.reminders.data.local

/** TODO: Legg til Room Entity for brukerens lokale paminnelser. */
data class ReminderEntity(
    val id: Long = 0,
    val userId: Long = 0,
    val type: String = "",
    val enabled: Boolean = true,
)
