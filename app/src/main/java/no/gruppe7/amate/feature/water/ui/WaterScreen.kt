package no.gruppe7.amate.feature.water.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.water.data.WaterRepository
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity

// Fargene gjelder bare vannprototypen. Gruppens felles tema kan kobles på senere.
private val WaterColors = lightColorScheme(
    primary = Color(0xFFAD590B), onPrimary = Color.White,
    secondary = Color(0xFF477491), background = Color(0xFFF6F2E8),
    surface = Color(0xFFFFFCF5), onSurface = Color(0xFF43372C),
    onBackground = Color(0xFF43372C), onSurfaceVariant = Color(0xFF71685E),
    outline = Color(0xFFBCB2A4),
)

@Composable
fun WaterRoute(repository: WaterRepository, modifier: Modifier = Modifier) {
    val factory = remember(repository) { WaterViewModel.factory(repository) }
    val model: WaterViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshDay() }

    MaterialTheme(colorScheme = WaterColors) {
        WaterScreen(
            state = state,
            onQuickAdd = model::addQuick,
            onAddCustom = { model.openEditor() },
            onEdit = model::openEditor,
            onDelete = model::askToDelete,
            onRetry = model::retry,
            onDismissError = model::dismissError,
            onAmountChange = model::changeAmount,
            onSaveEdit = model::saveEditor,
            onCancelEdit = model::closeEditor,
            modifier = modifier,
        )
        state.pendingDeletion?.let { entry ->
            AlertDialog(
                onDismissRequest = model::cancelDelete,
                title = { Text(stringResource(R.string.water_delete_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.water_delete_body, entry.amountMl))
                        state.errorMessageResId?.let {
                            Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = model::confirmDelete, enabled = !state.isSaving) {
                        Text(stringResource(R.string.water_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = model::cancelDelete, enabled = !state.isSaving) {
                        Text(stringResource(R.string.water_cancel))
                    }
                },
            )
        }
    }
}

// Denne delen viser bare data og sender knappetrykk tilbake til ViewModel.
@Composable
fun WaterScreen(
    state: WaterUiState,
    onQuickAdd: (Int) -> Unit,
    onAddCustom: () -> Unit,
    onEdit: (WaterEntryEntity) -> Unit,
    onDelete: (WaterEntryEntity) -> Unit,
    onRetry: () -> Unit,
    onDismissError: () -> Unit,
    onAmountChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.forLanguageTag("nb-NO")
    val numbers = remember { NumberFormat.getIntegerInstance(locale) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE d. MMMM", locale) }
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm", locale) }
    val enabled = !state.isSaving && !state.isLoading && !state.loadFailed
    val isAdding = state.isEditorOpen && state.editingEntry == null
    val addPosition = remember { BringIntoViewRequester() }
    LaunchedEffect(isAdding) {
        if (isAdding) {
            delay(300)
            addPosition.bringIntoView()
        }
    }

    Box(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("water_list"),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(44.dp).background(Color(0xFFE2E8D4), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("a", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF52683B))
                    }
                    Column {
                        Text(stringResource(R.string.water_brand), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(state.date.format(dateFormat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.water_heading), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.water_intro), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            WaterDrop()
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.water_today), style = MaterialTheme.typography.titleMedium)
                                if (state.isLoading) {
                                    val description = stringResource(R.string.water_loading)
                                    CircularProgressIndicator(Modifier.padding(top = 8.dp).size(32.dp).semantics { contentDescription = description })
                                } else if (!state.loadFailed) {
                                    Text(
                                        stringResource(R.string.water_amount, numbers.format(state.totalTodayMl)),
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.testTag("water_total"),
                                    )
                                }
                            }
                        }
                        if (!state.loadFailed) {
                            val progressDescription = stringResource(R.string.water_progress, (state.progress * 100).toInt())
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth().height(10.dp).semantics { contentDescription = progressDescription },
                                color = Color(0xFF5383A1), trackColor = Color(0xFFE1EAF0),
                            )
                            Text(stringResource(R.string.water_display_goal, numbers.format(state.displayGoalMl)), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.water_goal_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            state.errorMessageResId?.let { error ->
                item {
                    Column {
                        Text(stringResource(error), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = if (state.loadFailed) onRetry else onDismissError) {
                            Text(stringResource(if (state.loadFailed) R.string.water_retry else R.string.water_dismiss_error))
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { onQuickAdd(250) }, enabled = enabled,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text(stringResource(R.string.water_quick_250)) }
                        OutlinedButton(
                            onClick = { onQuickAdd(500) }, enabled = enabled,
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) { Text(stringResource(R.string.water_quick_500)) }
                    }
                    TextButton(onClick = onAddCustom, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.water_custom))
                    }
                    // Egen mengde åpnes rett under knappen, med samme felt som redigering.
                    AnimatedVisibility(
                        visible = isAdding,
                        enter = expandVertically(expandFrom = Alignment.Top) +
                            slideInVertically { -it / 4 } + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth().bringIntoViewRequester(addPosition),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            InlineWaterEditor(
                                state = state,
                                onAmountChange = onAmountChange,
                                onSave = onSaveEdit,
                                onCancel = onCancelEdit,
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.water_history), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (!state.isLoading && !state.loadFailed) {
                        Text(pluralStringResource(R.plurals.water_count, state.entries.size, state.entries.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (!state.isLoading && !state.loadFailed && state.entries.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE9ECDD)), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.water_empty_title), fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.water_empty_body), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            items(state.entries, key = { it.id }) { entry ->
                val isEditing = state.isEditorOpen && state.editingEntry?.id == entry.id
                val editorPosition = remember { BringIntoViewRequester() }
                LaunchedEffect(isEditing) {
                    if (isEditing) {
                        // Lar feltet gli ut før vi sørger for at det er synlig i listen.
                        delay(300)
                        editorPosition.bringIntoView()
                    }
                }
                val editDescription = stringResource(R.string.water_edit_description, entry.amountMl)
                val deleteDescription = stringResource(R.string.water_delete_description, entry.amountMl)
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("water_entry_${entry.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.water_amount, numbers.format(entry.amountMl)), fontWeight = FontWeight.Bold)
                            Text(Instant.ofEpochMilli(entry.loggedAtMillis).atZone(state.zoneId).format(timeFormat), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onEdit(entry) }, enabled = enabled, modifier = Modifier.semantics { contentDescription = editDescription }) {
                            Text(stringResource(R.string.water_edit))
                        }
                        TextButton(
                            onClick = { onDelete(entry) }, enabled = enabled,
                            modifier = Modifier.semantics { contentDescription = deleteDescription },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text(stringResource(R.string.water_delete)) }
                    }
                    // Kortet vokser nedover. Resten av listen skyves rolig ned.
                    AnimatedVisibility(
                        visible = isEditing,
                        enter = expandVertically(expandFrom = Alignment.Top) +
                            slideInVertically { -it / 4 } + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                    ) {
                        InlineWaterEditor(
                            state = state,
                            onAmountChange = onAmountChange,
                            onSave = onSaveEdit,
                            onCancel = onCancelEdit,
                            modifier = Modifier.bringIntoViewRequester(editorPosition),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterDrop() {
    Box(Modifier.size(64.dp).background(Color(0xFFDFEDF4), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(32.dp, 40.dp)) {
            // En enkel dråpe tegnet med kode, så prototypen ikke trenger bildefiler.
            val drop = Path().apply {
                moveTo(size.width / 2, 0f)
                cubicTo(size.width, size.height * .5f, size.width, size.height, size.width / 2, size.height)
                cubicTo(0f, size.height, 0f, size.height * .5f, size.width / 2, 0f)
                close()
            }
            drawPath(drop, Color(0xFF5383A1))
        }
    }
}

@Preview(showBackground = true, locale = "nb")
@Composable
private fun WaterScreenPreview() {
    MaterialTheme(colorScheme = WaterColors) {
        WaterScreen(
            state = WaterUiState(isLoading = false),
            onQuickAdd = {}, onAddCustom = {}, onEdit = {}, onDelete = {},
            onRetry = {}, onDismissError = {},
            onAmountChange = {}, onSaveEdit = {}, onCancelEdit = {},
        )
    }
}
