package no.gruppe7.amate.feature.profile.data

import kotlinx.coroutines.flow.Flow
import no.gruppe7.amate.feature.profile.data.local.AvatarConfigEntity
import no.gruppe7.amate.feature.profile.data.local.AvatarConfigDao
import no.gruppe7.amate.feature.profile.data.local.UserProfileEntity
import no.gruppe7.amate.feature.profile.data.local.UserProfileDao

//Interfacet er det ViewModel-en får bruke
//Skjuler Room, så Viewmodel ikke vet hvor dataene kommer fra.
interface ProfileRepository {
    fun observeProfile(userId: Long): Flow<UserProfileEntity?>
    suspend fun saveProfile(profile: UserProfileEntity): Long

    fun observeAvatar(userId: Long): Flow<AvatarConfigEntity?>
    suspend fun saveAvatar(avatar: AvatarConfigEntity)
}

class RoomProfileRepository(
    private val userProfileDao: UserProfileDao,
    private val avatarConfigDao: AvatarConfigDao
) : ProfileRepository {

override fun observeProfile(userId: Long) = userProfileDao.observeProfile(userId)

override suspend fun saveProfile(profile: UserProfileEntity) =
    userProfileDao.upsert(profile)

override fun observeAvatar(userId: Long) = avatarConfigDao.observeAvatarConfig(userId)

override suspend fun saveAvatar(avatar: AvatarConfigEntity) =
        avatarConfigDao.upsert(avatar)
}