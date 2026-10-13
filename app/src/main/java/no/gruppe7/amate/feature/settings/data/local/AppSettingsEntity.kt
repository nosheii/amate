package no.gruppe7.amate.feature.settings.data.local

/** TODO: Legg til Room Entity, eller flytt enkle preferanser til DataStore senere. */
data class AppSettingsEntity(
    val userId: Long = 0,
    val themeMode: String = "SYSTEM",
    val languageCode: String = "nb",
)
