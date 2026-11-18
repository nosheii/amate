package no.gruppe7.amate.feature.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import no.gruppe7.amate.R

// Samme nøkkel gir samme bilde på profilen og hjem. Gamle nøkler viser katten.
@Composable
internal fun AvatarImage(avatarKey: String, modifier: Modifier = Modifier) {
    val selected = avatarOptions.firstOrNull { it.key == avatarKey } ?: avatarOptions.first()
    Image(
        painter = painterResource(selected.imageRes),
        contentDescription = stringResource(R.string.profile_selected_avatar, stringResource(selected.nameRes)),
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
