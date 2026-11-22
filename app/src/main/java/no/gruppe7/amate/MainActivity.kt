package no.gruppe7.amate

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.gruppe7.amate.core.navigation.AppDestination
import no.gruppe7.amate.core.navigation.AmateShell
import no.gruppe7.amate.feature.goals.ui.GoalsRoute
import no.gruppe7.amate.feature.steps.ui.StepReminderSettingsRoute
import no.gruppe7.amate.feature.profile.ui.DeleteUserSection
import no.gruppe7.amate.feature.steps.ui.StepsRoute
import no.gruppe7.amate.feature.steps.ui.rememberStepsViewModel
import no.gruppe7.amate.feature.water.ui.WaterRoute
import no.gruppe7.amate.feature.profile.ui.ProfileViewModel
import no.gruppe7.amate.feature.profile.ui.ProfileRoute
import no.gruppe7.amate.feature.tasks.ui.rememberTasksViewModel
import no.gruppe7.amate.feature.tasks.ui.TasksRoute
import no.gruppe7.amate.feature.water.ui.rememberWaterViewModel
import no.gruppe7.amate.feature.reminders.ui.ReminderSettingsRoute
import no.gruppe7.amate.ui.theme.AmateTheme
import no.gruppe7.amate.feature.weather.ui.WeatherRoute
import no.gruppe7.amate.feature.progress.ui.ProgressRoute
import no.gruppe7.amate.feature.onboarding.ui.OnboardingGate

