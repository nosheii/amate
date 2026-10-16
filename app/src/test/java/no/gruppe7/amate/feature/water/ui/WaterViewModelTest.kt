package no.gruppe7.amate.feature.water.ui

import androidx.lifecycle.ViewModelStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.water.data.WaterRepository
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WaterViewModelTest {
    private val store = ViewModelStore()
    private val repository = FakeWaterRepository()
    private val clock = MutableClock(Instant.parse("2026-10-15T12:00:00Z"))
    private var zone = ZoneId.of("Europe/Oslo")

    @Before
    fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

    @After
    fun tearDown() {
        // Stopper også løkken som følger med på dagskifte.
        store.clear()
        Dispatchers.resetMain()
    }

    private fun model(): WaterViewModel = WaterViewModel(
        repository = repository, clock = clock, zoneProvider = { zone },
    ).also { store.put("water", it) }

    private fun waterTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            // Stopper midnattsløkken før testklokken prøver å kjøre videre for alltid.
            store.clear()
            runCurrent()
        }
    }

    @Test
    fun quickAdd_updatesListAndTotalFromRepository() = waterTest {
        val model = model()
        runCurrent()
        assertFalse(model.uiState.value.isLoading)
        assertEquals(0L, model.uiState.value.totalTodayMl)

        model.addQuick(250)
        runCurrent()
        model.addQuick(500)
        runCurrent()
        assertEquals(750L, model.uiState.value.totalTodayMl)
        assertEquals(2, model.uiState.value.entries.size)
        assertEquals(clock.millis(), model.uiState.value.entries.first().loggedAtMillis)
    }

    @Test
    fun editor_rejectsEmptyNegativeDecimalZeroAndOverflow() = waterTest {
        val model = model()
        runCurrent()
        model.openEditor()
        for (input in listOf("", " ", "-1", "0", "2.5", "abc", "10001", "9999999999999")) {
            model.changeAmount(input)
            model.saveEditor()
            runCurrent()
            assertEquals(R.string.water_invalid_amount, model.uiState.value.inputErrorResId)
            assertTrue(repository.entries.value.isEmpty())
        }
        model.changeAmount(" 375 ")
        model.saveEditor()
        runCurrent()
        assertEquals(375L, model.uiState.value.totalTodayMl)
        assertFalse(model.uiState.value.isEditorOpen)
    }

    @Test
    fun edit_keepsIdAndOriginalTime() = waterTest {
        val model = model()
        runCurrent()
        model.addQuick(250)
        runCurrent()
        val original = model.uiState.value.entries.single()
        clock.now = clock.now.plusSeconds(3600)
        model.openEditor(original)
        model.changeAmount("500")
        model.saveEditor()
        runCurrent()
        assertEquals(original.copy(amountMl = 500), model.uiState.value.entries.single())
        assertEquals(500L, model.uiState.value.totalTodayMl)
    }

    @Test
    fun delete_requiresConfirmationAndUpdatesTotal() = waterTest {
        val model = model()
        runCurrent()
        model.addQuick(250)
        runCurrent()
        val entry = model.uiState.value.entries.single()
        model.askToDelete(entry)
        model.cancelDelete()
        assertEquals(250L, model.uiState.value.totalTodayMl)
        model.askToDelete(entry)
        model.confirmDelete()
        runCurrent()
        assertTrue(model.uiState.value.entries.isEmpty())
        assertEquals(0L, model.uiState.value.totalTodayMl)
    }

    @Test
    fun writeFailure_keepsInputAndAllowsRetry() = waterTest {
        val model = model()
        runCurrent()
        model.openEditor()
        model.changeAmount("350")
        repository.failWrite = true
        model.saveEditor()
        runCurrent()
        assertTrue(model.uiState.value.isEditorOpen)
        assertEquals("350", model.uiState.value.amountInput)
        assertFalse(model.uiState.value.isSaving)
        assertEquals(R.string.water_save_error, model.uiState.value.errorMessageResId)

        repository.failWrite = false
        model.saveEditor()
        runCurrent()
        assertEquals(350L, model.uiState.value.totalTodayMl)
        assertFalse(model.uiState.value.isEditorOpen)
    }

    @Test
    fun loadFailure_canBeRetried() = waterTest {
        repository.failRead = true
        val model = model()
        runCurrent()
        assertTrue(model.uiState.value.loadFailed)
        repository.failRead = false
        model.retry()
        runCurrent()
        assertFalse(model.uiState.value.loadFailed)
        assertFalse(model.uiState.value.isLoading)
        assertNull(model.uiState.value.errorMessageResId)
    }

    @Test
    fun midnight_refreshesTheDayWithoutDeletingHistory() = waterTest {
        clock.now = Instant.parse("2026-10-15T21:59:50Z") // 23:59 i Oslo.
        val model = model()
        runCurrent()
        model.addQuick(250)
        runCurrent()
        assertEquals(250L, model.uiState.value.totalTodayMl)
        clock.now = clock.now.plusSeconds(30)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals("2026-10-16", model.uiState.value.date.toString())
        assertEquals(0L, model.uiState.value.totalTodayMl)
        assertEquals(1, repository.entries.value.size)
    }

    @Test
    fun daylightSaving_usesActualLocalDayInsteadOf24Hours() = waterTest {
        clock.now = Instant.parse("2026-03-29T12:00:00Z")
        model()
        runCurrent()
        assertEquals(23 * 60 * 60 * 1000L, repository.lastEnd - repository.lastStart)
    }

    @Test
    fun timezoneChange_refreshesLocalDayAndRange() = waterTest {
        clock.now = Instant.parse("2026-10-15T22:30:00Z")
        val model = model()
        runCurrent()
        assertEquals("2026-10-16", model.uiState.value.date.toString())
        zone = ZoneOffset.UTC
        model.refreshDay()
        runCurrent()
        assertEquals("2026-10-15", model.uiState.value.date.toString())
        assertEquals(ZoneOffset.UTC, model.uiState.value.zoneId)
    }

    @Test
    fun listAndTotal_excludeOtherUsersAndOtherDays() = waterTest {
        val now = clock.millis()
        repository.entries.value = listOf(
            WaterEntryEntity(1, 1, 250, now),
            WaterEntryEntity(2, 2, 500, now),
            WaterEntryEntity(3, 1, 750, now - 86_400_000),
        )
        val model = model()
        runCurrent()
        assertEquals(listOf(1L), model.uiState.value.entries.map { it.id })
        assertEquals(250L, model.uiState.value.totalTodayMl)
    }
}

