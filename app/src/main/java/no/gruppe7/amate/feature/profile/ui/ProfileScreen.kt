package no.gruppe7.amate.feature.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight

private val Cream = Color(0xFFFFFBF1)
private val Moss = Color(0xFF617D42)

private data class AvatarOption(val key: String, val color: Color)

//Midlertidige placeholdere for nå, endres senere.
private val avatarOptions = listOf(
    AvatarOption("katt", Color(0xFFFFC0CB)),
    AvatarOption("bjørn", Color(0xFFFF46A2)),
    AvatarOption("hund", Color(0xFF7D5260)),
    AvatarOption("frosk", Color(0xFFFFC5D3)),
    AvatarOption("kanin", Color(0xFF89CFF0)),
    AvatarOption("elefant", Color(0xFF8FD9FB)),
)

// Finner riktig avatar-data fra den lagrede nøkkelen
// Hvis ingenting er valgt, faller den tilbake til den første.
private fun optionFor(key: String): AvatarOption =
    avatarOptions.firstOrNull { it.key == key } ?: avatarOptions.first()

@Composable
fun ProfileRoute(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()

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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = "Min profil",
                fontWeight = FontWeight.ExtraBold,
                color = Moss,
                style = MaterialTheme.typography.titleLarge
            )
            //Forhåndsvisning av valgt avatar.
            val selected = optionFor(uiState.avatarKey)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .border(width = 3.dp, color = Moss, shape = CircleShape)
                        .background(color = selected.color, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    //Viser midlertidig avatarkey istedenfor avatar, endres laterr:
                    Text(
                        text = selected.key,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            ProfileCard {
                Text(
                    text = "Velg din avatar",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(avatarOptions) { option ->
                        AvatarCircle(
                            option = option,
                            isSelected = option.key == uiState.avatarKey,
                            onClick = { onAvatarSelected(option.key) }
                        )
                    }
                }
            }

            ProfileCard {

                OutlinedTextField(
                    value = uiState.age?.toString() ?: "",
                    onValueChange = { input ->
                        onAgeChanged(if (input.isBlank()) null else input.toIntOrNull())
                    },
                    label = { Text("Alder (valgfritt)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors()
                )

                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = onNameChanged,
                    label = { Text("Navn") },
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = onSaveClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Moss),
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Text("Lagre")
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

@Composable
private fun AvatarCircle(
    option: AvatarOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clickable(onClick = onClick)
            .background(color = option.color, shape = CircleShape)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                shape = CircleShape,
                color = if (isSelected) Moss else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.key,
            style = MaterialTheme.typography.labelMedium
        )
    }
}