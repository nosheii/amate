package no.gruppe7.amate.feature.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import no.gruppe7.amate.R

// Denne delen viser bare skritt fra modellen. Den starter ikke sensoren eller skriver i Room.
@Composable
internal fun StepsTodaySummary(
    state: StepsProgressUiState,
    numbers: NumberFormat,
    onRetry: () -> Unit,
    onOpenSteps: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.progress_tab_steps), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.size(24.dp).testTag("progress_steps_loading"))
            state.loadFailed -> RetryMessage(R.string.progress_steps_error, onRetry, "progress_steps_retry")
            else -> Text(stringResource(R.string.progress_steps_amount, numbers.format(state.totalToday)),
                style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("progress_steps_today_total"))
        }
        when {
            state.isGoalLoading -> Text(stringResource(R.string.progress_steps_goal_loading), style = MaterialTheme.typography.bodySmall)
            state.goalLoadFailed -> RetryMessage(R.string.progress_steps_goal_error, onRetry, "progress_steps_goal_retry")
            state.goal == null || state.goal <= 0 -> Text(stringResource(R.string.progress_steps_missing_goal),
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("progress_steps_missing_goal"))
            else -> {
                Text(stringResource(R.string.progress_steps_goal, numbers.format(state.goal)),
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("progress_steps_goal"))
                // Prosenten bruker skrittmålet, aldri vannmålet i kolonnen ved siden av.
                state.progress?.let { progress ->
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp)
                        .testTag("progress_steps_meter"), color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.progress_percent, numbers.format(state.percent)),
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("progress_steps_percent"))
                    if (state.remaining == 0L) Text(stringResource(R.string.progress_goal_reached), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        TextButton(onClick = onOpenSteps, modifier = Modifier.testTag("progress_open_steps")) {
            Text(stringResource(R.string.progress_steps_shortcut))
        }
    }
}
