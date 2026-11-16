package no.gruppe7.amate.feature.progress.ui

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
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.goals.data.local.GoalEntity
import no.gruppe7.amate.feature.goals.data.local.GoalType
import no.gruppe7.amate.feature.water.data.WaterRepository
import no.gruppe7.amate.feature.water.data.local.WaterEntryEntity
import no.gruppe7.amate.feature.steps.data.StepsRepository
import no.gruppe7.amate.feature.steps.data.local.DailyStepsEntity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {
    private val store = ViewModelStore()
    private val water = FakeWater()
    private val goals = FakeGoals()
    private val clock = MutableClock(Instant.parse("2026-11-09T12:00:00Z"))
    private var zone = ZoneId.of("Europe/Oslo")

    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    private fun model(stepsRepository: StepsRepository? = null) = ProgressViewModel(water, goals,
        stepsRepository = stepsRepository, clock = clock, zoneProvider = { zone })
        .also { store.put("progress", it) }

    private fun progressTest(block: suspend TestScope.() -> Unit) = runTest {
        try { block() } finally {
            // Stopper midnattsløkken før testklokken prøver å kjøre den videre for alltid.
            store.clear()
            runCurrent()
        }
    }

    private fun entry(amount: Int, time: String = "2026-11-09T11:00:00Z", userId: Long = 1, id: Long = 1) =
        WaterEntryEntity(id, userId, amount, Instant.parse(time).toEpochMilli())

    @Test fun loadingIsUnknownRatherThanPretendZero() = progressTest {
        val model = model()
        assertTrue(model.uiState.value.isLoading)
        assertNull(model.uiState.value.totalTodayMl)
        assertNull(model.uiState.value.progress)
    }

    @Test fun groupsSevenDaysAndLeavesZeroDaysInPlace() = progressTest {
        water.entries.value = listOf(
            entry(500), entry(250, id = 2), entry(700, "2026-11-03T12:00:00Z", id = 3),
            entry(900, "2026-11-02T12:00:00Z", id = 4), // For gammel.
            entry(800, "2026-11-10T00:00:00Z", id = 5), // Neste dag.
            entry(600, userId = 2, id = 6), // En annen bruker.
        )
        val model = model()
        runCurrent()
        val state = model.uiState.value
        assertEquals(7, state.days.size)
        assertEquals("2026-11-03", state.days.first().date.toString())
        assertEquals(700L, state.days.first().totalMl)
        assertEquals(5, state.days.count { it.totalMl == 0L })
        assertEquals(750L, state.totalTodayMl)
        assertEquals(1450L, state.totalWeekMl)
        assertEquals(207L, state.averageDailyMl)
        assertEquals(1L, water.requestedUser)
        assertEquals(1L, goals.requestedUser)
        assertEquals(GoalType.WATER, goals.requestedType)
    }

    @Test fun addEditAndDeleteAutomaticallyChangeTotals() = progressTest {
        val model = model()
        runCurrent()
        assertEquals(0L, model.uiState.value.totalTodayMl)
        water.entries.value = listOf(entry(250))
        runCurrent()
        assertEquals(250L, model.uiState.value.totalTodayMl)
        water.entries.value = listOf(entry(500))
        runCurrent()
        assertEquals(500L, model.uiState.value.totalWeekMl)
        water.entries.value = emptyList()
        runCurrent()
        assertEquals(0L, model.uiState.value.totalWeekMl)
    }

    @Test fun goalEditsAndDeletionImmediatelyChangePercentage() = progressTest {
        water.entries.value = listOf(entry(1000))
        goals.goal.value = GoalEntity(userId = 1, targetValue = 2000)
        val model = model()
        runCurrent()
        assertEquals(50L, model.uiState.value.percent)
        assertEquals(1000L, model.uiState.value.remainingMl)
        goals.goal.value = goals.goal.value!!.copy(targetValue = 1000)
        runCurrent()
        assertEquals(100L, model.uiState.value.percent)
        assertEquals(0L, model.uiState.value.remainingMl)
        goals.goal.value = null
        runCurrent()
        assertNull(model.uiState.value.goalMl)
        assertNull(model.uiState.value.percent)
        assertEquals(1000L, model.uiState.value.totalTodayMl)
    }

    @Test fun missingInactiveAndInvalidGoalsDoNotInventADefault() = progressTest {
        val model = model()
        runCurrent()
        for (goal in listOf(null, GoalEntity(targetValue = 2000, isActive = false), GoalEntity(targetValue = 0))) {
            goals.goal.value = goal
            runCurrent()
            assertNull(model.uiState.value.goalMl)
            assertNull(model.uiState.value.progress)
            assertFalse(model.uiState.value.goalLoadFailed)
        }
    }

    @Test fun overGoalPercentageIsHonestAndMeterStopsAtOne() = progressTest {
        water.entries.value = listOf(entry(2500))
        goals.goal.value = GoalEntity(targetValue = 2000)
        val model = model()
        runCurrent()
        assertEquals(125L, model.uiState.value.percent)
        assertEquals(1f, model.uiState.value.progress!!, 0f)
        assertEquals(0L, model.uiState.value.remainingMl)
    }

    @Test fun waterErrorIsNotZeroAndCanBeRetried() = progressTest {
        water.fail = true
        goals.goal.value = GoalEntity(targetValue = 2000)
        val model = model()
        runCurrent()
        assertTrue(model.uiState.value.waterLoadFailed)
        assertNull(model.uiState.value.totalTodayMl)
        assertNull(model.uiState.value.progress)
        assertEquals(2000, model.uiState.value.goalMl)
        water.fail = false
        water.entries.value = listOf(entry(375))
        model.retry()
        runCurrent()
        assertFalse(model.uiState.value.waterLoadFailed)
        assertEquals(375L, model.uiState.value.totalTodayMl)
    }

    @Test fun goalErrorDoesNotHideWaterAndCanBeRetried() = progressTest {
        water.entries.value = listOf(entry(500))
        goals.fail = true
        val model = model()
        runCurrent()
        assertTrue(model.uiState.value.goalLoadFailed)
        assertEquals(500L, model.uiState.value.totalWeekMl)
        assertNull(model.uiState.value.progress)
        goals.fail = false
        goals.goal.value = GoalEntity(targetValue = 2000)
        model.retry()
        runCurrent()
        assertFalse(model.uiState.value.goalLoadFailed)
        assertEquals(25L, model.uiState.value.percent)
    }

    @Test fun daylightSavingDayUsesLocalMidnightsAndExcludesNextDay() = progressTest {
        clock.now = Instant.parse("2026-10-25T12:00:00Z")
        water.entries.value = listOf(
            entry(200, "2026-10-24T22:00:00Z"), // Lokal midnatt før klokken stilles.
            entry(300, "2026-10-25T22:59:59Z", id = 2),
            entry(900, "2026-10-25T23:00:00Z", id = 3), // Lokal midnatt neste dag, skal ikke med.
        )
        val model = model()
        runCurrent()
        assertEquals(Instant.parse("2026-10-18T22:00:00Z").toEpochMilli(), water.startMillis)
        assertEquals(Instant.parse("2026-10-25T23:00:00Z").toEpochMilli(), water.endMillis)
        assertEquals(500L, model.uiState.value.totalTodayMl)
    }

    @Test fun midnightMovesTodayIntoYesterdayWithoutLosingHistory() = progressTest {
        clock.now = Instant.parse("2026-11-09T22:59:50Z")
        water.entries.value = listOf(entry(500))
        val model = model()
        runCurrent()
        clock.now = Instant.parse("2026-11-09T23:00:20Z")
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals("2026-11-10", model.uiState.value.date.toString())
        assertEquals(0L, model.uiState.value.totalTodayMl)
        assertEquals(500L, model.uiState.value.days[5].totalMl)
    }

    @Test fun changedTimezoneRegroupsEntriesOnResume() = progressTest {
        clock.now = Instant.parse("2026-11-09T23:30:00Z")
        water.entries.value = listOf(entry(500, "2026-11-09T23:15:00Z"))
        val model = model()
        runCurrent()
        assertEquals("2026-11-10", model.uiState.value.date.toString())
        zone = ZoneOffset.UTC
        model.refreshDay()
        runCurrent()
        assertEquals("2026-11-09", model.uiState.value.date.toString())
        assertEquals(500L, model.uiState.value.totalTodayMl)
    }

    @Test fun totalsUseLongRatherThanOverflowingInt() = progressTest {
        water.entries.value = listOf(entry(Int.MAX_VALUE), entry(Int.MAX_VALUE, id = 2))
        val model = model()
        runCurrent()
        assertEquals(4_294_967_294L, model.uiState.value.totalWeekMl)
    }

    @Test fun stepsHaveTheirOwnSevenDayHistoryAndUser() = progressTest {
        val steps = FakeSteps()
        val today = java.time.LocalDate.of(2026, 11, 9).toEpochDay()
        steps.history.value = listOf(
            DailyStepsEntity(1, today, 4000), DailyStepsEntity(1, today - 6, 3000),
            DailyStepsEntity(1, today - 7, 9000), DailyStepsEntity(1, today + 1, 8000), DailyStepsEntity(2, today, 7000),
        )
        val model = model(steps)
        assertNull(model.uiState.value.steps!!.totalToday)
        runCurrent()
        val state = model.uiState.value.steps!!
        assertEquals(1L, steps.requestedUser)
        assertEquals(7, state.days.size)
        assertEquals(4000L, state.totalToday)
        assertEquals(7000L, state.totalWeek)
        assertEquals(1000L, state.averageDaily)
        assertEquals(5, state.days.count { it.totalSteps == 0L })
    }

    @Test fun waterAndStepsUseDifferentGoalsAndReactToChanges() = progressTest {
        val steps = FakeSteps()
        val today = java.time.LocalDate.of(2026, 11, 9).toEpochDay()
        water.entries.value = listOf(entry(500))
        goals.goal.value = GoalEntity(targetValue = 2000)
        goals.stepsGoal.value = GoalEntity(type = GoalType.STEPS, targetValue = 10_000)
        steps.history.value = listOf(DailyStepsEntity(1, today, 4000))
        val model = model(steps)
        runCurrent()
        assertEquals(25L, model.uiState.value.percent)
        assertEquals(40L, model.uiState.value.steps!!.percent)
        goals.stepsGoal.value = goals.stepsGoal.value!!.copy(targetValue = 8000)
        steps.history.value = listOf(DailyStepsEntity(1, today, 6000))
        runCurrent()
        assertEquals(75L, model.uiState.value.steps!!.percent)
        assertEquals(2000L, model.uiState.value.steps!!.remaining)
        assertEquals(25L, model.uiState.value.percent)
        goals.stepsGoal.value = null
        runCurrent()
        assertNull(model.uiState.value.steps!!.percent)
        assertEquals(6000L, model.uiState.value.steps!!.totalToday)
    }

    @Test fun stepsReadErrorDoesNotStopWaterAndRetryRecovers() = progressTest {
        val steps = FakeSteps().also { it.fail = true }
        water.entries.value = listOf(entry(500))
        val model = model(steps)
        runCurrent()
        assertTrue(model.uiState.value.steps!!.loadFailed)
        assertNull(model.uiState.value.steps!!.totalToday)
        assertEquals(500L, model.uiState.value.totalTodayMl)
        steps.fail = false
        model.retry()
        runCurrent()
        assertFalse(model.uiState.value.steps!!.loadFailed)
        assertEquals(0L, model.uiState.value.steps!!.totalToday)
    }

    @Test fun stepsGoalErrorDoesNotStopWaterGoalOrStepsHistory() = progressTest {
        val steps = FakeSteps()
        goals.goal.value = GoalEntity(targetValue = 2000)
        goals.stepsFail = true
        val model = model(steps)
        runCurrent()
        assertTrue(model.uiState.value.steps!!.goalLoadFailed)
        assertEquals(0L, model.uiState.value.steps!!.totalToday)
        assertEquals(2000, model.uiState.value.goalMl)
        assertFalse(model.uiState.value.goalLoadFailed)
        goals.stepsFail = false
        goals.stepsGoal.value = GoalEntity(type = GoalType.STEPS, targetValue = 5000)
        model.retry()
        runCurrent()
        assertEquals(5000, model.uiState.value.steps!!.goal)
        assertFalse(model.uiState.value.steps!!.goalLoadFailed)
    }

    @Test fun waterErrorDoesNotStopStepsProgress() = progressTest {
        water.fail = true
        val steps = FakeSteps()
        goals.stepsGoal.value = GoalEntity(type = GoalType.STEPS, targetValue = 5000)
        val model = model(steps)
        runCurrent()
        assertTrue(model.uiState.value.waterLoadFailed)
        assertEquals(0L, model.uiState.value.steps!!.percent)
        assertFalse(model.uiState.value.steps!!.loadFailed)
    }

    @Test fun midnightMovesStepsToYesterdayAndDropsTheEighthDay() = progressTest {
        clock.now = Instant.parse("2026-11-09T22:59:50Z")
        val steps = FakeSteps()
        val today = java.time.LocalDate.of(2026, 11, 9).toEpochDay()
        steps.history.value = listOf(DailyStepsEntity(1, today, 4000), DailyStepsEntity(1, today - 6, 3000))
        val model = model(steps)
        runCurrent()
        clock.now = Instant.parse("2026-11-09T23:00:20Z")
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(0L, model.uiState.value.steps!!.totalToday)
        assertEquals(4000L, model.uiState.value.steps!!.days[5].totalSteps)
        assertEquals(4000L, model.uiState.value.steps!!.totalWeek)
    }

    @Test fun stepsMeterCapsAtOneButPercentageMayExceedOneHundred() = progressTest {
        val steps = FakeSteps()
        steps.history.value = listOf(DailyStepsEntity(1, java.time.LocalDate.of(2026, 11, 9).toEpochDay(), 12_500))
        goals.stepsGoal.value = GoalEntity(type = GoalType.STEPS, targetValue = 10_000)
        val model = model(steps)
        runCurrent()
        assertEquals(125L, model.uiState.value.steps!!.percent)
        assertEquals(1f, model.uiState.value.steps!!.progress!!, 0f)
        assertEquals(0L, model.uiState.value.steps!!.remaining)
    }

    @Test fun missingInactiveAndZeroStepsGoalsHaveNoInventedDefault() = progressTest {
        val model = model(FakeSteps())
        runCurrent()
        for (goal in listOf(null, GoalEntity(type = GoalType.STEPS, targetValue = 5000, isActive = false),
            GoalEntity(type = GoalType.STEPS, targetValue = 0))) {
            goals.stepsGoal.value = goal
            runCurrent()
            assertNull(model.uiState.value.steps!!.goal)
            assertNull(model.uiState.value.steps!!.progress)
        }
    }

    private class FakeSteps : StepsRepository {
        val history = MutableStateFlow<List<DailyStepsEntity>>(emptyList())
        var fail = false
        var requestedUser = -1L
        override val isSensorAvailable = false
        override fun getStepsHistory(userId: Long): Flow<List<DailyStepsEntity>> {
            requestedUser = userId
            return if (fail) flow { error("Test: skrittfeil") } else history
        }
        override fun getStepsForToday(userId: Long, epochDay: Long) = history.map { list ->
            list.find { it.userId == userId && it.epochDay == epochDay }?.stepCount ?: 0
        }
        override suspend fun incrementStep(userId: Long, count: Int, epochDay: Long) = error("Brukes ikke")
    }

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }

    // Enkle testkilder: gir oss kontroll på data og feil uten å måtte starte Android/Room.
    private class FakeWater : WaterRepository {
        val entries = MutableStateFlow<List<WaterEntryEntity>>(emptyList())
        var fail = false
        var requestedUser = -1L
        var startMillis = 0L
        var endMillis = 0L
        override fun observeEntriesForDay(userId: Long, startMillis: Long, endMillis: Long): Flow<List<WaterEntryEntity>> {
            requestedUser = userId
            this.startMillis = startMillis
            this.endMillis = endMillis
            return if (fail) flow { error("Test: vannfeil") } else entries.map { list ->
                list.filter { it.userId == userId && it.loggedAtMillis >= startMillis && it.loggedAtMillis < endMillis }
            }
        }
        override fun observeEntries(userId: Long) = entries
        override fun observeTotalForDay(userId: Long, startMillis: Long, endMillis: Long) =
            observeEntriesForDay(userId, startMillis, endMillis).map { list -> list.sumOf { it.amountMl } }
        override suspend fun addWater(userId: Long, amountMl: Int, loggedAtMillis: Long): Long = error("Brukes ikke")
        override suspend fun update(entry: WaterEntryEntity) = error("Brukes ikke")
        override suspend fun delete(entry: WaterEntryEntity) = error("Brukes ikke")
    }

    private class FakeGoals : GoalRepository {
        val goal = MutableStateFlow<GoalEntity?>(null)
        val stepsGoal = MutableStateFlow<GoalEntity?>(null)
        var stepsFail = false
        var fail = false
        var requestedUser = -1L
        var requestedType: GoalType? = null
        override fun observeGoal(userId: Long, type: GoalType): Flow<GoalEntity?> {
            requestedUser = userId
            requestedType = type
            return if (type == GoalType.STEPS) {
                if (stepsFail) flow { error("Test: skrittmålfeil") } else stepsGoal
            } else if (fail) flow { error("Test: målfeil") } else goal
        }
        override fun observeGoals(userId: Long) = goal.map { listOfNotNull(it) }
        override suspend fun saveGoal(userId: Long, type: GoalType, targetValue: Int): Long = error("Brukes ikke")
        override suspend fun deleteGoal(goal: GoalEntity) = error("Brukes ikke")
    }
}
