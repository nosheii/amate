package no.gruppe7.amate.feature.reminders.ui

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.reminders.data.ReminderRepository
import no.gruppe7.amate.feature.reminders.data.local.ReminderEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// Her tester vi lagringsreglene uten Android-skjerm og uten ekte Room.
// FakeReminderRepository lar oss styre data og late som en lagring feiler.
@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {
    private val store = ViewModelStore()
    private val repository = FakeReminderRepository()
    // Bytter Androids hovedtråd med en styrbar testversjon. resetMain rydder opp etterpå.
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }
    // Lagrer ViewModel i en test-store slik at vi også kan rydde bort coroutine-jobbene dens.
    private fun model() = RemindersViewModel(repository).also { store.put("reminders", it) }
    // runTest gir oss styring over coroutines. runCurrent kjører det som står klart nå.
    private fun reminderTest(block: suspend TestScope.() -> Unit) = runTest {
        try { block() } finally { store.clear(); runCurrent() }
    }

    @Test fun loadsDefaultsWithoutSaving() = reminderTest {
        val model = model()
        runCurrent()
        val state = model.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.enabled)
        assertEquals(3, state.remindersPerDay)
        assertEquals("08:00", state.startTime)
        assertEquals("21:00", state.endTime)
        assertEquals(0, repository.saveCount)
    }

    @Test fun draftIsSavedOnlyWhenSaveIsPressed() = reminderTest {
        // Først endrer vi flere felt og sjekker at de ikke er lagret. Så trykker vi «Lagre» i koden.
        val model = model()
        runCurrent()
        model.changeCount(5)
        model.changeStart("09:30")
        model.changeEnd("20:15")
        model.changeEnabled(true)
        assertEquals(0, repository.saveCount)
        model.save()
        runCurrent()
        assertEquals(ReminderEntity(1, true, 5, 570, 1215), repository.settings.value)
        assertFalse(model.uiState.value.hasChanges)
        assertTrue(model.uiState.value.saved)
    }

    @Test fun cancelRestoresLastSavedSettings() = reminderTest {
        val model = model()
        runCurrent()
        model.changeCount(1)
        model.changeStart("11:00")
        model.discardChanges()
        assertEquals(3, model.uiState.value.remindersPerDay)
        assertEquals("08:00", model.uiState.value.startTime)
        assertFalse(model.uiState.value.hasChanges)
        assertEquals(0, repository.saveCount)
    }

    @Test fun invalidTimesCannotBeSaved() = reminderTest {
        val model = model()
        runCurrent()
        // Feil format, like tider og tidsrom over midnatt avvises i denne versjonen.
        for ((start, end) in listOf("8:00" to "21:00", "24:00" to "21:00",
            "08:60" to "21:00", "" to "21:00", "12:00" to "12:00", "22:00" to "07:00")) {
            model.changeStart(start)
            model.changeEnd(end)
            model.save()
            runCurrent()
            assertEquals(R.string.reminders_invalid_time, model.uiState.value.errorMessageResId)
        }
        assertEquals(0, repository.saveCount)
    }

    @Test fun saveFailureKeepsDraftAndRetryWorks() = reminderTest {
        val model = model()
        runCurrent()
        model.changeCount(4)
        repository.failWrite = true
        model.save()
        runCurrent()
        assertEquals(4, model.uiState.value.remindersPerDay)
        assertTrue(model.uiState.value.hasChanges)
        assertFalse(model.uiState.value.isSaving)
        assertEquals(R.string.reminders_save_error, model.uiState.value.errorMessageResId)
        repository.failWrite = false
        model.save()
        runCurrent()
        assertEquals(4, repository.settings.value.remindersPerDay)
        assertTrue(model.uiState.value.saved)
    }

    @Test fun loadFailureCanBeRetried() = reminderTest {
        repository.failRead = true
        val model = model()
        runCurrent()
        assertTrue(model.uiState.value.loadFailed)
        repository.failRead = false
        model.retry()
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertFalse(model.uiState.value.loadFailed)
    }

    @Test fun permissionIsSeparateFromSavedPreferences() = reminderTest {
        val model = model()
        runCurrent()
        model.permissionResult(false)
        assertFalse(model.uiState.value.enabled)
        assertFalse(model.uiState.value.notificationsAllowed)
        model.permissionResult(true)
        assertTrue(model.uiState.value.enabled)
        assertTrue(model.uiState.value.notificationsAllowed)
        assertEquals(0, repository.saveCount)
    }

    @Test fun externalUpdateDoesNotReplaceUnsavedInput() = reminderTest {
        val model = model()
        runCurrent()
        model.changeStart("10:30")
        repository.settings.value = ReminderEntity(1, remindersPerDay = 5)
        runCurrent()
        assertEquals("10:30", model.uiState.value.startTime)
        model.discardChanges()
        assertEquals(5, model.uiState.value.remindersPerDay)
    }

    @Test fun minuteConversionCoversWholeDay() {
        assertEquals(0, parseMinutes("00:00"))
        assertEquals(1439, parseMinutes("23:59"))
        assertEquals("08:05", formatMinutes(485))
        assertNull(parseMinutes("09:99"))
    }
}

// Room erstattes bare i disse testene, så logikken kan testes uten emulator.
private class FakeReminderRepository : ReminderRepository {
    val settings = MutableStateFlow(ReminderEntity(1))
    var saveCount = 0
    var failRead = false
    var failWrite = false
    override fun observeSettings(userId: Long): Flow<ReminderEntity> =
        if (failRead) flow { error("Test: lesefeil") } else settings
    override suspend fun save(settings: ReminderEntity) {
        check(!failWrite)
        saveCount++
        this.settings.value = settings
    }
}
