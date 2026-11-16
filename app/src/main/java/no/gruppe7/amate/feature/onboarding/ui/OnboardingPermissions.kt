package no.gruppe7.amate.feature.onboarding.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import no.gruppe7.amate.R

internal enum class SetupPermission { STEPS, NOTIFICATIONS, LOCATION }
internal enum class SetupPermissionStatus { NOT_ASKED, GRANTED, DENIED, SETTINGS, NO_SENSOR }

// Android-tillatelser er ikke innstillinger vi kan lagre som «ja» i Room.
// Vi sjekker det ekte svaret fra Android, også etter et besøk i telefonens innstillinger.
@Composable
internal fun OnboardingPermissions(enabled: Boolean) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val preferences = remember { context.getSharedPreferences("onboarding_permissions", Context.MODE_PRIVATE) }
    val weatherPreferences = remember { context.getSharedPreferences("weather_permission", Context.MODE_PRIVATE) }
    var pending by rememberSaveable { mutableStateOf<SetupPermission?>(null) }
    var statuses by remember { mutableStateOf<Map<SetupPermission, SetupPermissionStatus>>(emptyMap()) }

    fun refresh() {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val hasSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null ||
            sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        statuses = SetupPermission.entries.associateWith { kind ->
            val permission = permissionFor(kind)
            val granted = (permission == null || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) &&
                (kind != SetupPermission.NOTIFICATIONS || NotificationManagerCompat.from(context).areNotificationsEnabled())
            val asked = preferences.getBoolean(kind.name, false) ||
                (kind == SetupPermission.LOCATION && weatherPreferences.getBoolean("denied", false))
            when {
                kind == SetupPermission.STEPS && !hasSensor -> SetupPermissionStatus.NO_SENSOR
                granted -> SetupPermissionStatus.GRANTED
                // Når runtime-tilgang allerede er gitt, må f.eks. avskrudd varsling endres i Innstillinger.
                permission == null || ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> SetupPermissionStatus.SETTINGS
                !asked -> SetupPermissionStatus.NOT_ASKED
                activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) } == true -> SetupPermissionStatus.DENIED
                else -> SetupPermissionStatus.SETTINGS
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (pending == SetupPermission.LOCATION) {
            // Værkortet skal kjenne igjen et nei fra oppstarten også. Selve posisjonen lagres ikke.
            weatherPreferences.edit { putBoolean("denied", !granted) }
        }
        pending = null
        refresh()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (pending == null) refresh() }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SetupPermission.entries.forEach { kind ->
            val status = statuses[kind] ?: SetupPermissionStatus.NOT_ASKED
            OnboardingCard {
                Text(stringResource(when (kind) {
                    SetupPermission.STEPS -> R.string.onboarding_permission_steps
                    SetupPermission.NOTIFICATIONS -> R.string.onboarding_permission_notifications
                    SetupPermission.LOCATION -> R.string.onboarding_permission_location
                }), fontWeight = FontWeight.Bold)
                Text(stringResource(when (kind) {
                    SetupPermission.STEPS -> R.string.onboarding_permission_steps_body
                    SetupPermission.NOTIFICATIONS -> R.string.onboarding_permission_notifications_body
                    SetupPermission.LOCATION -> R.string.onboarding_permission_location_body
                }), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(when (status) {
                    SetupPermissionStatus.GRANTED -> R.string.onboarding_permission_granted
                    SetupPermissionStatus.NO_SENSOR -> R.string.onboarding_permission_no_sensor
                    SetupPermissionStatus.DENIED -> R.string.onboarding_permission_denied
                    SetupPermissionStatus.SETTINGS -> R.string.onboarding_permission_settings_body
                    SetupPermissionStatus.NOT_ASKED -> R.string.onboarding_permission_optional
                }), style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("onboarding_permission_status_${kind.name}"))
                if (status != SetupPermissionStatus.GRANTED && status != SetupPermissionStatus.NO_SENSOR) {
                    OutlinedButton(enabled = enabled && pending == null,
                        modifier = Modifier.testTag("onboarding_permission_${kind.name}"), onClick = {
                            if (status == SetupPermissionStatus.SETTINGS) {
                                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    "package:${context.packageName}".toUri()))
                            } else {
                                permissionFor(kind)?.let { permission ->
                                    preferences.edit { putBoolean(kind.name, true) }
                                    pending = kind
                                    launcher.launch(permission)
                                }
                            }
                        }) {
                        Text(stringResource(when (status) {
                            SetupPermissionStatus.SETTINGS -> R.string.onboarding_permission_open_settings
                            SetupPermissionStatus.DENIED -> R.string.common_retry
                            else -> R.string.onboarding_permission_allow
                        }))
                    }
                }
            }
        }
    }
}

private fun permissionFor(kind: SetupPermission): String? = when (kind) {
    SetupPermission.STEPS -> if (Build.VERSION.SDK_INT >= 29) Manifest.permission.ACTIVITY_RECOGNITION else null
    SetupPermission.NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
    SetupPermission.LOCATION -> Manifest.permission.ACCESS_COARSE_LOCATION
}
