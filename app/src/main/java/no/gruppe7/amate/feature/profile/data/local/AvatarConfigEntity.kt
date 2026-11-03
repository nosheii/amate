package no.gruppe7.amate.feature.profile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "avatar_config")
data class AvatarConfigEntity(
    @PrimaryKey
    val userId: Long = 0,
    val avatarKey: String = "",
)
