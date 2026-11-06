package no.gruppe7.amate.feature.profile.ui

data class ProfileUiState(
    val userId: Long = 1,
    val displayName: String = "",
    val age: Int? = null,
    val isLoading: Boolean = true,
    val avatarKey: String = "",
)
