package no.gruppe7.amate.feature.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.water.data.WaterRepository
import no.gruppe7.amate.feature.steps.data.StepsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val waterRepository: WaterRepository,
    private val goalRepository: GoalRepository,
    private val stepsRepository: StepsRepository? = null,
    // Samme lokale bruker som vannskjermen, skrittelleren og målene.
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
    // Telleren økes ved «Prøv igjen», slik at datakildene kobles til på nytt.
    private val reload = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(ProgressUiState(
        date = day.value.date, zoneId = day.value.zone,
        steps = stepsRepository?.let { StepsProgressUiState(date = day.value.date) },
    ))
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Ny dag eller nytt forsøk bytter spørring. flatMapLatest stopper den gamle.
            combine(day, reload) { current, _ -> current }.flatMapLatest { current ->
                val firstDate = current.date.minusDays(6)
                // Vi bruker lokale midnatter, ikke 7 x 24 timer. Da blir også sommertid riktig.
                val start = firstDate.atStartOfDay(current.zone).toInstant().toEpochMilli()
                val end = current.date.plusDays(1).atStartOfDay(current.zone).toInstant().toEpochMilli()
                // Den eksisterende datospørringen kan hente et helt tidsrom, ikke bare én dag.
                waterRepository.observeEntriesForDay(userId, start, end)
                    .onStart {
                        _uiState.update { it.copy(date = current.date, zoneId = current.zone,
                            days = emptyList(), isLoading = true, waterLoadFailed = false) }
                    }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        _uiState.update { it.copy(days = emptyList(), isLoading = false, waterLoadFailed = true) }
                    }
                    .map { current to it }
            }.collect { (current, entries) ->
                // En utsending fra før et dagskifte skal ikke dukke opp på den nye datoen.
                if (current != day.value) return@collect
                val totals = entries.groupBy {
                    java.time.Instant.ofEpochMilli(it.loggedAtMillis).atZone(current.zone).toLocalDate()
                }.mapValues { (_, registrations) -> registrations.sumOf { it.amountMl.toLong() } }
                // Alle sju dager får en søyle. Manglende registreringer gir 0, ikke en manglende dato.
                val days = (6L downTo 0L).map { offset ->
                    val date = current.date.minusDays(offset)
                    WaterProgressDay(date, totals[date] ?: 0L)
                }
                _uiState.update { it.copy(days = days, isLoading = false, waterLoadFailed = false) }
            }
        }

        viewModelScope.launch {
            // Vi leser Najmas eksisterende vannmål. Endringer og sletting kommer automatisk hit.
            reload.flatMapLatest {
                goalRepository.observeGoal(userId, GoalType.WATER)
                    .onStart { _uiState.update { it.copy(isGoalLoading = true, goalLoadFailed = false) } }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        _uiState.update { it.copy(goalMl = null, isGoalLoading = false, goalLoadFailed = true) }
                    }
            }.collect { goal ->
                _uiState.update { it.copy(
                    goalMl = goal?.takeIf { it.isActive }?.targetValue?.takeIf { it > 0 },
                    isGoalLoading = false, goalLoadFailed = false,
                ) }
            }
        }

        stepsRepository?.let { steps ->
            viewModelScope.launch {
                combine(day, reload) { current, _ -> current }.flatMapLatest { current ->
                    // Historikken inneholder også i dag. Da følger sum og søyler samme utsending.
                    // Vi leser bare lagrede skritt her; det er fortsatt skrittelleren som styrer sensoren.
                    steps.getStepsHistory(userId)
                        .onStart { updateSteps { it.copy(date = current.date, days = emptyList(), isLoading = true, loadFailed = false) } }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            updateSteps { it.copy(days = emptyList(), isLoading = false, loadFailed = true) }
                        }
                        .map { current to it }
                }.collect { (current, history) ->
                    if (current != day.value) return@collect
                    val counts = history.filter { it.userId == userId }.associateBy { it.epochDay }
                    // Skritt er allerede lagret per dato. Vi flytter ikke gamle skritt mellom datoer.
                    val days = (6L downTo 0L).map { offset ->
                        val date = current.date.minusDays(offset)
                        StepsProgressDay(date, counts[date.toEpochDay()]?.stepCount?.toLong() ?: 0L)
                    }
                    updateSteps { it.copy(date = current.date, days = days, isLoading = false, loadFailed = false) }
                }
            }
            viewModelScope.launch {
                reload.flatMapLatest {
                    // Samme målkilde som for vann, men bare målet med typen STEPS.
                    goalRepository.observeGoal(userId, GoalType.STEPS)
                        .onStart { updateSteps { it.copy(isGoalLoading = true, goalLoadFailed = false) } }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            updateSteps { it.copy(goal = null, isGoalLoading = false, goalLoadFailed = true) }
                        }
                }.collect { goal ->
                    updateSteps { it.copy(goal = goal?.takeIf { it.isActive }?.targetValue?.takeIf { it > 0 },
                        isGoalLoading = false, goalLoadFailed = false) }
                }
            }
        }

        viewModelScope.launch {
            // Oppdaterer dag og tidssone også hvis fremgangssiden står åpen over midnatt.
            while (true) {
                delay(30.seconds)
                refreshDay()
            }
        }
    }

    private fun updateSteps(change: (StepsProgressUiState) -> StepsProgressUiState) {
        _uiState.update { state -> state.copy(steps = state.steps?.let(change)) }
    }

    fun refreshDay() { day.value = currentDay() }
    fun retry() {
        refreshDay()
        reload.value += 1
    }

    companion object {
        fun factory(waterRepository: WaterRepository, goalRepository: GoalRepository, stepsRepository: StepsRepository? = null) = viewModelFactory {
            initializer { ProgressViewModel(waterRepository, goalRepository, stepsRepository) }
        }
    }
}
