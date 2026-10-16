package no.gruppe7.amate.feature.water.ui

import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import java.time.LocalDate
import java.time.ZoneId

// Alt skjermen trenger å vise. Room er kilden til listen og totalsummen.
data class WaterUiState(
    val entries: List<WaterEntryEntity> = emptyList(),
    val date: LocalDate = LocalDate.now(),
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val errorMessageResId: Int? = null,
    val isEditorOpen: Boolean = false,
    val editingEntry: WaterEntryEntity? = null,
    val amountInput: String = "",
    val inputErrorResId: Int? = null,
    val pendingDeletion: WaterEntryEntity? = null,
    // Bare et visningsmål frem til gruppens mål-funksjon er koblet på.
    val displayGoalMl: Int = 2_000,
) {
    // Listen og summen følger samme utsending fra Room.
    val totalTodayMl: Long get() = entries.sumOf { it.amountMl.toLong() }
    val progress: Float get() = (totalTodayMl.toFloat() / displayGoalMl).coerceIn(0f, 1f)
}
