package no.gruppe7.amate.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.gruppe7.amate.feature.profile.data.ProfileRepository
import no.gruppe7.amate.feature.profile.data.local.AvatarConfigEntity
import no.gruppe7.amate.feature.profile.data.local.UserProfileEntity


/** TODO: Koble ProfileRepository til ProfileUiState. */

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val userId: Long = 1
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState(userId = userId))
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        //oppdaterer navn og alder automatisk når databasen endrer seg.
        viewModelScope.launch {
            profileRepository.observeProfile(userId).collect { profile ->
                _uiState.update {
                    it.copy(
                        displayName = profile?.displayName ?: "",
                        age = profile?.age,
                        isLoading = false
                    )
                }
            }
        }

        //Lytter på avataren separat siden den ligger i en annen tabell
        viewModelScope.launch {
            profileRepository.observeAvatar(userId).collect { avatar ->
                _uiState.update {
                    it.copy(avatarKey = avatar?.avatarKey ?: "")
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    fun onAgeChanged(age: Int?) {
        _uiState.update { it.copy(age = age)}
    }

    fun onAvatarSelected(avatarKey: String) {
        _uiState.update { it.copy(avatarKey = avatarKey) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            profileRepository.saveProfile(
                UserProfileEntity(
                    userId = state.userId,
                    displayName = state.displayName,
                    age = state.age
                )
            )
            profileRepository.saveAvatar(
                AvatarConfigEntity(
                    userId = state.userId,
                    avatarKey = state.avatarKey
                )
            )
        }
    }

    companion object {
        fun factory(profileRepository: ProfileRepository) = viewModelFactory {
            initializer { ProfileViewModel(profileRepository)}
        }
    }
}