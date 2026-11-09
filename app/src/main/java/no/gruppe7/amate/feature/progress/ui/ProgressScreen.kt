package no.gruppe7.amate.feature.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.steps.data.StepsRepository
import no.gruppe7.amate.feature.water.data.WaterRepository

@Composable
fun ProgressRoute(
    waterRepository: WaterRepository,
    goalRepository: GoalRepository,
    onOpenGoals: () -> Unit,
    onOpenWater: () -> Unit,
    stepsRepository: StepsRepository? = null,
    onOpenSteps: () -> Unit = {},
) {
    val factory = remember(waterRepository, goalRepository, stepsRepository) {
        ProgressViewModel.factory(waterRepository, goalRepository, stepsRepository)
    }
    val model: ProgressViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.refreshDay() }
    ProgressScreen(state, model::retry, onOpenGoals, onOpenWater, onOpenSteps = onOpenSteps)
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onRetry: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenWater: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSteps: () -> Unit = {},
) {
    val numbers = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("nb-NO")) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("d. MMMM", Locale.forLanguageTag("nb-NO")) }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("progress_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ProgressCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.progress_today, state.date.format(dateFormat)),
                            fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = onOpenGoals, modifier = Modifier.testTag("progress_open_goals")) {
                            Text(stringResource(R.string.progress_goals_shortcut))
                        }
                    }
                    // Begge vises samtidig. Smal skjerm eller stor tekst? Da legger vi dem under hverandre.
                    BoxWithConstraints {
                        val stack = maxWidth < 300.dp || LocalDensity.current.fontScale > 1.3f
                        if (stack) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                WaterTodaySummary(state, numbers, onRetry, onOpenWater)
                                state.steps?.let { StepsTodaySummary(it, numbers, onRetry, onOpenSteps) }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(Modifier.weight(1f)) {
                                    WaterTodaySummary(state, numbers, onRetry, onOpenWater)
                                }
                                state.steps?.let {
                                    Column(Modifier.weight(1f)) {
                                        StepsTodaySummary(it, numbers, onRetry, onOpenSteps)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // En feil i skritt skal fortsatt ikke skjule vannhistorikken, eller motsatt.
            val hasWater = !state.isLoading && !state.waterLoadFailed
            val hasSteps = state.steps?.let { !it.isLoading && !it.loadFailed } == true
            if (hasWater || hasSteps) {
                item {
                    ProgressCard {
                        Text(stringResource(R.string.progress_combined_week), style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.progress_week_dates,
                            state.date.minusDays(6).format(dateFormat), state.date.format(dateFormat)),
                            style = MaterialTheme.typography.bodySmall)
                        if (hasWater) {
                            ProgressHistorySection(
                                days = state.days.map { ProgressChartDay(it.date, it.totalMl) },
                                goal = state.goalMl?.takeIf { it > 0 && !state.isGoalLoading && !state.goalLoadFailed },
                                color = MaterialTheme.colorScheme.secondary,
                                titleRes = R.string.progress_tab_water, amountRes = R.string.progress_amount,
                                descriptionRes = R.string.progress_day_description, tag = "progress_history",
                                goalTag = "progress_water_goal_line", numbers = numbers,
                                total = state.totalWeekMl ?: 0L, average = state.averageDailyMl ?: 0L,
                                totalRes = R.string.progress_week_total, averageRes = R.string.progress_week_average,
                                totalTag = "progress_week_total", averageTag = "progress_week_average",
                                emptyRes = R.string.progress_empty, emptyTag = "progress_empty",
                            )
                        }
                        state.steps?.takeIf { hasSteps }?.let { steps ->
                            ProgressHistorySection(
                                days = steps.days.map { ProgressChartDay(it.date, it.totalSteps) },
                                goal = steps.goal?.takeIf { it > 0 && !steps.isGoalLoading && !steps.goalLoadFailed },
                                color = MaterialTheme.colorScheme.primary,
                                titleRes = R.string.progress_tab_steps, amountRes = R.string.progress_steps_amount,
                                descriptionRes = R.string.progress_steps_day_description, tag = "progress_steps_history",
                                goalTag = "progress_steps_goal_line", numbers = numbers,
                                total = steps.totalWeek ?: 0L, average = steps.averageDaily ?: 0L,
                                totalRes = R.string.progress_steps_week_total, averageRes = R.string.progress_steps_average,
                                totalTag = "progress_steps_week_total", averageTag = "progress_steps_average",
                                emptyRes = R.string.progress_steps_empty, emptyTag = "progress_steps_empty",
                            )
                        }
                        // Gamle målverdier lagres ikke. Linjene viser dagens mål, ikke hva målet var før.
                        Text(stringResource(R.string.progress_chart_note), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterTodaySummary(
    state: ProgressUiState,
    numbers: NumberFormat,
    onRetry: () -> Unit,
    onOpenWater: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.progress_tab_water), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.size(24.dp).testTag("progress_loading"))
            state.waterLoadFailed -> RetryMessage(R.string.progress_water_error, onRetry, "progress_water_retry")
            else -> Text(stringResource(R.string.progress_amount, numbers.format(state.totalTodayMl)),
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("progress_today_total"))
        }
        when {
            state.isGoalLoading -> Text(stringResource(R.string.water_goal_loading), style = MaterialTheme.typography.bodySmall)
            state.goalLoadFailed -> RetryMessage(R.string.water_goal_load_error, onRetry, "progress_goal_retry")
            state.goalMl == null || state.goalMl <= 0 -> Text(stringResource(R.string.water_goal_missing),
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("progress_missing_goal"))
            else -> {
                Text(stringResource(R.string.water_goal_value, numbers.format(state.goalMl)),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("progress_goal"))
                state.progress?.let { progress ->
                    LinearProgressIndicator(progress = { progress }, color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth().height(6.dp).testTag("progress_today_meter"))
                    Text(stringResource(R.string.progress_percent, numbers.format(state.percent)),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("progress_today_percent"))
                    if (state.remainingMl == 0L) Text(stringResource(R.string.progress_goal_reached), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        TextButton(onClick = onOpenWater, modifier = Modifier.testTag("progress_open_water")) {
            Text(stringResource(R.string.progress_register_water))
        }
    }
}

@Composable
internal fun ProgressCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun RetryMessage(messageRes: Int, onRetry: () -> Unit, tag: String) {
    Text(stringResource(messageRes), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = onRetry, modifier = Modifier.testTag(tag)) { Text(stringResource(R.string.water_retry)) }
}
