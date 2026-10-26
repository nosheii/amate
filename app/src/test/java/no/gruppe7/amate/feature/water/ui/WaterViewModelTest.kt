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
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType
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
    private val goals = FakeGoalRepository()
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
        repository = repository, goalRepository = goals, clock = clock, zoneProvider = { zone },
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
        goals.goal.value = GoalEntity(userId = 1, targetValue = 2500)
        clock.now = Instant.parse("2026-10-15T21:59:50Z") // 23:59 i Oslo.
        val model = model()
        runCurrent()
        model.addQuick(250)
        runCurrent()
        assertEquals(250L, model.uiState.value.totalTodayMl)
        model.openEditor(model.uiState.value.entries.single())
        clock.now = clock.now.plusSeconds(30)
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals("2026-10-16", model.uiState.value.date.toString())
        assertEquals(0L, model.uiState.value.totalTodayMl)
        assertEquals(1, repository.entries.value.size)
        assertEquals(2500, model.uiState.value.goalMl)
        assertFalse(model.uiState.value.isEditorOpen)
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

    @Test
    fun missingChangedAndDeletedGoal_updatesMeterWithoutChangingWater() = waterTest {
        val model = model()
        runCurrent()
        assertNull(model.uiState.value.goalMl)
        assertNull(model.uiState.value.progress)
        model.addQuick(500)
        runCurrent()
        assertEquals(500L, model.uiState.value.totalTodayMl)

        goals.goal.value = GoalEntity(id = 1, userId = 1, targetValue = 2000)
        runCurrent()
        assertEquals(2000, model.uiState.value.goalMl)
        assertEquals(0.25f, model.uiState.value.progress!!, 0.001f)
        goals.goal.value = goals.goal.value!!.copy(targetValue = 1000)
        runCurrent()
        assertEquals(0.5f, model.uiState.value.progress!!, 0.001f)
        goals.goal.value = null
        runCurrent()
        assertNull(model.uiState.value.goalMl)
        assertNull(model.uiState.value.progress)
        assertEquals(500L, model.uiState.value.totalTodayMl)
    }

    @Test
    fun goalFailure_doesNotBlockWaterAndCanBeRetried() = waterTest {
        goals.failRead = true
        val model = model()
        runCurrent()
        assertTrue(model.uiState.value.goalLoadFailed)
        assertFalse(model.uiState.value.loadFailed)
        model.addQuick(375)
        runCurrent()
        assertEquals(375L, model.uiState.value.totalTodayMl)
        assertNull(model.uiState.value.progress)
        goals.failRead = false
        goals.goal.value = GoalEntity(userId = 1, targetValue = 1500)
        model.retry()
        runCurrent()
        assertFalse(model.uiState.value.goalLoadFailed)
        assertEquals(1500, model.uiState.value.goalMl)
        assertEquals(375L, model.uiState.value.totalTodayMl)
    }

    @Test
    fun stepsGoalsOtherUsersAndInactiveGoalsAreNotWaterTargets() = waterTest {
        val model = model()
        runCurrent()
        for (goal in listOf(
            GoalEntity(userId = 2, targetValue = 3000),
            GoalEntity(userId = 1, type = GoalType.STEPS, targetValue = 10000),
            GoalEntity(userId = 1, targetValue = 2000, isActive = false),
        )) {
            goals.goal.value = goal
            runCurrent()
            assertNull(model.uiState.value.goalMl)
            assertNull(model.uiState.value.progress)
        }
    }

    @Test
    fun meterNeverDividesByZeroOrGoesPastFull() = waterTest {
        val model = model()
        runCurrent()
        goals.goal.value = GoalEntity(userId = 1, targetValue = 0)
        model.addQuick(3000)
        runCurrent()
        assertNull(model.uiState.value.progress)
        goals.goal.value = GoalEntity(userId = 1, targetValue = 1000)
        runCurrent()
        assertEquals(1f, model.uiState.value.progress!!, 0.001f)
    }

    @Test
    fun listOnlyShowsWriteErrorWhenNoEditorOrDeleteDialogOwnsIt() {
        val state = WaterUiState(errorMessageResId = R.string.water_save_error)
        assertEquals(R.string.water_save_error, state.listErrorMessageResId)
        assertNull(state.copy(isEditorOpen = true).listErrorMessageResId)
        assertNull(state.copy(pendingDeletion = WaterEntryEntity(id = 1, userId = 1, amountMl = 250,
            loggedAtMillis = 0)).listErrorMessageResId)
        // En annen feiltype: selv med editoren åpen skal det gå an å prøve innlasting på nytt.
        assertEquals(R.string.water_load_error,
            state.copy(isEditorOpen = true, loadFailed = true).listErrorMessageResId)
    }
}

// Samme kontrakt som Najmas repository. Bare lagringsstedet er byttet ut for denne testen.
private class FakeGoalRepository : GoalRepository {
    val goal = MutableStateFlow<GoalEntity?>(null)
    var failRead = false
    override fun observeGoal(userId: Long, type: GoalType): Flow<GoalEntity?> {
        if (failRead) return flow { error("Test: kunne ikke hente mål") }
        return goal.map { it?.takeIf { row -> row.userId == userId && row.type == type && row.isActive } }
    }
    override fun observeGoals(userId: Long) = goal.map { listOfNotNull(it?.takeIf { row -> row.userId == userId }) }
    override suspend fun saveGoal(userId: Long, type: GoalType, targetValue: Int): Long {
        goal.value = GoalEntity(id = 1, userId = userId, type = type, targetValue = targetValue)
        return 1
    }
    override suspend fun deleteGoal(goal: GoalEntity) { this.goal.value = null }
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
