package no.gruppe7.amate.feature.steps.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.goals.data.GoalRepository
import no.gruppe7.amate.feature.steps.data.StepsRepository
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Cream = Color(0xFFFFFBF1)
private val Moss = Color(0xFF617D42)
private val DarkMoss = Color(0xFF42572D)
private val SoftGreen = Color(0xFFE4EBCB)

@Composable
fun StepsRoute(
    stepsRepository: StepsRepository,
    goalRepository: GoalRepository? = null,
    onOpenGoals: () -> Unit = {},
) {
    val viewModel = rememberStepsViewModel(stepsRepository, goalRepository)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StepsScreen(
        uiState = uiState,
        onPermissionResult = viewModel::onPermissionResult,
        onAddStep = viewModel::addStep,
        onOpenGoals = onOpenGoals,
    )
}

@Composable
fun StepsScreen(
    uiState: StepsUiState,
    onPermissionResult: (Boolean) -> Unit = {},
    onAddStep: (Int) -> Unit = {},
    onOpenGoals: () -> Unit = {},
) {
    val context = LocalContext.current
    val numbers = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("nb-NO")) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> onPermissionResult(isGranted) }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
            onPermissionResult(granted)
        } else {
            onPermissionResult(true)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .testTag("steps_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(SoftGreen, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                            contentDescription = null,
                            tint = Moss,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.steps_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.steps_today_heading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Hovedkort med dagens skritt
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Cream),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.steps_count_format, numbers.format(uiState.stepsToday)),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkMoss,
                            modifier = Modifier.testTag("steps_today_count"),
                        )

                        val goal = uiState.stepGoal
                        val progress = uiState.progress
                        if (goal != null && progress != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = stringResource(R.string.steps_goal_summary, numbers.format(goal)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Moss,
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = Moss,
                                    trackColor = SoftGreen,
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = stringResource(R.string.steps_no_goal),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(
                                    onClick = onOpenGoals,
                                    modifier = Modifier.testTag("steps_open_goals"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Flag,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.size(4.dp))
                                    Text(stringResource(R.string.steps_edit_goal))
                                }
                            }
                        }
                    }
                }
            }

            // Tillatelseskort (ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !uiState.permissionGranted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = Color(0xFF856404),
                                modifier = Modifier.size(28.dp),
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(R.string.steps_permission_required),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF856404),
                                )
                                Button(
                                    onClick = { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Moss),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("steps_permission_button"),
                                ) {
                                    Text(stringResource(R.string.steps_grant_permission))
                                }
                            }
                        }
                    }
                }
            }

            // Sensor status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Cream),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = if (uiState.sensorAvailable) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                            contentDescription = null,
                            tint = if (uiState.sensorAvailable) Moss else Color(0xFFB96523),
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(
                                if (uiState.sensorAvailable) R.string.steps_sensor_available
                                else R.string.steps_sensor_unavailable
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // Test / Manuell skrittregistrering
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Cream),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.steps_manual_test_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedButton(
                                onClick = { onAddStep(100) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("steps_add_100"),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(stringResource(R.string.steps_add_100))
                            }
                            OutlinedButton(
                                onClick = { onAddStep(1000) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("steps_add_1000"),
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(stringResource(R.string.steps_add_1000))
                            }
                        }
                    }
                }
            }

            // Historikk
            item {
                Text(
                    text = stringResource(R.string.steps_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Moss,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (uiState.history.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.steps_history_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(uiState.history) { entity ->
                    val dateStr = remember(entity.epochDay) {
                        LocalDate.ofEpochDay(entity.epochDay)
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Cream),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = stringResource(R.string.steps_count_format, numbers.format(entity.stepCount)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Moss,
                            )
                        }
                    }
                }
            }
        }
    }
}
