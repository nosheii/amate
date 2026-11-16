package no.gruppe7.amate.feature.progress.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import no.gruppe7.amate.R

internal data class ProgressChartDay(val date: LocalDate, val value: Long)

// Vi tar også med målet i skalaen. Da forsvinner ikke linjen hvis uka har lite eller ingen data.
// Double gir plass til store summer, og litt luft over høyeste søyle/mål gjør linjen lettere å se.
internal fun progressChartMaximum(days: List<ProgressChartDay>, goal: Int?): Double =
    maxOf(days.maxOfOrNull { it.value }?.toDouble() ?: 0.0, goal?.toDouble() ?: 0.0, 1.0) * 1.15

@Composable
internal fun ProgressHistorySection(
    days: List<ProgressChartDay>,
    goal: Int?,
    color: Color,
    titleRes: Int,
    amountRes: Int,
    descriptionRes: Int,
    tag: String,
    goalTag: String,
    numbers: NumberFormat,
    total: Long,
    average: Long,
    totalRes: Int,
    averageRes: Int,
    totalTag: String,
    averageTag: String,
    emptyRes: Int,
    emptyTag: String,
) {
    val usableGoal = goal?.takeIf { it > 0 }
    val goalLabel = usableGoal?.let {
        stringResource(R.string.progress_goal_reference, stringResource(amountRes, numbers.format(it)))
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Text(stringResource(titleRes), fontWeight = FontWeight.Bold, color = color,
                modifier = Modifier.weight(1f))
            if (goalLabel != null) {
                // Den lille streken ved teksten forklarer hva den stiplede linjen i grafen betyr.
                Canvas(Modifier.size(width = 18.dp, height = 12.dp)) {
                    drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                        strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())))
                }
                Text(goalLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (total == 0L) Text(stringResource(emptyRes), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag(emptyTag))
        ProgressHistoryChart(days, usableGoal, goalLabel, color, numbers, descriptionRes, tag, goalTag)
        Text(stringResource(totalRes, numbers.format(total)), style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold, modifier = Modifier.testTag(totalTag))
        Text(stringResource(averageRes, numbers.format(average)), style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag(averageTag))
    }
}

@Composable
private fun ProgressHistoryChart(
    days: List<ProgressChartDay>,
    goal: Int?,
    goalLabel: String?,
    color: Color,
    numbers: NumberFormat,
    descriptionRes: Int,
    tag: String,
    goalTag: String,
) {
    val shortDate = remember { DateTimeFormatter.ofPattern("EEE", Locale.forLanguageTag("nb-NO")) }
    val spokenDate = remember { DateTimeFormatter.ofPattern("EEEE d. MMMM", Locale.forLanguageTag("nb-NO")) }
    val maximum = progressChartMaximum(days, goal)
    val baseline = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxWidth().testTag(tag), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Ml og skritt får hver sin skala i samme kort, ikke en misvisende felles tallakse.
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom) {
                days.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                        // Null betyr ingen søyle. Vi legger ikke inn en minsteverdi som ser ut som ekte data.
                        Box(Modifier.fillMaxWidth(0.55f).height((64 * (day.value.coerceAtLeast(0) / maximum)).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)).background(color))
                    }
                }
            }
            if (goal != null && goalLabel != null) {
                Canvas(Modifier.fillMaxSize().testTag(goalTag).semantics { contentDescription = goalLabel }) {
                    val y = size.height * (1 - goal / maximum).toFloat()
                    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            days.forEach { day ->
                val description = stringResource(descriptionRes, day.date.format(spokenDate), numbers.format(day.value))
                Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = description },
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day.date.format(shortDate), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text(numbers.format(day.value), style = MaterialTheme.typography.labelSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
