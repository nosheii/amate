package no.gruppe7.amate.feature.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import no.gruppe7.amate.feature.tasks.data.MAX_TITLE_LENGTH
import no.gruppe7.amate.feature.tasks.data.TaskRepository
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity

// Samme oppsett som WaterViewModel: gjøremål hører til en dag, så vi må vite hvilken dag det er.
@OptIn(ExperimentalCoroutinesApi:: class)
class TasksViewModel(
    private val repository: TaskRepository,
    // Fast lokal bruker frem til profilfunksjonen er koblet på.
    private val userId: Long = 1,
    // Klokke og tidssone sendes inn, så testene kan late som det er midnatt.
    private val clock: Clock = Clock.systemUTC(),
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault()},
) : ViewModel() {

    // Dagen vi viser, med start og slutt i millisekunder.
    // Neste datos start takler også døgn med overgang til sommertid.
    private data class Day(val date: LocalDate, val zone: ZoneId) {
        val startMillis: Long get() = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis: Long get() = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun currentDay(): Day {
        val zone = zoneProvider()
        return Day(clock.instant().atZone(zone).toLocalDate(), zone)
    }

    private val day = MutableStateFlow(currentDay())
    // Økes ved "Prøv igjen", så Flow-en fra Room startes på nytt.
    private val reload = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(TasksUiState(date = day.value.date, zoneId = day.value.zone))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Ny dag eller "Prøv igjen" gir en ny spørring. flatMapLatest avbryter den gamle.
            combine(day, reload) { current, _ -> current }
                .flatMapLatest { current ->
                    repository.observeTasksForDay(userId, current.startMillis, current.endMillis)
                        .onStart{
                            _uiState.update {
                                val sameDay = it.date == current.date
                                it.copy(
                                    date = current.date, zoneId = current.zone,
                                    tasks = emptyList(), isLoading = true, loadFailed = false,
                                    // Et felt for gårsdagens gjøremål skal ikke bli hengende på en ny dag.
                                    isEditorOpen = it.isEditorOpen && sameDay,
                                    editingTask = if (sameDay) it.editingTask else null,
                                )
                            }
                        }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            _uiState.update {
                                it.copy(isLoading = false, loadFailed = true,
                                    errorMessageResId = R.string.tasks_load_error)
                            }
                        }
                }
                .collectLatest { tasks ->
                    _uiState.update { it.copy (tasks = tasks, isLoading = false, loadFailed = false) }
                }
        }
    }
    fun refreshDay() { day.value = currentDay() }

    fun retry() {
        dismissError()
        refreshDay()
        reload.value +=1
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessageResId = null) }
    }

    fun openEditor(task: SelfCareTaskEntity? = null ) {
        if (_uiState.value.isSaving) return
        _uiState.update {
            it.copy(isEditorOpen = true, editingTask = task,
                titleInput = task?.title.orEmpty(), inputErrorResId = null)
        }
    }

    fun changeTitle(input: String) {
        _uiState.update { it.copy(titleInput = input, inputErrorResId = null) }
    }

    fun closeEditor() {
        if (!_uiState.value.isSaving) {
            _uiState.update {it.copy(isEditorOpen = false, editingTask = null, inputErrorResId = null, ) }
        }
    }

    fun saveEditor() {
        val state = _uiState.value
        val title = state.titleInput.trim()
        if (title.isEmpty() || title.length > MAX_TITLE_LENGTH) {
            _uiState.update { it.copy(inputErrorResId = R.string.tasks_invalid_title) }
            return
        }
        val task = state.editingTask
        write {
            if (task == null) {
                repository.addTask(userId, title, day.value.startMillis)
            } else {
                repository.rename(task, title)
            }
            _uiState.update {it.copy(isEditorOpen = false, editingTask = null) }
        }
    }

    fun toggleCompleted(task: SelfCareTaskEntity) {
        write { repository.setCompleted(task, !task.isCompleted, clock.millis())}
    }

    fun deleteTask(task: SelfCareTaskEntity) {
        write { repository.delete(task) }
    }

    private fun write(action: suspend () -> Unit) {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, errorMessageResId = null)}
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update {it.copy(errorMessageResId = R.string.tasks_save_error) }
            } finally {
                _uiState.update {it.copy(isSaving = false)}
            }
        }
    }

    companion object {
        fun factory(repository: TaskRepository) = viewModelFactory {
            initializer { TasksViewModel(repository) }
        }
    }
}
