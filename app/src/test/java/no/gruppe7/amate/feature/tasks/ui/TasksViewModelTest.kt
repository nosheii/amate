package no.gruppe7.amate.feature.tasks.ui

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.tasks.data.TaskRepository
import no.gruppe7.amate.feature.tasks.data.local.SelfCareTaskEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Vanlig JVM-test: kjører på PC-en uten telefon, fordi Room er byttet ut med et falskt repository.
@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    private val repository = FakeTaskRepository()
    // Fast klokke, så testene vet nøyaktig hvilken dag det er.
    private val clock = MutableClock(Instant.parse("2026-10-30T12:00:00Z"))
    private val zone = ZoneId.of("Europe/Oslo")

    @Before
    fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun model() = TasksViewModel(repository, clock = clock, zoneProvider = { zone })

    @Test
    fun add_createsTaskForTodayAndClosesEditor() = runTest {
        val model = model()
        runCurrent()
        assertFalse(model.uiState.value.isLoading)

        model.openEditor()
        model.changeTitle("  Gå en tur  ")
        model.saveEditor()
        runCurrent()

        assertFalse(model.uiState.value.isEditorOpen)
        val task = model.uiState.value.tasks.single()
        assertEquals("Gå en tur", task.title)
        // Lagt på starten av dagen i Oslo-tid (30. okt 00:00 = 29. okt 23:00 UTC), ikke på "nå".
        assertEquals(Instant.parse("2026-10-29T23:00:00Z").toEpochMilli(), task.scheduledForMillis)
    }

    @Test
    fun save_rejectsBlankAndTooLongTitle() = runTest {
        val model = model()
        runCurrent()
        model.openEditor()
        for (input in listOf("", "   ", "a".repeat(61))) {
            model.changeTitle(input)
            model.saveEditor()
            runCurrent()
            assertEquals(R.string.tasks_invalid_title, model.uiState.value.inputErrorResId)
            assertTrue(repository.tasks.value.isEmpty())
        }
    }

    @Test
    fun edit_keepsIdAndChangesTitle() = runTest {
        val model = model()
        runCurrent()
        model.openEditor(); model.changeTitle("Lese"); model.saveEditor(); runCurrent()
        val original = model.uiState.value.tasks.single()

        // Åpner på nytt: feltet skal være forhåndsutfylt.
        model.openEditor(original)
        assertEquals("Lese", model.uiState.value.titleInput)
        model.changeTitle("Lese 10 sider"); model.saveEditor(); runCurrent()

        val edited = model.uiState.value.tasks.single()
        assertEquals(original.id, edited.id)
        assertEquals("Lese 10 sider", edited.title)
    }

    @Test
    fun toggle_marksDoneWithTimeAndCanUndo() = runTest {
        val model = model()
        runCurrent()
        model.openEditor(); model.changeTitle("Strekke ut"); model.saveEditor(); runCurrent()

        model.toggleCompleted(model.uiState.value.tasks.single())
        runCurrent()
        val done = model.uiState.value.tasks.single()
        assertTrue(done.isCompleted)
        assertEquals(clock.millis(), done.completedAtMillis)
        assertEquals(1, model.uiState.value.completedCount)

        // Angre: tilbake til ikke gjort, og tidspunktet nullstilles.
        model.toggleCompleted(done)
        runCurrent()
        assertFalse(model.uiState.value.tasks.single().isCompleted)
        assertNull(model.uiState.value.tasks.single().completedAtMillis)
    }

    @Test
    fun delete_removesTask() = runTest {
        val model = model()
        runCurrent()
        model.openEditor(); model.changeTitle("Slettes"); model.saveEditor(); runCurrent()

        model.deleteTask(model.uiState.value.tasks.single())
        runCurrent()

        assertTrue(model.uiState.value.tasks.isEmpty())
    }

    @Test
    fun writeFailure_keepsEditorOpenAndShowsError() = runTest {
        val model = model()
        runCurrent()
        repository.failWrite = true
        model.openEditor(); model.changeTitle("Gå en tur"); model.saveEditor(); runCurrent()

        assertTrue(model.uiState.value.isEditorOpen)
        assertEquals("Gå en tur", model.uiState.value.titleInput)
        assertEquals(R.string.tasks_save_error, model.uiState.value.errorMessageResId)
        assertFalse(model.uiState.value.isSaving)
    }

    @Test
    fun loadFailure_canBeRetried() = runTest {
        repository.failRead = true
        val model = model()
        runCurrent()
        assertTrue(model.uiState.value.loadFailed)

        repository.failRead = false
        model.retry()
        runCurrent()
        assertFalse(model.uiState.value.loadFailed)
        assertNull(model.uiState.value.errorMessageResId)
    }

    @Test
    fun newDay_showsEmptyListWithoutDeletingYesterday() = runTest {
        val model = model()
        runCurrent()
        model.openEditor(); model.changeTitle("I går"); model.saveEditor(); runCurrent()
        assertEquals(1, model.uiState.value.tasks.size)

        // Klokken går over midnatt, og skjermen vises igjen.
        clock.now = Instant.parse("2026-10-31T06:00:00Z")
        model.refreshDay()
        runCurrent()

        assertEquals(LocalDate.of(2026, 10, 31), model.uiState.value.date)
        assertTrue(model.uiState.value.tasks.isEmpty())
        // Gårsdagens gjøremål ligger fortsatt i "databasen", bare ikke i dagens liste.
        assertEquals(1, repository.tasks.value.size)
    }
}

// Klokke vi kan stille selv, så testene kan late som det er midnatt.
private class MutableClock(var now: Instant) : Clock() {
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
}

// Falskt repository som holder gjøremålene i en liste i minnet.
private class FakeTaskRepository : TaskRepository {
    val tasks = MutableStateFlow<List<SelfCareTaskEntity>>(emptyList())
    var failRead = false
    var failWrite = false
    private var nextId = 1L

    override fun observeTasksForDay(userId: Long, startMillis: Long, endMillis: Long): Flow<List<SelfCareTaskEntity>> {
        if (failRead) return flow { error("Test: kunne ikke lese") }
        return tasks.map { all ->
            all.filter { it.userId == userId && it.scheduledForMillis >= startMillis && it.scheduledForMillis < endMillis }
                .sortedWith(compareBy<SelfCareTaskEntity> { it.isCompleted }.thenBy { it.id })
        }
    }

    override suspend fun addTask(userId: Long, title: String, scheduledForMillis: Long): Long {
        check(!failWrite)
        val id = nextId++
        tasks.value += SelfCareTaskEntity(id = id, userId = userId, title = title.trim(), scheduledForMillis = scheduledForMillis)
        return id
    }

    override suspend fun rename(task: SelfCareTaskEntity, title: String) {
        check(!failWrite)
        tasks.value = tasks.value.map { if (it.id == task.id) it.copy(title = title.trim()) else it }
    }

    override suspend fun setCompleted(task: SelfCareTaskEntity, completed: Boolean, nowMillis: Long) {
        check(!failWrite)
        tasks.value = tasks.value.map {
            if (it.id == task.id) it.copy(isCompleted = completed, completedAtMillis = if (completed) nowMillis else null) else it
        }
    }

    override suspend fun delete(task: SelfCareTaskEntity) {
        check(!failWrite)
        tasks.value = tasks.value.filterNot { it.id == task.id }
    }
}