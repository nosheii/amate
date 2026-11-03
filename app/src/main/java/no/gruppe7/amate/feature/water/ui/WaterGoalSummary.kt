package no.gruppe7.amate.feature.water.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale
import no.gruppe7.amate.R

// Samme lille målvisning på begge skjermer, så de ikke gir forskjellige beskjeder.
@Composable
internal fun WaterGoalSummary(state: WaterUiState, onRetry: () -> Unit, tagPrefix: String = "water") {
    val numbers = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("nb-NO")) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            state.isGoalLoading -> Text(stringResource(R.string.water_goal_loading))
            state.goalLoadFailed -> {
                // En lesefeil er ikke det samme som at brukeren mangler et mål.
                Text(stringResource(R.string.water_goal_load_error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry, modifier = Modifier.testTag("${tagPrefix}_goal_retry")) {
                    Text(stringResource(R.string.water_retry))
                }
            }
            state.goalMl == null -> Text(stringResource(R.string.water_goal_missing),
                modifier = Modifier.testTag("${tagPrefix}_goal"))
            else -> {
                Text(stringResource(R.string.water_goal_value, numbers.format(state.goalMl)),
                    modifier = Modifier.testTag("${tagPrefix}_goal"))
                // Ingen tom eller oppdiktet måler mens vannloggen lastes eller har feilet.
                state.progress?.let { progress ->
                    val description = stringResource(R.string.water_progress, (progress * 100).toInt())
                    LinearProgressIndicator(progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).testTag("${tagPrefix}_progress")
                            .semantics { contentDescription = description },
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer)
                }
            }
        }
    }
}
