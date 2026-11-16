package no.gruppe7.amate.feature.profile.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import no.gruppe7.amate.R

private val Cream = Color(0xFFFFFBF1)
private val Moss = Color(0xFF617D42)

internal data class AvatarOption(
    val key: String,
    @param:DrawableRes val imageRes: Int,
    @param:StringRes val nameRes: Int,
)

// Vi lagrer fortsatt bare nøkkelen i Room. Bildene følger med appen, så de virker uten nett.
// «bjørn» beholder skrivemåten fra databasen, selv om filnavnet må bruke vanlig o.
internal val avatarOptions = listOf(
    AvatarOption("katt", R.drawable.avatar_katt, R.string.profile_avatar_cat),
    AvatarOption("bjørn", R.drawable.avatar_bjorn, R.string.profile_avatar_bear),
    AvatarOption("hund", R.drawable.avatar_hund, R.string.profile_avatar_dog),
    AvatarOption("frosk", R.drawable.avatar_frosk, R.string.profile_avatar_frog),
    AvatarOption("kanin", R.drawable.avatar_kanin, R.string.profile_avatar_rabbit),
)


// Samme velger brukes ved oppstart og i profilen. Ingen kopi av bildene eller nye avatarnøkler.
@Composable
internal fun AvatarPicker(selectedKey: String, onSelected: (String) -> Unit, enabled: Boolean = true) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        avatarOptions.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { option ->
                    AvatarTile(option, option.key == selectedKey, { if (enabled) onSelected(option.key) },
                        Modifier.weight(1f), enabled)
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AvatarTile(
    option: AvatarOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            // selectable gir både klikk og beskjed til skjermleseren om hvilket dyr som er valgt.
            .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .testTag("profile_avatar_${option.key}")
            .background(color = Cream, shape = RoundedCornerShape(16.dp))
            .border(
                width = 2.dp,
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) Moss else Color.Transparent
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(option.imageRes),
            // Navnet leses fra teksten under, så skjermleseren slipper å si det to ganger.
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Text(
            text = stringResource(option.nameRes),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
