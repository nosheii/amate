package no.gruppe7.amate.feature.tasks.ui

import java.time.LocalDate
import java.time.ZoneId
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity

// Samler data og visningstilstand som skjermen trenger i én uforanderlig verdi.
data class TasksUiState(
    val tasks: List<SelfCareTaskEntity> = emptyList(),
    val date: LocalDate = LocalDate.now(),
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val errorMessageResId: Int? = null,

    val isEditorOpen: Boolean = false,
    val editingTask: SelfCareTaskEntity? = null,
    val titleInput: String = "",
    val inputErrorResId: Int? = null,
) {
    // Beregnes fra listen, så telleren alltid følger siste databaseoppdatering.
    val completedCount: Int get() = tasks.count { it.isCompleted }
    // En tom liste gir ingen framdrift å vise.
    val progress: Float get() = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size
}
