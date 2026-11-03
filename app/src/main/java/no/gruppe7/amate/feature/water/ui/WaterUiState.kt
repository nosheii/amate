package no.gruppe7.amate.feature.water.ui

import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import java.time.LocalDate
import java.time.ZoneId
import no.gruppe7.amate.R

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
    // null betyr at brukeren ikke har satt et vannmål. Jeg finner ikke på et standardmål.
    val goalMl: Int? = null,
    val isGoalLoading: Boolean = true,
    val goalLoadFailed: Boolean = false,
) {
    // Listen og summen følger samme utsending fra Room.
    val totalTodayMl: Long get() = entries.sumOf { it.amountMl.toLong() }
    // Lagringsfeilen hører til feltet eller slettedialogen når de er åpne, ellers til listen.
    // En lesefeil skal aldri skjules bare fordi brukeren holder på å redigere.
    val listErrorMessageResId: Int? get() = if (loadFailed) R.string.water_load_error else {
        errorMessageResId.takeIf { !isEditorOpen && pendingDeletion == null }
    }
    // Måleren vises bare når både vannmengden og et positivt mål faktisk er hentet.
    // Da unngår jeg både deling på null og å vise gamle tall som om de var oppdaterte.
    val progress: Float? get() = goalMl?.takeIf {
        it > 0 && !isLoading && !loadFailed && !isGoalLoading && !goalLoadFailed
    }?.let { (totalTodayMl.toFloat() / it).coerceIn(0f, 1f) }
}
