package no.gruppe7.amate.feature.progress.ui

import java.time.LocalDate
import java.time.ZoneId

// Én søyle i historikken. Vi summerer i Long, så mange registreringer ikke gir talloverløp.
data class WaterProgressDay(val date: LocalDate, val totalMl: Long)

data class StepsProgressDay(val date: LocalDate, val totalSteps: Long)

// Skritt har egen lastestatus. En feil her skal ikke skjule vannfremgangen, eller motsatt.
data class StepsProgressUiState(
    val date: LocalDate = LocalDate.now(),
    val days: List<StepsProgressDay> = emptyList(),
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val goal: Int? = null,
    val isGoalLoading: Boolean = true,
    val goalLoadFailed: Boolean = false,
) {
    val totalToday: Long? get() = if (isLoading || loadFailed) null else days.find { it.date == date }?.totalSteps ?: 0L
    val totalWeek: Long? get() = if (isLoading || loadFailed) null else days.sumOf { it.totalSteps }
    val averageDaily: Long? get() = totalWeek?.div(7)
    private val usableGoal: Int? get() = goal?.takeIf { it > 0 && !isGoalLoading && !goalLoadFailed }
    val percent: Long? get() = usableGoal?.let { goal -> totalToday?.let { it * 100 / goal } }
    val progress: Float? get() = usableGoal?.let { goal -> totalToday?.let { (it.toFloat() / goal).coerceIn(0f, 1f) } }
    val remaining: Long? get() = usableGoal?.let { goal -> totalToday?.let { (goal - it).coerceAtLeast(0L) } }
}

data class ProgressUiState(
    val date: LocalDate = LocalDate.now(),
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val days: List<WaterProgressDay> = emptyList(),
    val isLoading: Boolean = true,
    val waterLoadFailed: Boolean = false,
    val goalMl: Int? = null,
    val isGoalLoading: Boolean = true,
    val goalLoadFailed: Boolean = false,
    // null brukes bare når en forhåndsvisning/test kobler inn vann alene.
    val steps: StepsProgressUiState? = null,
) {
    // null betyr at vi ikke vet tallet ennå. Det skal ikke se ut som «0 ml» ved en lesefeil.
    val totalTodayMl: Long? get() = if (isLoading || waterLoadFailed) null else days.find { it.date == date }?.totalMl ?: 0L
    val totalWeekMl: Long? get() = if (isLoading || waterLoadFailed) null else days.sumOf { it.totalMl }
    // Dette er sju kalenderdager, inkludert i dag og dager uten registreringer.
    val averageDailyMl: Long? get() = totalWeekMl?.div(7)

    private val usableGoal: Int? get() = goalMl?.takeIf { it > 0 && !isGoalLoading && !goalLoadFailed }
    val percent: Long? get() = usableGoal?.let { goal -> totalTodayMl?.let { it * 100 / goal } }
    // Søylen stopper på 100 %, men tallet kan være høyere hvis man har registrert mer enn målet.
    val progress: Float? get() = usableGoal?.let { goal -> totalTodayMl?.let { (it.toFloat() / goal).coerceIn(0f, 1f) } }
    val remainingMl: Long? get() = usableGoal?.let { goal -> totalTodayMl?.let { (goal - it).coerceAtLeast(0L) } }
}
