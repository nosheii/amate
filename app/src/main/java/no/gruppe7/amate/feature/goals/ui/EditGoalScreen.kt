package no.gruppe7.amate.feature.goals.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import no.gruppe7.amate.R

// Et felt som glir ned inne i et kort. Generelt nok til å brukes for mål, gjøremål og annet.
// Det vet ingenting om mål. Det får bare tekster og verdier inn,
// og sender knappetrykk ut.
// Derfor kan det også brukes for gjøremål.
// (sett keyboardType = KeyboardType.Text).
@Composable
fun InlineValueEditor(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean,
    inputError: String? = null,     // feil ved selve verdien, vises under feltet
    generalError: String? = null,   // f.eks. "kunne ikke lagre"
    keyboardType: KeyboardType = KeyboardType.Number,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp).testTag("inline_editor"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Tynn strek som skiller feltet fra resten av kortet.
        HorizontalDivider()
        Text(title, style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            // Talltastatur for mål, så brukeren slipper å bytte selv.
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            // Låser feltet mens vi lagrer.
            enabled = !isSaving,
            // Rød ramme og feilmelding under feltet når inputError slår ut.
            isError = inputError != null,
            supportingText = { inputError?.let { Text(it) } },
            // S
            modifier = Modifier.fillMaxWidth().testTag("inline_editor_input"),
        )
        // Knappene ligger til høyre. Avbryt først og fortsette.
        generalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            TextButton(onClick = onCancel, enabled = !isSaving) { Text(stringResource(R.string.common_cancel)) }
            Button(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(if (isSaving) R.string.common_saving else R.string.common_save))
            }
        }
    }
}