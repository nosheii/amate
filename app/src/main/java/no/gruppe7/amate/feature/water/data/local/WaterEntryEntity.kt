package com.example.amate.feature.water.data.local

/** TODO: Legg til Room Entity-annotasjon og endelige felt. */
data class WaterEntryEntity(
    val id: Long = 0,
    val userId: Long = 0,
    val amountMl: Int = 0,
    val loggedAtMillis: Long = 0,
)
