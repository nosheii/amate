package no.gruppe7.amate.feature.onboarding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.home.ui.HomeShellTheme
import no.gruppe7.amate.feature.onboarding.data.OnboardingRepository
import no.gruppe7.amate.feature.profile.ui.AvatarPicker

@Composable
fun OnboardingGate(repository: OnboardingRepository, content: @Composable () -> Unit) {
    val factory = remember(repository) { OnboardingViewModel.factory(repository) }
    val model: OnboardingViewModel = viewModel(factory = factory)
    val state by model.uiState.collectAsStateWithLifecycle()
    when (state.entry) {
        // Først her opprettes de vanlige skjermmodellene. Ingen værforespørsel/sensor startes av skjemaet.
        OnboardingEntry.READY -> content()
        else -> HomeShellTheme {
            OnboardingScreen(state, model::next, model::back, model::skipGoals, model::retry,
                model::changeName, model::selectAvatar, model::changeWater, model::changeSteps,
                permissionContent = { OnboardingPermissions(enabled = !state.isSaving) })
        }
    }
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkipGoals: () -> Unit,
    onRetry: () -> Unit,
    onName: (String) -> Unit,
    onAvatar: (String) -> Unit,
    onWater: (String) -> Unit,
    onSteps: (String) -> Unit,
    modifier: Modifier = Modifier,
    permissionContent: @Composable () -> Unit = {},
) {
    val focus = LocalFocusManager.current
    // Androids tilbakeknapp skal gå til forrige steg, ikke lukke hele appen midt i skjemaet.
    BackHandler(state.entry == OnboardingEntry.SETUP && (state.isSaving || state.step != OnboardingStep.PROFILE)) {
        if (!state.isSaving) onBack()
    }
    Box(modifier.fillMaxSize().background(Brush.verticalGradient(
        listOf(Color(0xFFD9EDF7), Color(0xFFF5F4DC), Color(0xFFE3EACF))))
        .safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
        when (state.entry) {
            OnboardingEntry.LOADING -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(Modifier.testTag("onboarding_loading"))
                Text(stringResource(R.string.onboarding_loading))
            }
            OnboardingEntry.ERROR -> Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                Text(stringResource(R.string.onboarding_load_error))
                Button(onClick = onRetry, modifier = Modifier.testTag("onboarding_retry")) { Text(stringResource(R.string.common_retry)) }
            }
            OnboardingEntry.SETUP -> key(state.step) {
                // Hvert steg starter øverst. Hele siden kan skrolles når tastatur/stor tekst tar plass.
                LazyColumn(Modifier.widthIn(max = 600.dp).fillMaxSize().testTag("onboarding_list"),
                    contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("AMATE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.onboarding_step, state.step.ordinal + 1), style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(progress = { (state.step.ordinal + 1) / 3f }, modifier = Modifier.fillMaxWidth())
                            Text(stringResource(when (state.step) {
                                OnboardingStep.PROFILE -> R.string.onboarding_welcome
                                OnboardingStep.GOALS -> R.string.onboarding_goals_title
                                OnboardingStep.PERMISSIONS -> R.string.onboarding_permissions_title
                            }), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(when (state.step) {
                                OnboardingStep.PROFILE -> R.string.onboarding_welcome_body
                                OnboardingStep.GOALS -> R.string.onboarding_goals_body
                                OnboardingStep.PERMISSIONS -> R.string.onboarding_permissions_body
                            }))
                        }
                    }
                    when (state.step) {
                        OnboardingStep.PROFILE -> {
                            item {
                                OnboardingCard {
                                    OutlinedTextField(state.name, onName, enabled = !state.isSaving, singleLine = true,
                                        label = { Text(stringResource(R.string.profile_name)) }, isError = state.nameError,
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                                        modifier = Modifier.fillMaxWidth().testTag("onboarding_name"))
                                    if (state.nameError) ErrorText(R.string.onboarding_name_error, "onboarding_name_error")
                                }
                            }
                            item {
                                OnboardingCard {
                                    Text(stringResource(R.string.profile_choose_avatar), fontWeight = FontWeight.Bold)
                                    AvatarPicker(state.avatarKey, { focus.clearFocus(); onAvatar(it) }, enabled = !state.isSaving)
                                    if (state.avatarError) ErrorText(R.string.onboarding_avatar_error, "onboarding_avatar_error")
                                }
                            }
                        }
                        OnboardingStep.GOALS -> item {
                            OnboardingCard {
                                GoalInput(state.waterInput, onWater, R.string.onboarding_water_goal, state.waterError,
                                    R.string.onboarding_water_error, "onboarding_water_goal")
                                GoalInput(state.stepsInput, onSteps, R.string.onboarding_steps_goal, state.stepsError,
                                    R.string.onboarding_steps_error, "onboarding_steps_goal")
                            }
                        }
                        OnboardingStep.PERMISSIONS -> item { permissionContent() }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.saveFailed) ErrorText(R.string.onboarding_save_error, "onboarding_save_error")
                            Button(onClick = { focus.clearFocus(); onNext() }, enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_next")) {
                                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                else Text(stringResource(if (state.step == OnboardingStep.PERMISSIONS) R.string.onboarding_finish else R.string.onboarding_next))
                            }
                            if (state.step == OnboardingStep.GOALS) TextButton(onClick = onSkipGoals, enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_skip_goals")) { Text(stringResource(R.string.onboarding_skip_goals)) }
                            if (state.step != OnboardingStep.PROFILE) TextButton(onClick = onBack, enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_back")) { Text(stringResource(R.string.onboarding_back)) }
                        }
                    }
                }
            }
            OnboardingEntry.READY -> Unit
        }
    }
}

@Composable
internal fun OnboardingCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun GoalInput(value: String, onChange: (String) -> Unit, label: Int, error: Boolean, errorRes: Int, tag: String) {
    OutlinedTextField(value, onChange, singleLine = true, isError = error,
        label = { Text(stringResource(label)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth().testTag(tag))
    if (error) ErrorText(errorRes, "${tag}_error")
}

@Composable
private fun ErrorText(message: Int, tag: String) {
    Text(stringResource(message), color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag(tag))
}
