package no.gruppe7.amate.feature.achievements.data.local

import androidx.room.Entity
// Én rad = ett merke brukeren har tatt. Katalogen (hvilke merker som finnes) ligger i kode,
// så databasen trenger bare å huske HVA som er tatt og NÅR.
// userId + code er sammensatt primærnøkkel: samme merke kan ikke tas to ganger.
@Entity(tableName = "user_achievements", primaryKeys = ["userId", "code"])
data class UserAchievementEntity(
    val userId: Long,
    // Stabil kode fra AchievementCatalog, f.eks. "WATER_10L. Ikke en løpende ID,
    // så katalogen kan sorteres om uten at gamle rader peker feil.
    val code: String,
    val earnedAtMillis: Long,
)
