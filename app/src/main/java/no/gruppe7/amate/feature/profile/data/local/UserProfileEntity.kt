package no.gruppe7.amate.feature.profile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate =true)
    val userId: Long = 0,
    val displayName: String = "",
    // Gjør alder valgfritt
    val age: Int? = null
)