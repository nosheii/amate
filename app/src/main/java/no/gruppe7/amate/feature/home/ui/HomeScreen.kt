package no.gruppe7.amate.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.tasks.ui.TasksUiState
import no.gruppe7.amate.feature.profile.ui.AvatarImage
import no.gruppe7.amate.feature.steps.ui.StepsUiState
import no.gruppe7.amate.feature.water.ui.WaterGoalSummary
import no.gruppe7.amate.feature.water.ui.WaterUiState
import no.gruppe7.amate.feature.weather.ui.WeatherCard
import java.text.NumberFormat
import java.util.Locale

// Foreløpige farger til skallet. Vi endrer ikke det felles designsystemet til gruppa.
private val Cream = Color(0xFFFFFBF1)
private val Ink = Color(0xFF493C2E)
private val Moss = Color(0xFF617D42)
private val Sky = Color(0xFFD9EDF7)
private val WaterBlue = Color(0xFF4A7791)

@Composable
internal fun HomeShellTheme(content: @Composable () -> Unit) {
    // Faste, lyse farger gjør at prototypen ligner mockupen også med mørk telefoninnstilling.
    // Dette gjelder bare skallet. Vannskjermen beholder sitt eget eksisterende tema.
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Moss, onPrimary = Color.White,
            secondary = WaterBlue, background = Cream, surface = Cream,
            onBackground = Ink, onSurface = Ink, onSurfaceVariant = Color(0xFF726654),
            secondaryContainer = Color(0xFFE5ECD7), onSecondaryContainer = Ink,
        ),
        content = content,
    )
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenWater: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenGoals: () -> Unit = {},
    onOpenSteps: () -> Unit = {},
    onOpenTasks: () -> Unit = {},
    waterState: WaterUiState = WaterUiState(),
    stepsState: StepsUiState = StepsUiState(),
    onRetryWater: () -> Unit = {},
    weatherContent: @Composable () -> Unit = { WeatherCard() },
    avatarKey: String = "",
    tasksState: TasksUiState = TasksUiState(),
    onRetryTasks: () -> Unit = {},
    onAddTask: () -> Unit = onOpenTasks,
) {
    // Forsiden får en ferdig tilstand fra vannmodellen. Jeg gjør ikke egne databasekall her.
    // Da kan vannkortet vise ekte tall, mens resten av forsiden fortsatt er et skall.
    val numbers = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("nb-NO")) }
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Sky, Color(0xFFF5F4DC), Color(0xFFE3EACF)))
        ), contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("home_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("AMATE", fontWeight = FontWeight.ExtraBold, color = Moss,
                            style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodySmall)
                    }
                    Surface(shape = CircleShape, color = Cream, shadowElevation = 2.dp) {
                        IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("home_settings")) {
                            Icon(HomeIcons.Settings, stringResource(R.string.home_open_settings), tint = Ink)
                        }
                    }
                }
            }
            item {
                // Skiltet i tegningen skaleres til kortets bredde, også på større skjermer.
                // Dyrene har ulik luft rundt seg, derfor bruker vi utsnittet for valgt bilde.
                val hasAvatar = avatarKey.isNotBlank()
                val frame = avatarFrame(avatarKey)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val imageSize = maxWidth / frame.signWidth
                    val trimTop = imageSize * frame.top
                    val cardTop = imageSize * (frame.signTop - frame.top - 0.006f)
                    val visibleHeight = imageSize * (frame.pawsBottom - frame.top)
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = if (hasAvatar) cardTop else 0.dp),
                        shape = RoundedCornerShape(32.dp), color = frame.signColor,
                        border = BorderStroke(3.dp, Color.Black),
                        shadowElevation = 1.dp,
                    ) {
                        Text(stringResource(R.string.home_encouragement),
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp,
                                top = if (hasAvatar) imageSize * (frame.pawsBottom - frame.signTop) + 12.dp else 26.dp,
                                bottom = 24.dp))
                    }
                    if (hasAvatar) Box(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth().height(visibleHeight).clipToBounds(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        AvatarImage(avatarKey, Modifier.wrapContentSize(Alignment.TopCenter, unbounded = true)
                            .requiredSize(imageSize).offset(y = -trimTop)
                            .drawWithContent {
                                // Klipp bort skiltet i bildet. Én ramme tegner nå hele boksen,
                                // så en håndtegnet sidekant ikke må skjøtes mot en ny kant.
                                val silhouette = Path().apply {
                                    addRect(Rect(0f, 0f, size.width, size.height * (frame.signTop - 0.006f)))
                                    frame.paws.forEach { (left, right) ->
                                        addOval(Rect(size.width * left,
                                            size.height * (frame.signTop - 0.08f),
                                            size.width * right, size.height * frame.pawsBottom))
                                    }
                                }
                                clipPath(silhouette) { this@drawWithContent.drawContent() }
                            }
                            .testTag("home_avatar"))
                    }
                }
            }
            item {
                HomeCard {
                    Text(stringResource(R.string.home_greeting),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    // Værinnholdet beholder koblingen til API, stedsvalg og oppdatering.
                    weatherContent()
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.home_today), color = Moss,
                        style= MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))

                }

            }
            item {
                HomeCard(modifier = Modifier.clickable { onOpenSteps() }.testTag("home_open_steps")) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconTile(HomeIcons.Steps, Color(0xFFE4EBCB), Moss)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.goals_steps), fontWeight = FontWeight.Bold)
                            Text(
                                stringResource(R.string.steps_count_format, numbers.format(stepsState.stepsToday)),
                                color = Moss,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.testTag("home_steps_total"),
                            )
                            val progress = stepsState.progress
                            val goal = stepsState.stepGoal
                            if (goal != null && progress != null) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape),
                                    color = Moss,
                                    trackColor = Color(0xFFE9E4D3),
                                )
                                Text(
                                    stringResource(R.string.steps_goal_summary, numbers.format(goal)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFE9E4D3), CircleShape))
                                Text(
                                    stringResource(R.string.steps_no_goal),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Button(
                        onClick = onOpenSteps,
                        modifier = Modifier.fillMaxWidth().testTag("home_open_steps_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Moss),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(stringResource(R.string.steps_widget_today_summary, numbers.format(stepsState.stepsToday)))
                    }
                }
            }
            item {
                HomeCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconTile(HomeIcons.Water, Color(0xFFDCECF4), WaterBlue)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.water_today), fontWeight = FontWeight.Bold)
                            when {
                                waterState.isLoading -> CircularProgressIndicator(Modifier.size(24.dp))
                                waterState.loadFailed -> {
                                    Text(stringResource(R.string.water_load_error), color = MaterialTheme.colorScheme.error)
                                    TextButton(onClick = onRetryWater, modifier = Modifier.testTag("home_water_retry")) {
                                        Text(stringResource(R.string.water_retry))
                                    }
                                }
                                else -> Text(stringResource(R.string.water_amount, numbers.format(waterState.totalTodayMl)),
                                    color = WaterBlue, fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("home_water_total"))
                            }
                        }
                    }
                    WaterGoalSummary(waterState, onRetryWater, tagPrefix = "home")
                    // Knappen går fortsatt til selve registreringen. Forsiden lagrer ingenting selv.
                    Button(onClick = onOpenWater, modifier = Modifier.fillMaxWidth().testTag("home_open_water"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAA5B12)),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(14.dp)) {
                        Text(stringResource(R.string.home_register_water))
                    }
                    TextButton(onClick = onOpenGoals, modifier = Modifier.fillMaxWidth().testTag("home_open_goals")) {
                        Text(stringResource(R.string.home_open_goals))
                    }
                }
            }
            item {
                HomeTasksCard(tasksState, onOpenTasks, onAddTask, onRetryTasks)
            }
            item {
                // De små kortene speiler mockupen. Ingen streak eller merker beregnes ennå.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(HomeIcons.Flame, R.string.home_streak, Color(0xFFB96523), Modifier.weight(1f))
                    SummaryCard(HomeIcons.Trophy, R.string.home_achievements, Color(0xFFAA7C20), Modifier.weight(1f))
                }
            }
        }
    }
}

