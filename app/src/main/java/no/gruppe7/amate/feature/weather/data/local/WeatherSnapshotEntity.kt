package com.example.amate.feature.weather.data.local

/** TODO: Legg til Room Entity dersom vaerdata skal caches lokalt. */
data class WeatherSnapshotEntity(
    val id: Long = 0,
    val temperatureCelsius: Double = 0.0,
    val conditionCode: String = "",
    val fetchedAtMillis: Long = 0,
)
