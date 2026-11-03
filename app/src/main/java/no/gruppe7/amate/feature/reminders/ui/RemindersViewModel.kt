package no.gruppe7.amate.feature.reminders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.reminders.data.ReminderRepository
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity

// Holder styr på valgene mens man bruker skjermen. UI-et viser bare det vi legger i uiState.
// Denne klassen lagrer via repository-et og trenger ikke å vite hvordan Room er satt opp.
class RemindersViewModel(
    private val repository: ReminderRepository,
    // Samme midlertidige bruker som vannfunksjonen. Profil kobles til senere.
    private val userId: Long = 1,
) : ViewModel() {
    // _uiState kan endres her inne. Skjermen får en lesbar versjon gjennom asStateFlow().
    // Når vi oppdaterer state, får Compose beskjed om å vise de nye verdiene.
    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState = _uiState.asStateFlow()
    // Holder en separat kopi av det som faktisk er lagret, slik at Avbryt kan hente det tilbake.
    // Standardverdiene byttes ut når vi får første svar fra Room.
    private var lastSaved = ReminderEntity(userId)
    // Referansen gjør at vi kan stoppe den gamle lytteren før vi lager en ny ved Prøv igjen.
    private var observeJob: Job? = null

    // Kjøres når ViewModel opprettes, ikke ved hver opptegning av skjermen.
    init { observeSettings() }

    private fun observeSettings() {
        // Uten dette kunne flere lyttere blitt stående og oppdatert samme skjerm samtidig.
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, loadFailed = false, errorMessageResId = null) }
        // launch starter en coroutine. viewModelScope rydder den bort når ViewModel fjernes.
        observeJob = viewModelScope.launch {
            repository.observeSettings(userId)
                .catch { error ->
                    // En coroutine som stoppes, skal få stoppe. Det er ikke en innlastingsfeil.
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(isLoading = false, loadFailed = true,
                            errorMessageResId = R.string.reminders_load_error)
                    }
                }
                // collect mottar første svar og deretter nye lagrede valg når de endres.
                .collect { settings ->
                    val changed = settings != lastSaved
                    lastSaved = settings
                    // En oppdatering fra Room skal ikke overskrive et utkast som redigeres.
                    // Vi oppdaterer feltene automatisk bare når brukeren ikke redigerer/lagrer.
                    // Slik slipper også vår egen lagring å nullstille bekreftelsen unødvendig.
                    val state = _uiState.value
                    if (!state.hasChanges && !state.isSaving && (state.isLoading || changed)) {
                        showSavedSettings()
                    } else {
                        // Et bevart utkast skal også kunne brukes etter en ny innlasting.
                        _uiState.update { it.copy(isLoading = false, loadFailed = false) }
                    }
                }
        }
    }

    // Kopierer siste lagrede valg til feltene og fjerner markeringen for ulagrede endringer.
    // copy() endrer bare feltene vi oppgir; f.eks. telefonens tillatelse beholdes.
    private fun showSavedSettings() {
        _uiState.update {
            it.copy(enabled = lastSaved.enabled, remindersPerDay = lastSaved.remindersPerDay,
                startTime = formatMinutes(lastSaved.startMinute), endTime = formatMinutes(lastSaved.endMinute),
                isLoading = false, loadFailed = false, hasChanges = false, saved = false,
                errorMessageResId = null)
        }
    }

    // Prøv igjen starter innlastingen på nytt. change...-funksjonene under redigerer bare utkastet.
    fun retry() = observeSettings()

    fun changeEnabled(enabled: Boolean) = edit { it.copy(enabled = enabled) }
    fun changeCount(count: Int) {
        if (count in 1..5) edit { it.copy(remindersPerDay = count) }
    }
    fun changeStart(value: String) = edit { it.copy(startTime = value) }
    fun changeEnd(value: String) = edit { it.copy(endTime = value) }

    // change er en liten funksjon fra f.eks. changeCount som sier hvilket felt vi skal bytte.
    // Samler felles regler her, så alle feltene håndteres likt.
    private fun edit(change: (RemindersUiState) -> RemindersUiState) {
        if (_uiState.value.isLoading || _uiState.value.isSaving || _uiState.value.loadFailed) return
        // it er gjeldende state. Vi markerer utkastet som endret og fjerner gamle meldinger.
        _uiState.update { change(it).copy(hasChanges = true, saved = false, errorMessageResId = null) }
    }

    fun discardChanges() {
        // Vi stopper ikke en lagring som allerede er i gang. Ellers hentes de lagrede verdiene tilbake.
        if (!_uiState.value.isSaving) showSavedSettings()
    }

    // Dette er telefonens tillatelse, og skal ikke lagres som en innstilling i Room.
    // Derfor endrer denne funksjonen heller ikke bryteren eller hasChanges.
    fun updatePermission(allowed: Boolean) {
        _uiState.update { it.copy(notificationsAllowed = allowed) }
    }

    // Brukes når tillatelsesdialogen kom fra av/på-bryteren.
    // Testknappen bruker bare updatePermission, så en varseltest ikke slår på bryteren.
    fun permissionResult(allowed: Boolean) {
        updatePermission(allowed)
        changeEnabled(allowed)
    }

    // Først her går vi fra et utkast på skjermen til lagrede innstillinger.
    fun save() {
        // Tar et bilde av valgene nå, slik at vi lagrer ett sammenhengende sett med verdier.
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.loadFailed) return
        val start = parseMinutes(state.startTime)
        val end = parseMinutes(state.endTime)
        // null betyr at teksten ikke var et gyldig klokkeslett. Vi avviser også over midnatt.
        if (start == null || end == null || start >= end) {
            _uiState.update { it.copy(errorMessageResId = R.string.reminders_invalid_time) }
            return
        }
        // Gjør UI-feltene om til typen repository-et/Room forventer.
        val settings = ReminderEntity(userId, state.enabled, state.remindersPerDay, start, end)
        // isSaving gjør at man ikke får startet flere lagringer med raske knappetrykk.
        _uiState.update { it.copy(isSaving = true, errorMessageResId = null, saved = false) }
        viewModelScope.launch {
            try {
                // Vi venter på at lagringen fullføres før vi viser at det gikk bra.
                // Appens oppstartslytter reagerer på lagrede valg og oppdaterer bakgrunnsjobben.
                repository.save(settings)
                lastSaved = settings
                showSavedSettings()
                _uiState.update { it.copy(saved = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Beholder alle feltene, så brukeren slipper å skrive dem på nytt.
                _uiState.update { it.copy(errorMessageResId = R.string.reminders_save_error) }
            } finally {
                // Kjøres både ved suksess og feil, så skjermen ikke blir stående i «Lagrer…».
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    companion object {
        // Android lager ViewModel-en gjennom denne oppskriften, med riktig repository.
        fun factory(repository: ReminderRepository) = viewModelFactory {
            initializer { RemindersViewModel(repository) }
        }
    }
}

// Tar bare imot et tydelig klokkeslett på formen TT:MM, for eksempel 08:30.
internal fun parseMinutes(value: String): Int? {
    // Regex sjekker to siffer, kolon og to siffer. trim fjerner mellomrom i endene.
    if (!Regex("[0-9]{2}:[0-9]{2}").matches(value.trim())) return null
    // Deler «08:30» i time=8 og minutt=30. Vi sjekker gyldig område før vi regner videre.
    val (hour, minute) = value.trim().split(":").map { it.toInt() }
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

// Motsatt vei: 510 minutter blir «08:30». / finner timer, % finner minuttene som er igjen.
// padStart fyller på en null foran enkle siffer, så det ikke blir «8:3».
internal fun formatMinutes(value: Int): String =
    "${(value / 60).toString().padStart(2, '0')}:${(value % 60).toString().padStart(2, '0')}"
