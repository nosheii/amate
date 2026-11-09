package no.gruppe7.amate.feature.goals.ui

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
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Vanlig JVM-test: kjører på PC-en uten telefon, fordi Room er byttet ut med et falskt repository.
@OptIn(ExperimentalCoroutinesApi::class)
class GoalsViewModelTest {
    private val repository = FakeGoalRepository()

    // ViewModel bruker Dispatchers.Main, som ikke finnes på PC-en.
    // Vi bytter den ut med en testversjon vi styrer selv med runCurrent().
    @Before
    fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun save_createsGoalAndClosesEditor() = runTest {
        val model = GoalsViewModel(repository)
        // runCurrent() lar alt som venter i bakgrunnen kjøre ferdig.
        runCurrent()
        assertFalse(model.uiState.value.isLoading)

        model.openEditor(GoalType.WATER)
        // Mellomrom rundt tallet skal godtas, siden vi trimmer.
        model.changeValue(" 2000 ")
        model.saveEditor()
        runCurrent()

        assertNull(model.uiState.value.editingType)
        assertEquals(2000, model.uiState.value.goalFor(GoalType.WATER)?.targetValue)
    }

    @Test
    fun save_rejectsValuesOutsideRange() = runTest {
        val model = GoalsViewModel(repository)
        runCurrent()
        model.openEditor(GoalType.STEPS)
        // Alt dette skal gi feilmelding og ikke lagre noe.
        for (input in listOf("", "abc", "0", "-5", "2.5", "999", "50001")) {
            model.changeValue(input)
            model.saveEditor()
            runCurrent()
            assertEquals(R.string.goals_invalid_steps, model.uiState.value.inputErrorResId)
            assertTrue(repository.goals.value.isEmpty())
        }
    }

    @Test
    fun edit_keepsIdAndChangesValue() = runTest {
        val model = GoalsViewModel(repository)
        runCurrent()
        model.openEditor(GoalType.WATER); model.changeValue("2000"); model.saveEditor(); runCurrent()
        val id = model.uiState.value.goalFor(GoalType.WATER)!!.id

        // Åpner på nytt: feltet skal være forhåndsutfylt med dagens verdi.
        model.openEditor(GoalType.WATER)
        assertEquals("2000", model.uiState.value.valueInput)
        model.changeValue("2500"); model.saveEditor(); runCurrent()

        // Samme ID, ny verdi, og fortsatt bare ett vannmål.
        val goal = model.uiState.value.goalFor(GoalType.WATER)!!
        assertEquals(id, goal.id)
        assertEquals(2500, goal.targetValue)
        assertEquals(1, model.uiState.value.goals.size)
    }

    @Test
    fun delete_removesGoal() = runTest {
        val model = GoalsViewModel(repository)
        runCurrent()
        model.openEditor(GoalType.STEPS); model.changeValue("8000"); model.saveEditor(); runCurrent()

        model.deleteGoal(model.uiState.value.goalFor(GoalType.STEPS)!!)
        runCurrent()

        assertNull(model.uiState.value.goalFor(GoalType.STEPS))
        assertTrue(repository.goals.value.isEmpty())
    }

    @Test
    fun writeFailure_keepsEditorOpenAndShowsError() = runTest {
        val model = GoalsViewModel(repository)
        runCurrent()
        repository.failWrite = true
        model.openEditor(GoalType.WATER); model.changeValue("2000"); model.saveEditor(); runCurrent()

        // Feltet skal fortsatt være åpent med det brukeren skrev, så det går an å prøve igjen.
        assertEquals(GoalType.WATER, model.uiState.value.editingType)
        assertEquals("2000", model.uiState.value.valueInput)
        assertEquals(R.string.goals_save_error, model.uiState.value.errorMessageResId)
        assertFalse(model.uiState.value.isSaving)
    }

    @Test
    fun loadFailure_canBeRetried() = runTest {
        repository.failRead = true
        val model = GoalsViewModel(repository)
        runCurrent()
        assertTrue(model.uiState.value.loadFailed)
        assertEquals(R.string.goals_load_error, model.uiState.value.errorMessageResId)

        // Når databasen virker igjen, skal «Prøv igjen» få listen tilbake.
        repository.failRead = false
        model.retry()
        runCurrent()
        assertFalse(model.uiState.value.loadFailed)
        assertNull(model.uiState.value.errorMessageResId)
    }
}

// Falskt repository som holder målene i en liste i minnet.
// Lar oss teste ViewModel-en uten Room, og late som databasen feiler.
private class FakeGoalRepository : GoalRepository {
    val goals = MutableStateFlow<List<GoalEntity>>(emptyList())
    var failRead = false
    var failWrite = false
    private var nextId = 1L

    override fun observeGoals(userId: Long): Flow<List<GoalEntity>> {
        if (failRead) return flow { error("Test: kunne ikke lese") }
        return goals.map { all -> all.filter { it.userId == userId && it.isActive } }
    }

    override fun observeGoal(userId: Long, type: GoalType) =
        observeGoals(userId).map { all -> all.firstOrNull { it.type == type } }

    override suspend fun saveGoal(userId: Long, type: GoalType, targetValue: Int): Long {
        // check() kaster feil hvis failWrite er true, slik Room ville gjort ved diskfeil.
        check(!failWrite)
        val existing = goals.value.firstOrNull { it.userId == userId && it.type == type }
        return if (existing == null) {
            val id = nextId++
            goals.value += GoalEntity(id = id, userId = userId, type = type, targetValue = targetValue)
            id
        } else {
            goals.value = goals.value.map { if (it.id == existing.id) it.copy(targetValue = targetValue) else it }
            existing.id
        }
    }

    override suspend fun deleteGoal(goal: GoalEntity) {
        check(!failWrite)
        goals.value = goals.value.filterNot { it.id == goal.id }
    }
}