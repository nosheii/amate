package no.gruppe7.amate.feature.steps.ui

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.steps.data.StepsRepository
import no.gruppe7.amate.feature.steps.data.local.DailyStepsEntity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StepsViewModelTest {

    private val store = ViewModelStore()
    private val stepsRepository = FakeStepsRepository()
    private val goalRepository = FakeGoalRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun TestScope.model(): StepsViewModel {
        return StepsViewModel(stepsRepository, goalRepository, userId = 1).also { vm ->
            store.put("steps", vm)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.uiState.collect {}
            }
        }
    }

    private fun stepsTest(block: suspend TestScope.() -> Unit) = runTest {
        try {
            block()
        } finally {
            store.clear()
            runCurrent()
        }
    }

    @Test
    fun loadsInitialStepDataAndSensorAvailability() = stepsTest {
        val viewModel = model()
        runCurrent()
        val state = viewModel.uiState.value
        assertEquals(0, state.stepsToday)
        assertTrue(state.sensorAvailable)
        assertFalse(state.permissionGranted)
    }

    @Test
    fun addStepIncrementsStepsForToday() = stepsTest {
        val viewModel = model()
        runCurrent()
        viewModel.addStep(100)
        runCurrent()
        assertEquals(100, viewModel.uiState.value.stepsToday)
    }

    @Test
    fun permissionResultUpdatesUiState() = stepsTest {
        val viewModel = model()
        runCurrent()
        viewModel.onPermissionResult(true)
        runCurrent()
        assertTrue(viewModel.uiState.value.permissionGranted)
    }

    @Test
    fun observesStepsGoalAndCalculatesProgress() = stepsTest {
        val viewModel = model()
        runCurrent()
        goalRepository.setGoal(GoalEntity(userId = 1, type = GoalType.STEPS, targetValue = 10000, isActive = true))
        runCurrent()
        assertEquals(10000, viewModel.uiState.value.stepGoal)
        assertEquals(0f, viewModel.uiState.value.progress)

        viewModel.addStep(5000)
        runCurrent()
        assertEquals(0.5f, viewModel.uiState.value.progress)
    }
}

private class FakeStepsRepository : StepsRepository {
    private val stepsFlow = MutableStateFlow(0)
    private val historyFlow = MutableStateFlow<List<DailyStepsEntity>>(emptyList())
    override val isSensorAvailable: Boolean = true

    override fun getStepsForToday(userId: Long, epochDay: Long): Flow<Int> = stepsFlow

    override fun getStepsHistory(userId: Long): Flow<List<DailyStepsEntity>> = historyFlow

    override suspend fun incrementStep(userId: Long, count: Int, epochDay: Long) {
        stepsFlow.value += count
        val currentHistory = historyFlow.value.toMutableList()
        val existingIndex = currentHistory.indexOfFirst { it.epochDay == epochDay }
        if (existingIndex >= 0) {
            val existing = currentHistory[existingIndex]
            currentHistory[existingIndex] = existing.copy(stepCount = existing.stepCount + count)
        } else {
            currentHistory.add(DailyStepsEntity(userId, epochDay, count))
        }
        historyFlow.value = currentHistory
    }
}

private class FakeGoalRepository : GoalRepository {
    private val goalsFlow = MutableStateFlow<Map<GoalType, GoalEntity>>(emptyMap())

    fun setGoal(goal: GoalEntity) {
        goalsFlow.value = goalsFlow.value + (goal.type to goal)
    }

    override fun observeGoals(userId: Long): Flow<List<GoalEntity>> =
        goalsFlow.map { it.values.toList() }

    override fun observeGoal(userId: Long, type: GoalType): Flow<GoalEntity?> =
        goalsFlow.map { it[type] }

    override suspend fun saveGoal(userId: Long, type: GoalType, targetValue: Int): Long {
        setGoal(GoalEntity(userId = userId, type = type, targetValue = targetValue, isActive = true))
        return 1L
    }

    override suspend fun deleteGoal(goal: GoalEntity) {
        goalsFlow.value = goalsFlow.value - goal.type
    }
}