private class MutableClock(var now: Instant) : Clock() {
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
}

// En liten testutgave, så ViewModel-testene kan kjøres uten emulator.
private class FakeWaterRepository : WaterRepository {
    val entries = MutableStateFlow<List<WaterEntryEntity>>(emptyList())
    var failRead = false
    var failWrite = false
    var lastStart = 0L
    var lastEnd = 0L
    private var nextId = 1L

    override fun observeEntries(userId: Long) = entries.map { all -> all.filter { it.userId == userId } }

    override fun observeEntriesForDay(userId: Long, startMillis: Long, endMillis: Long): Flow<List<WaterEntryEntity>> {
        lastStart = startMillis
        lastEnd = endMillis
        if (failRead) return flow { error("Test: kunne ikke lese") }
        return entries.map { all ->
            all.filter { it.userId == userId && it.loggedAtMillis >= startMillis && it.loggedAtMillis < endMillis }
                .sortedWith(compareByDescending<WaterEntryEntity> { it.loggedAtMillis }.thenByDescending { it.id })
        }
    }

    override fun observeTotalForDay(userId: Long, startMillis: Long, endMillis: Long) =
        observeEntriesForDay(userId, startMillis, endMillis).map { rows -> rows.sumOf { it.amountMl } }

    override suspend fun addWater(userId: Long, amountMl: Int, loggedAtMillis: Long): Long {
        check(!failWrite)
        val id = nextId++
        entries.value += WaterEntryEntity(id, userId, amountMl, loggedAtMillis)
        return id
    }

    override suspend fun update(entry: WaterEntryEntity) {
        check(!failWrite)
        entries.value = entries.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun delete(entry: WaterEntryEntity) {
        check(!failWrite)
        entries.value = entries.value.filterNot { it.id == entry.id }
    }
}
