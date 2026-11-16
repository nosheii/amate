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
import no.gruppe7.amate.feature.steps.ui.StepsRoute
import no.gruppe7.amate.feature.steps.ui.rememberStepsViewModel
import no.gruppe7.amate.feature.water.ui.WaterRoute
import no.gruppe7.amate.feature.profile.ui.ProfileViewModel
import no.gruppe7.amate.feature.profile.ui.ProfileRoute
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Et gammelt varsel skal ikke åpnes på nytt ved rotasjon. Et NYTT varsel må likevel
        // virke hvis Android gjenoppretter en app som tidligere ble lukket i bakgrunnen.
        handledWaterRequestId = savedInstanceState?.getString(HANDLED_WATER_REQUEST)
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
                    DisposableEffect(stepsModel) {
                        // Hvis profilen nullstilles og hjem forsvinner, stopper også sensorlytteren.
                        onDispose { stepsModel.onPermissionResult(false) }
                    }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        waterModel.refreshDay()
                        // Fanger også opp et ja/nei gitt i oppstarten eller i Androids innstillinger.
                        stepsModel.onPermissionResult(Build.VERSION.SDK_INT < 29 || ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
                    }
                    AmateShell(
                        waterContent = { WaterRoute(container.waterRepository, container.goalRepository) },
                        stepsContent = { StepsRoute(container.stepsRepository, container.goalRepository, onOpenGoals = { navigationRequest = AppDestination.GOALS }) },
                        settingsContent = { ReminderSettingsRoute(container.reminderRepository) },
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
                        tasksContent = { TasksRoute(container.taskRepository) },
                        waterState = waterState,
                        stepsState = stepsState,
                        onRetryWater = waterModel::retry,
                        navigationRequest = navigationRequest,
                        onNavigationHandled = {
                            handledWaterRequestId = waterRequestId(intent)
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
    }

    private fun readNavigationRequest(intent: Intent, newDelivery: Boolean = false) {
        if (intent.action == ACTION_OPEN_WATER && (newDelivery || waterRequestId(intent) != handledWaterRequestId)) {
            navigationRequest = AppDestination.WATER
        }
    }

    private fun waterRequestId(intent: Intent) = intent.getStringExtra(EXTRA_WATER_REQUEST_ID) ?: "water-without-id"

    companion object {
        // Delt navn mellom varselet og mottakeren. Vi sender ikke brukerdata i Intent-en.
        const val ACTION_OPEN_WATER = "no.gruppe7.amate.action.OPEN_WATER"
        const val EXTRA_WATER_REQUEST_ID = "water_request_id"
        private const val HANDLED_WATER_REQUEST = "handled_water_request_id"
    }
}
