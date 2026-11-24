package no.gruppe7.amate.feature.profile.ui

data class ProfileUiState(
    val userId: Long =1,
    val displayName: String = "",
    val nameError: Boolean = false,
    val age: Int? = null,
    val isLoading: Boolean = true,
    val avatarKey: String = "",
    //Brukes til å vise "Lagret" knappen
    //true etter at profilen er lagret
    val saved: Boolean = false,
)
