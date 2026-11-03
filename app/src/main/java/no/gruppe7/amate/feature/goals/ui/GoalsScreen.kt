package no.gruppe7.amate.feature.goals.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.ui.theme.AmateTheme

// Tekster per måltype. Ny måltype = tre linjer her pluss strenger.
internal val GoalType.labelRes: Int
    get() = when (this) {
        GoalType.WATER -> R.string.goals_water
        GoalType.STEPS -> R.string.goals_steps
    }

internal val GoalType.inputLabelRes: Int
    get() = when (this) {
        GoalType.WATER -> R.string.goals_value_label_water
        GoalType.STEPS -> R.string.goals_value_label_steps
    }

internal val GoalType.valueFormatRes: Int
    get() = when (this) {
        GoalType.WATER -> R.string.goals_water_value
        GoalType.STEPS -> R.string.goals_steps_value
    }

// Kobler ViewModel til skjermen. Navigasjonen viser denne.
@Composable
fun GoalsRoute(repository: GoalRepository, modifier: Modifier = Modifier) {
    val factory = remember(repository) { GoalsViewModel.factory(repository) }
    val model: GoalsViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()

    GoalsScreen(
        state = state,
        onEdit = model::openEditor,
        onDelete = model::deleteGoal,
        onValueChange = model::changeValue,
        onSave = model::saveEditor,
        onCancel = model::closeEditor,
        onRetry = model::retry,
        onDismissError = model::dismissError,
        modifier = modifier,
    )
}

// Viser bare data og sender knappetrykk tilbake til ViewModel.
@Composable
fun GoalsScreen(
    state: GoalsUiState,
    onEdit: (GoalType) -> Unit,
    onDelete: (GoalEntity) -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numbers = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("nb-NO")) }
    val enabled = !state.isSaving && !state.isLoading && !state.loadFailed

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().imePadding().testTag("goals_list"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.goals_title),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.goals_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.isLoading) {
            item {
                val description = stringResource(R.string.goals_loading)
                CircularProgressIndicator(Modifier.size(32.dp).semantics { contentDescription = description })
            }
        }
        // Feil vises her bare når feltet er lukket. Når det er åpent, viser feltet feilen selv.
        state.errorMessageResId?.takeIf { state.editingType == null }?.let { error ->
            item {
                Column {
                    Text(stringResource(error), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = if (state.loadFailed) onRetry else onDismissError) {
                        Text(stringResource(if (state.loadFailed) R.string.common_retry else R.string.common_dismiss_error))
                    }
                }
            }
        }
        if (!state.loadFailed) {
            items(GoalType.entries, key = { it.name }) { type ->
                GoalCard(
                    type = type,
                    goal = state.goalFor(type),
                    state = state,
                    enabled = enabled,
                    numbers = numbers,
                    onEdit = { onEdit(type) },
                    onDelete = onDelete,
                    onValueChange = onValueChange,
                    onSave = onSave,
                    onCancel = onCancel,
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    type: GoalType,
    goal: GoalEntity?,
    state: GoalsUiState,
    enabled: Boolean,
    numbers: NumberFormat,
    onEdit: () -> Unit,
    onDelete: (GoalEntity) -> Unit,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val isEditing = state.editingType == type
    val label = stringResource(type.labelRes)
    val editDescription = stringResource(R.string.goals_edit_description, label)

    Card(
        modifier = Modifier.fillMaxWidth().testTag("goal_card_${type.name}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (goal == null) stringResource(R.string.goals_not_set)
                    else stringResource(type.valueFormatRes, numbers.format(goal.targetValue)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("goal_value_${type.name}"),
                )
            }
            if (goal != null) {
                TextButton(
                    onClick = { onDelete(goal) }, enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.goals_remove)) }
            }
            TextButton(
                onClick = onEdit, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = editDescription },
            ) { Text(stringResource(if (goal == null) R.string.goals_set else R.string.goals_edit)) }
        }
        // Kortet vokser nedover når feltet åpnes, som i vannskjermen.
        AnimatedVisibility(
            visible = isEditing,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            InlineValueEditor(
                title = stringResource(R.string.goals_edit_title, label),
                label = stringResource(type.inputLabelRes),
                value = state.valueInput,
                onValueChange = onValueChange,
                onSave = onSave,
                onCancel = onCancel,
                isSaving = state.isSaving,
                inputError = state.inputErrorResId?.let { stringResource(it) },
                generalError = state.errorMessageResId?.let { stringResource(it) },
            )
        }
    }
}

@Preview(showBackground = true, locale = "nb")
@Composable
private fun GoalsScreenPreview() {
    AmateTheme {
        GoalsScreen(
            state = GoalsUiState(
                isLoading = false,
                goals = listOf(GoalEntity(id = 1, userId = 1, type = GoalType.WATER, targetValue = 2000)),
            ),
            onEdit = {}, onDelete = {}, onValueChange = {}, onSave = {}, onCancel = {},
            onRetry = {}, onDismissError = {},
        )
    }
}
