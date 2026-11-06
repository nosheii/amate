package no.gruppe7.amate.feature.tasks.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.goals.ui.InlineValueEditor

// Gjenbruker redigeringsfeltet fra målskjermen for å holde skjemaene like.
@Composable
fun InlineTaskEditor (
    state: TasksUiState,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InlineValueEditor(
        // Skjemaoverskriften viser om brukeren oppretter eller endrer et gjøremål.
        title = stringResource(if (state.editingTask == null) R.string.tasks_add_title else R.string.tasks_edit_title),
        label = stringResource(R.string.tasks_title_label),
        value = state.titleInput,
        onValueChange = onTitleChange,
        onSave = onSave,
        onCancel = onCancel,
        isSaving = state.isSaving,
        // Gjør ressurs-ID-ene i UI-tilstanden om til lokaliserte tekster for feltet.
        inputError = state.inputErrorResId?.let { stringResource(it)},
        generalError = state.errorMessageResId?.let { stringResource(it) },

        keyboardType = KeyboardType.Text,
        modifier = modifier,
    )
}