class MainActivity : ComponentActivity() {
    // Et varsel kan komme både før skjermen er laget og mens appen allerede er åpen.
    // Compose følger denne verdien, så vi trenger ikke starte en ekstra kopi av appen.
    private var navigationRequest by mutableStateOf<AppDestination?>(null)
    private var handledWaterRequestId: String? = null
    private var handledStepsRequestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Et gammelt varsel skal ikke åpnes på nytt ved rotasjon. Et NYTT varsel må likevel
        // virke hvis Android gjenoppretter en app som tidligere ble lukket i bakgrunnen.
        handledWaterRequestId = savedInstanceState?.getString(HANDLED_WATER_REQUEST)
        handledStepsRequestId = savedInstanceState?.getString(HANDLED_STEPS_REQUEST)
        readNavigationRequest(intent)
        setContent {
            AmateTheme {
                val container = (application as AmateApplication).container
                // Oppstarten står foran hele appen. Et varseltrykk venter her til profilen er klar.
                OnboardingGate(container.onboardingRepository) {
                    // Samme modell brukes på begge sider. Den følger både vannloggen og Najmas mål.
                    val waterModel = rememberWaterViewModel(container.waterRepository, container.goalRepository)
                    val waterState by waterModel.uiState.collectAsStateWithLifecycle()
                    val stepsModel = rememberStepsViewModel(container.stepsRepository, container.goalRepository)
                    val stepsState by stepsModel.uiState.collectAsStateWithLifecycle()
                    // Samme modell gir hjemkortet og gjøremålslisten oppdateringer fra Room.
                    val tasksModel = rememberTasksViewModel(container.taskRepository)
                    val tasksState by tasksModel.uiState.collectAsStateWithLifecycle()
                    DisposableEffect(stepsModel) {
                        // Hvis profilen nullstilles og hjem forsvinner, stopper også sensorlytteren.
                        onDispose { stepsModel.onPermissionResult(false) }
                    }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        waterModel.refreshDay()
                        tasksModel.refreshDay()
                        stepsModel.refreshDay()
                        // Fanger også opp et ja/nei gitt i oppstarten eller i Androids innstillinger.
                        stepsModel.onPermissionResult(Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
                    }
                    // Oppstart og profil skriver til samme Room-rad. Hjem viser bare lagret valg.
                    val avatar by remember(container.profileRepository) {
                        container.profileRepository.observeAvatar(1L)
                    }.collectAsStateWithLifecycle(initialValue = null)
                    AmateShell(
                        avatarKey = avatar?.avatarKey.orEmpty(),
                        waterContent = { WaterRoute(container.waterRepository, container.goalRepository) },
                        stepsContent = {
                            StepsRoute(
                                container.stepsRepository,
                                container.goalRepository,
                                onOpenGoals = { navigationRequest = AppDestination.GOALS },
                                onTestNotification = { container.stepReminderNotifications.showTestNotification(stepsState.stepsToday) },
                            )
                        },
                        settingsContent = {
                            ReminderSettingsRoute(container.reminderRepository)
                            StepReminderSettingsRoute(container.stepReminderRepository, stepsState.stepsToday)
                            DeleteUserSection(onDelete = { container.deleteUserRepository.deleteEverything(1L) })
                        },
                        profileContent = {
                            val viewModel: ProfileViewModel = viewModel(
                                factory = ProfileViewModel.factory(container.profileRepository)
                            )
                            ProfileRoute(viewModel = viewModel)
                        },
                        // Vær kobles bare inn på hjemskjermen. Vann og varsler fungerer som før.
                        weatherContent = { WeatherRoute(container.weatherRepository, container.weatherLocation) },
                        goalsContent = { GoalsRoute(container.goalRepository) },
                        // Fremgang bruker de eksisterende kildene til vann, skritt og mål, ikke en egen database.
                        progressContent = { openGoals, openWater, openSteps ->
                            ProgressRoute(container.waterRepository, container.goalRepository, openGoals, openWater,
                                stepsRepository = container.stepsRepository, onOpenSteps = openSteps)
                        },
                        tasksContent = { TasksRoute(container.taskRepository, model = tasksModel) },
                        tasksState = tasksState,
                        onRetryTasks = tasksModel::retry,
                        onPrepareNewTask = { tasksModel.openEditor() },
                        waterState = waterState,
                        stepsState = stepsState,
                        onRetryWater = waterModel::retry,
                        navigationRequest = navigationRequest,
                        onNavigationHandled = {
                            handledWaterRequestId = waterRequestId(intent)
                            handledStepsRequestId = stepsRequestId(intent)
                            navigationRequest = null
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // SINGLE_TOP i varselet gjør at dette kalles når Activity-en allerede finnes.
        readNavigationRequest(intent, newDelivery = true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(HANDLED_WATER_REQUEST, handledWaterRequestId)
        outState.putString(HANDLED_STEPS_REQUEST, handledStepsRequestId)
    }

    private fun readNavigationRequest(intent: Intent, newDelivery: Boolean = false) {
        if (intent.action == ACTION_OPEN_WATER && (newDelivery || waterRequestId(intent) != handledWaterRequestId)) {
            navigationRequest = AppDestination.WATER
        } else if (intent.action == ACTION_OPEN_STEPS && (newDelivery || stepsRequestId(intent) != handledStepsRequestId)) {
            navigationRequest = AppDestination.STEPS
        }
    }

    private fun waterRequestId(intent: Intent) = intent.getStringExtra(EXTRA_WATER_REQUEST_ID) ?: "water-without-id"
    private fun stepsRequestId(intent: Intent) = intent.getStringExtra(EXTRA_STEPS_REQUEST_ID) ?: "steps-without-id"

    companion object {
        // Delt navn mellom varselet og mottakeren. Vi sender ikke brukerdata i Intent-en.
        const val ACTION_OPEN_WATER = "no.gruppe7.amate.action.OPEN_WATER"
        const val EXTRA_WATER_REQUEST_ID = "water_request_id"
        private const val HANDLED_WATER_REQUEST = "handled_water_request_id"

        const val ACTION_OPEN_STEPS = "no.gruppe7.amate.action.OPEN_STEPS"
        const val EXTRA_STEPS_REQUEST_ID = "steps_request_id"
        private const val HANDLED_STEPS_REQUEST = "handled_steps_request_id"
    }
}
