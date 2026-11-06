package no.gruppe7.amate.feature.tasks.ui

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.format.DateTimeFormatter
import java.util.Locale
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.tasks.data.TaskRepository
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity
import no.gruppe7.amate.ui.theme.AmateTheme

// Kobler ViewModel til skjermen. Det er denne navigasjonen skal vise.
@Composable
fun TasksRoute(repository: TaskRepository, modifier: Modifier = Modifier) {
    val factory = remember(repository) { TasksViewModel.factory(repository) }
    val model: TasksViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    // Sjekker om det har blitt ny dag hver gang skjermen vises igjen.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshDay() }

    TasksScreen(
        state = state,
        onAdd = { model.openEditor() },
        onEdit = model::openEditor,
        onToggle = model::toggleCompleted,
        onDelete = model::deleteTask,
        onTitleChange = model::changeTitle,
        onSave = model::saveEditor,
        onCancel = model::closeEditor,
        onRetry = model::retry,
        onDismissError = model::dismissError,
        modifier = modifier,
    )
}

// Viser bare data og sender knappetrykk tilbake til ViewModel.
@Composable
fun TasksScreen(
    state: TasksUiState,
    onAdd: () -> Unit,
    onEdit: (SelfCareTaskEntity) -> Unit,
    onToggle: (SelfCareTaskEntity) -> Unit,
    onDelete: (SelfCareTaskEntity) -> Unit,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Norsk dato, f.eks. "fredag 30. oktober".
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE d. MMMM", Locale.forLanguageTag("nb-NO")) }
    val enabled = !state.isSaving && !state.isLoading && !state.loadFailed
    val isAdding = state.isEditorOpen && state.editingTask == null

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().imePadding().testTag("tasks_list"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.tasks_title),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(state.date.format(dateFormat), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Fremdrift: "2 av 5 gjort" med en stolpe. Vises bare når listen er lastet.
        if (!state.isLoading && !state.loadFailed && state.tasks.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (state.completedCount == state.tasks.size) stringResource(R.string.tasks_all_done)
                            else "${state.completedCount} ${stringResource(R.string.tasks_progress_of)} ${state.tasks.size} ${stringResource(R.string.tasks_progress_done)}",
                            fontWeight = FontWeight.Bold, modifier = Modifier.testTag("tasks_progress"),
                        )
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                        )
                    }
                }
            }
        }
        if (state.isLoading) {
            item {
                val description = stringResource(R.string.tasks_loading)
                CircularProgressIndicator(Modifier.size(32.dp).semantics { contentDescription = description })
            }
        }
        // Feil vises her bare når feltet er lukket. Når det er åpent, viser feltet feilen selv.
        state.errorMessageResId?.takeIf { !state.isEditorOpen }?.let { error ->
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
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onAdd, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("tasks_add"),
                        shape = RoundedCornerShape(16.dp)) {
                        Text(stringResource(R.string.tasks_add))
                    }
                    // Nytt gjøremål skrives rett under knappen, som egen mengde i vann.
                    AnimatedVisibility(
                        visible = isAdding,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            InlineTaskEditor(state, onTitleChange, onSave, onCancel, Modifier.padding(top = 16.dp))
                        }
                    }
                }
            }
            if (!state.isLoading && state.tasks.isEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.tasks_empty_title), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.tasks_empty_body), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            // key gjør at Compose kjenner igjen kortene når listen oppdateres.
            items(state.tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    state = state,
                    enabled = enabled,
                    onToggle = { onToggle(task) },
                    onEdit = { onEdit(task) },
                    onDelete = { onDelete(task) },
                    onTitleChange = onTitleChange,
                    onSave = onSave,
                    onCancel = onCancel,
                )
            }
        }
    }
}

// Ett kort per gjøremål: avkrysning, tittel, Rediger/Slett, og feltet som glir ned ved redigering.
@Composable
private fun TaskCard(
    task: SelfCareTaskEntity,
    state: TasksUiState,
    enabled: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val isEditing = state.isEditorOpen && state.editingTask?.id == task.id
    val toggleDescription = stringResource(R.string.tasks_toggle_description, task.title)
    val editDescription = stringResource(R.string.tasks_edit_description, task.title)
    val deleteDescription = stringResource(R.string.tasks_delete_description, task.title)

    Card(
        modifier = Modifier.fillMaxWidth().testTag("task_${task.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.isCompleted, onCheckedChange = { onToggle() }, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = toggleDescription },
            )
            Text(
                task.title,
                modifier = Modifier.weight(1f),
                // Gjennomstreking i stedet for å fjerne ferdige, så dagen føles fullført.
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = onEdit, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = editDescription }) {
                Text(stringResource(R.string.tasks_edit))
            }
            TextButton(onClick = onDelete, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = deleteDescription },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.tasks_delete))
            }
        }
        AnimatedVisibility(
            visible = isEditing,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            InlineTaskEditor(state, onTitleChange, onSave, onCancel)
        }
    }
}

@Preview(showBackground = true, locale = "nb")
@Composable
private fun TasksScreenPreview() {
    AmateTheme {
        TasksScreen(
            state = TasksUiState(
                isLoading = false,
                tasks = listOf(
                    SelfCareTaskEntity(id = 1, userId = 1, title = "Gå en tur", scheduledForMillis = 0),
                    SelfCareTaskEntity(id = 2, userId = 1, title = "Ring mamma", scheduledForMillis = 0, isCompleted = true),
                ),
            ),
            onAdd = {}, onEdit = {}, onToggle = {}, onDelete = {}, onTitleChange = {},
            onSave = {}, onCancel = {}, onRetry = {}, onDismissError = {},
        )
    }
}