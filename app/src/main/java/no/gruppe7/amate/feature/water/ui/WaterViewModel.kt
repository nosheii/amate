package no.gruppe7.amate.feature.water.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.water.data.MAX_ENTRY_ML
import no.gruppe7.amate.feature.water.data.WaterRepository
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity

@OptIn(ExperimentalCoroutinesApi::class)
class WaterViewModel(
    private val repository: WaterRepository,
    private val goalRepository: GoalRepository,
    // Fast lokal bruker frem til profilfunksjonen er koblet på.
    private val userId: Long = 1,
    private val clock: Clock = Clock.systemUTC(),
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
) : ViewModel() {

    private data class Day(val date: LocalDate, val zone: ZoneId)

    private fun currentDay(): Day {
        val zone = zoneProvider()
        return Day(clock.instant().atZone(zone).toLocalDate(), zone)
    }

    private val day = MutableStateFlow(currentDay())
    private val reload = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(
        WaterUiState(date = day.value.date, zoneId = day.value.zone)
    )
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(day, reload) { current, _ -> current }
                .flatMapLatest { current ->
                    // Neste datos start takler også døgn med overgang til sommertid.
                    val start = current.date.atStartOfDay(current.zone).toInstant().toEpochMilli()
                    val end = current.date.plusDays(1).atStartOfDay(current.zone).toInstant().toEpochMilli()
                    repository.observeEntriesForDay(userId, start, end)
                        .onStart {
                            _uiState.update {
                                val sameDay = it.date == current.date
                                it.copy(
                                    date = current.date, zoneId = current.zone,
                                    entries = emptyList(), isLoading = true, loadFailed = false,
                                    // Et felt for gårsdagens registrering skal ikke bli hengende på en ny dag.
                                    isEditorOpen = it.isEditorOpen && sameDay,
                                    editingEntry = if (sameDay) it.editingEntry else null,
                                    pendingDeletion = if (sameDay) it.pendingDeletion else null,
                                )
                            }
                        }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            _uiState.update {
                                // Lesefeil har sitt eget flagg. errorMessageResId brukes til lagringsfeil.
                                it.copy(isLoading = false, loadFailed = true)
                            }
                        }
                }
                .collectLatest { entries ->
                    _uiState.update { it.copy(entries = entries, isLoading = false, loadFailed = false) }
                }
        }

        viewModelScope.launch {
            // Najmas repository gir en Flow: et endret eller slettet mål sendes hit automatisk.
            // Jeg henter bare WATER for samme lokale bruker som vannregistreringene.
            reload.flatMapLatest {
                goalRepository.observeGoal(userId, GoalType.WATER)
                    .onStart { _uiState.update { it.copy(isGoalLoading = true, goalLoadFailed = false) } }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        // Målfeil stopper ikke vannloggen. Den har sin egen innlasting over.
                        _uiState.update { it.copy(goalMl = null, isGoalLoading = false, goalLoadFailed = true) }
                    }
            }.collectLatest { goal ->
                _uiState.update {
                    it.copy(
                        goalMl = goal?.takeIf { it.isActive }?.targetValue?.takeIf { it > 0 },
                        isGoalLoading = false, goalLoadFailed = false,
                    )
                }
            }
        }

        viewModelScope.launch {
            // Bytter dag også hvis appen står åpen over midnatt.
            while (true) {
                delay(30_000)
                refreshDay()
            }
        }
    }

    // Kalles også når skjermen blir synlig etter å ha vært i bakgrunnen.
    fun refreshDay() { day.value = currentDay() }

    fun retry() {
        dismissError()
        refreshDay()
        reload.value += 1
    }

    fun dismissError() { _uiState.update { it.copy(errorMessageResId = null) } }

    fun openEditor(entry: WaterEntryEntity? = null) {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(isEditorOpen = true, editingEntry = entry,
                amountInput = entry?.amountMl?.toString().orEmpty(), inputErrorResId = null,
                errorMessageResId = null, pendingDeletion = null)
        }
    }

    fun changeAmount(input: String) {
        _uiState.update { it.copy(amountInput = input, inputErrorResId = null) }
    }

    fun closeEditor() {
        if (!_uiState.value.isSaving) {
            _uiState.update { it.copy(isEditorOpen = false, editingEntry = null, inputErrorResId = null,
                errorMessageResId = null) }
        }
    }

    fun addQuick(amountMl: Int) {
        if (amountMl !in 1..MAX_ENTRY_ML) return
        write { repository.addWater(userId, amountMl, clock.millis()) }
    }

    fun saveEditor() {
        val state = _uiState.value
        val amount = state.amountInput.trim().toIntOrNull()
        if (amount == null || amount !in 1..MAX_ENTRY_ML) {
            _uiState.update { it.copy(inputErrorResId = R.string.water_invalid_amount) }
            return
        }
        val entry = state.editingEntry
        write {
            if (entry == null) {
                repository.addWater(userId, amount, clock.millis())
            } else {
                // Beholder ID og tidspunkt, og endrer bare mengden.
                repository.update(entry.copy(amountMl = amount))
            }
            _uiState.update { it.copy(isEditorOpen = false, editingEntry = null) }
        }
    }

    fun askToDelete(entry: WaterEntryEntity) {
        if (!_uiState.value.isSaving) _uiState.update {
            it.copy(pendingDeletion = entry, isEditorOpen = false, editingEntry = null, errorMessageResId = null)
        }
    }

    fun cancelDelete() {
        if (!_uiState.value.isSaving) _uiState.update { it.copy(pendingDeletion = null, errorMessageResId = null) }
    }

    fun confirmDelete() {
        val entry = _uiState.value.pendingDeletion ?: return
        write {
            repository.delete(entry)
            _uiState.update { it.copy(pendingDeletion = null) }
        }
    }

    private fun write(action: suspend () -> Unit) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessageResId = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Beholder dialogen og innholdet, så brukeren kan prøve igjen.
                _uiState.update { it.copy(errorMessageResId = R.string.water_save_error) }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    companion object {
        // Lar Android opprette ViewModel med repository fra AppContainer.
        fun factory(repository: WaterRepository, goalRepository: GoalRepository) = viewModelFactory {
            initializer { WaterViewModel(repository, goalRepository) }
        }
    }
}