// Samme kort, ikonfelt og knappestil som de andre tellerne. Tallene kommer fra gjøremålslisten.
@Composable
private fun HomeTasksCard(state: TasksUiState, onOpen: () -> Unit, onAdd: () -> Unit, onRetry: () -> Unit) {
    HomeCard(modifier = Modifier.testTag("home_tasks_card")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(HomeIcons.Tasks, Color(0xFFE4EBCB), Moss)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.home_tasks_today), fontWeight = FontWeight.Bold)
                when {
                    state.isLoading -> CircularProgressIndicator(Modifier.size(24.dp))
                    state.loadFailed -> {
                        Text(stringResource(R.string.tasks_load_error), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry, modifier = Modifier.testTag("home_tasks_retry")) {
                            Text(stringResource(R.string.water_retry))
                        }
                    }
                    state.tasks.isEmpty() -> Text(stringResource(R.string.home_tasks_empty),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("home_tasks_empty"))
                    else -> {
                        Text(stringResource(R.string.home_tasks_completed, state.completedCount, state.tasks.size),
                            color = Moss, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("home_tasks_total"))
                        LinearProgressIndicator(progress = { state.progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).testTag("home_tasks_progress"),
                            color = Moss, trackColor = Color(0xFFE9E4D3))
                        if (state.completedCount == state.tasks.size) Text(stringResource(R.string.home_tasks_all_done),
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("home_tasks_all_done"))
                    }
                }
            }
        }
        val empty = !state.isLoading && !state.loadFailed && state.tasks.isEmpty()
        Button(onClick = if (empty) onAdd else onOpen,
            modifier = Modifier.fillMaxWidth().testTag("home_open_tasks"),
            colors = ButtonDefaults.buttonColors(containerColor = Moss),
            shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(14.dp)) {
            Text(stringResource(if (empty) R.string.tasks_add else R.string.home_tasks_open))
        }
    }
}

