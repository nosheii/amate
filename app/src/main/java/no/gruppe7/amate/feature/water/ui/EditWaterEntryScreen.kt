package no.gruppe7.amate.feature.water.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import no.gruppe7.amate.R

// Samme felt glir frem både ved ny mengde og under en registrering som redigeres.
@Composable
fun InlineWaterEditor(
    state: WaterUiState,
    onAmountChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
            .testTag("water_inline_editor"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.editingEntry != null) {
            HorizontalDivider()
        }
        Text(
            stringResource(if (state.editingEntry == null) R.string.water_add_title else R.string.water_edit_title),
            style = MaterialTheme.typography.titleSmall,
        )
        WaterAmountField(state, onAmountChange)
        state.errorMessageResId?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            TextButton(onClick = onCancel, enabled = !state.isSaving) {
                Text(stringResource(R.string.water_cancel))
            }
            Button(onClick = onSave, enabled = !state.isSaving) {
                Text(stringResource(if (state.isSaving) R.string.water_saving else R.string.water_save))
            }
        }
    }
}

// Samme inndata og feilmeldinger for ny mengde og redigering.
@Composable
private fun WaterAmountField(state: WaterUiState, onAmountChange: (String) -> Unit) {
    OutlinedTextField(
        value = state.amountInput,
        onValueChange = onAmountChange,
        label = { Text(stringResource(R.string.water_amount_label)) },
        placeholder = { Text(stringResource(R.string.water_amount_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        enabled = !state.isSaving,
        isError = state.inputErrorResId != null,
        supportingText = {
            state.inputErrorResId?.let { Text(stringResource(it)) }
        },
        modifier = Modifier.fillMaxWidth().testTag("water_amount_input"),
    )
}
