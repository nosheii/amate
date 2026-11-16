package no.gruppe7.amate.feature.profile.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.gruppe7.amate.R

private val Cream = Color(0xFFFFFBF1)
private val Moss = Color(0xFF617D42)

// Finner riktig avatar-data fra den lagrede nøkkelen
// Tom eller gammel nøkkel (f.eks. elefant) viser katten. Vi endrer ikke databasen av den grunn.
private fun optionFor(key: String): AvatarOption =
    avatarOptions.firstOrNull { it.key == key } ?: avatarOptions.first()

@Composable
fun ProfileRoute(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        onAvatarSelected = viewModel::onAvatarSelected,
        onAgeChanged = viewModel::onAgeChanged,
        onNameChanged = viewModel::onNameChanged,
        onSaveClick = viewModel::saveProfile
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAgeChanged: (Int?) -> Unit,
    onNameChanged: (String) -> Unit,
    onAvatarSelected: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                // Hele siden scroller, ikke bare avatarene. Da kan vi nå Lagre på små skjermer også.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("profile_list"),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(R.string.profile_title),
                fontWeight = FontWeight.ExtraBold,
                color = Moss,
                style = MaterialTheme.typography.titleLarge
            )
            // Samme bilde brukes i forhåndsvisningen og i valgene under.
            val selected = optionFor(uiState.avatarKey)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(selected.imageRes),
                    contentDescription = stringResource(R.string.profile_selected_avatar, stringResource(selected.nameRes)),
                    // Fit viser hele tegningen uten å kutte av ører eller boksen under dyret.
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(160.dp).testTag("profile_avatar_preview"),
                )
            }

            ProfileCard {
                Text(
                    text = stringResource(R.string.profile_choose_avatar),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                AvatarPicker(selectedKey = selected.key, onSelected = onAvatarSelected)

            }

            ProfileCard {

                OutlinedTextField(
                    value = uiState.age?.toString() ?: "",
                    onValueChange = { input ->
                        onAgeChanged(if (input.isBlank()) null else input.toIntOrNull())
                    },
                    label = { Text(stringResource(R.string.profile_age)) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("profile_age"),
                    colors = fieldColors()
                )

                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = onNameChanged,
                    label = { Text(stringResource(R.string.profile_name)) },
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag("profile_name")
                )
                Button(
                    onClick = onSaveClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("profile_save"),
                    colors = ButtonDefaults.buttonColors(containerColor = Moss),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text(stringResource(R.string.profile_save))
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Moss,
    cursorColor = Moss
)

@Composable
private fun ProfileCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Cream),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            content()
        }
    }
}