@Composable
private fun HomeCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // Samme avrunding og luft i alle kort, så vi slipper å gjenta oppsettet hver gang.
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Cream),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

@Composable
private fun IconTile(icon: ImageVector, background: Color, tint: Color) {
    Box(Modifier.size(56.dp).background(background, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
        // Teksten forklarer kortet, så skjermleseren trenger ikke lese ikonet i tillegg.
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun SummaryCard(icon: ImageVector, title: Int, tint: Color, modifier: Modifier) {
    HomeCard(modifier) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        Text(stringResource(title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.home_coming_later), style = MaterialTheme.typography.bodySmall)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 900)
@Composable
private fun HomePreview() {
    HomeShellTheme { HomeScreen(waterState = WaterUiState(isLoading = false, isGoalLoading = false)) }
}

// Andeler av originalbildet: skiltets bredde/overkant og utsnittet med hele hodet og potene.
private data class AvatarFrame(
    val signWidth: Float, val signTop: Float, val pawsBottom: Float, val top: Float = 0.15f,
    // Kremtonen er hentet fra skiltet i den tilhørende bildefilen.
    val signColor: Color = Color(0xFFFCF3E1),
    val paws: List<Pair<Float, Float>> = listOf(0.235f to 0.385f, 0.605f to 0.755f),
)

private fun avatarFrame(key: String): AvatarFrame = when (key) {
    "kanin" -> AvatarFrame(0.705f, 0.700f, 0.744f, top = 0.075f, signColor = Color(0xFFFCF3DE),
        paws = listOf(0.25f to 0.385f, 0.60f to 0.735f))
    "hund" -> AvatarFrame(0.825f, 0.650f, 0.700f)
    "bjørn" -> AvatarFrame(0.825f, 0.670f, 0.720f, signColor = Color(0xFFFCF3E3),
        paws = listOf(0.20f to 0.36f, 0.635f to 0.795f))
    "frosk" -> AvatarFrame(0.845f, 0.628f, 0.675f, signColor = Color(0xFFFDF3E2),
        paws = listOf(0.19f to 0.35f, 0.645f to 0.805f))
    else -> AvatarFrame(0.745f, 0.645f, 0.688f, signColor = Color(0xFFFDF2E0))
}
