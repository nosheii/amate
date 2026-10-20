package no.gruppe7.amate.feature.reminders.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.reminders.data.ReminderRepository
import no.gruppe7.amate.feature.reminders.scheduling.WaterReminderNotifications
import no.gruppe7.amate.feature.reminders.scheduling.WaterReminderNotifications.Companion.notificationsAllowed

// Denne delen håndterer Android-tillatelser. Selve skjermen nedenfor viser bare data.
// @Composable betyr at funksjonen beskriver UI som Compose kan tegne på nytt når state endres.
// Route kobler ting sammen; Screen lenger nede inneholder selve knappene og feltene.
@Composable
fun ReminderSettingsRoute(repository: ReminderRepository) {
    // remember beholder oppskriften mellom opptegninger så lenge repository-et er det samme.
    // viewModel() finner eller oppretter ViewModel-en; den lages ikke på nytt ved hvert tastetrykk.
    val factory = remember(repository) { RemindersViewModel.factory(repository) }
    val model: RemindersViewModel = viewModel(factory = factory)
    // Følger uiState når skjermen er aktiv. Nye verdier gjør at Compose oppdaterer visningen.
    val state by model.uiState.collectAsStateWithLifecycle()
    // Context gir tilgang til Android, f.eks. tillatelser, varsler og åpning av systeminnstillinger.
    val context = LocalContext.current
    val notifications = remember(context) { WaterReminderNotifications(context) }
    // Dette er bare visningsvalg, ikke Room-data. rememberSaveable kan bevare dem ved rotasjon.
    // expanded styrer utfoldingen. testMessageResId velger teksten etter en varseltest.
    var expanded by rememberSaveable { mutableStateOf(false) }
    var testMessageResId by rememberSaveable { mutableStateOf<Int?>(null) }
    // Samme tillatelsesdialog brukes av bryteren og testknappen. Vi må huske hvem som ba om den.
    var waitingForTestPermission by rememberSaveable { mutableStateOf(false) }

    fun sendTestNotification() {
        // true betyr at varselet ble sendt til Android. Lyd/banner kan fortsatt være dempet.
        // Vi viser tilbakemeldingen her, uten å lagre innstillinger eller bruke dags-telleren.
        testMessageResId = if (notifications.showTestNotification()) {
            R.string.reminders_test_sent
        } else {
            R.string.reminders_test_blocked
        }
        model.updatePermission(notifications.isAllowed())
    }

    // Lager en trygg kobling til Androids dialog. Blokken under kjøres når brukeren har svart.
    // Vi leser faktisk tillatelse på nytt, også fordi selve varselkanalen kan være blokkert.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (waitingForTestPermission) {
            waitingForTestPermission = false
            // Tillatelse til en test skal ikke slå på eller lagre daglige påminnelser.
            sendTestNotification()
        } else {
            // Kom forespørselen fra bryteren, kan svaret endre bryteren i utkastet.
            // Brukeren må fremdeles trykke Lagre for å endre daglige påminnelser.
            model.permissionResult(notificationsAllowed(context))
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        // Brukeren kan ha endret tillatelsen i telefonens innstillinger.
        model.updatePermission(notificationsAllowed(context))
    }

    // on... er handlinger vi sender inn til Screen. Screen sier hva som ble trykket,
    // mens Route/ViewModel bestemmer hva vi gjør med det.
    ReminderSettingsScreen(
        state = state,
        expanded = expanded,
        onToggleExpanded = { expanded = !expanded },
        onEnabledChange = { enabled ->
            // Av krever ingen tillatelse. På kan kreve Androids dialog, eller hjelp via innstillingslenken.
            when {
                !enabled -> model.changeEnabled(false)
                notificationsAllowed(context) -> {
                    model.updatePermission(true)
                    model.changeEnabled(true)
                }
                Build.VERSION.SDK_INT >= 33 -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                // Eldre Android har ikke denne tillatelsesdialogen. Da vises lenken til innstillinger.
                else -> model.updatePermission(false)
            }
        },
        // :: sender selve funksjonen videre, uten å kjøre den nå. Den kjøres ved brukerens handling.
        onCountChange = model::changeCount,
        onStartChange = model::changeStart,
        onEndChange = model::changeEnd,
        onSave = model::save,
        // Avbryt gjør to ting: henter lagrede valg tilbake og lukker utfoldingen.
        onCancel = { model.discardChanges(); expanded = false },
        onRetry = model::retry,
        testMessageResId = testMessageResId,
        onTestNotification = {
            // Fra Android 13 (API 33) må vi spørre om varslingstillatelse ved behov.
            // Har appen tillatelsen, men kanalen er blokkert, prøver vi ikke en ny dialog;
            // sendTestNotification viser da en forklaring og skjermen har en innstillingslenke.
            val needsPermission = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                waitingForTestPermission = true
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                sendTestNotification()
            }
        },
        onOpenSystemSettings = {
            // Dette åpner Androids varselinnstillinger for akkurat Amate, ikke vår egen editor.
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            })
        },
    )
}

// Ren visning: ingen databasekall her. Dette gjør også at knappene kan testes med testdata.
@Composable
fun ReminderSettingsScreen(
    state: RemindersUiState,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onCountChange: (Int) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    onTestNotification: () -> Unit,
    testMessageResId: Int?,
) {
    // Låser redigeringen mens data hentes/lagres, eller dersom innlasting feilet.
    // Testknappen er uavhengig: den trenger ikke gyldige innstillinger i Room.
    val controlsEnabled = !state.isLoading && !state.isSaving && !state.loadFailed
    // Disse tekstene brukes også av skjermleser, så utfolding og bryter blir forståelige uten å se.
    val expansionDescription = stringResource(if (expanded) R.string.reminders_expanded else R.string.reminders_collapsed)
    val switchDescription = stringResource(R.string.reminders_enable)

    // Card er boksen rundt alt. Modifier bestemmer størrelse, luft og testnavn.
    // testTag er et navn UI-testene kan finne; det er ikke synlig tekst i appen.
    Card(
        modifier = Modifier.fillMaxWidth().testTag("reminder_settings_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        TextButton(
            // Endrer bare om boksen er åpen. Det lagrer eller avbryter ikke utkastet.
            onClick = onToggleExpanded, enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("reminder_settings_toggle")
                .semantics { stateDescription = expansionDescription },
        ) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reminders_title), modifier = Modifier.weight(1f))
                Text(if (expanded) "−" else "+")
            }
        }
        // Samme type utfolding som vannfeltene. Dette åpner ingen ny navigasjonsrute.
        // visible styrer om feltene vises. enter/exit gir utvidelse og myk inn-/utfading.
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)
                    .testTag("reminder_settings_fields"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.reminders_delivery_note), style = MaterialTheme.typography.bodySmall)
                // Denne knappen tester bare varselvisningen, ikke bakgrunnsplanen.
                OutlinedButton(onClick = onTestNotification,
                    modifier = Modifier.fillMaxWidth().testTag("reminder_test_notification")) {
                    Text(stringResource(R.string.reminders_test_button))
                }
                Text(stringResource(R.string.reminders_test_hint), style = MaterialTheme.typography.bodySmall)
                // ?.let viser tekst bare når vi har en melding etter et testtrykk.
                // liveRegion lar skjermleser lese opp en ny melding uten at fokus må flyttes.
                testMessageResId?.let {
                    Text(stringResource(it), modifier = Modifier.testTag("reminder_test_result")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodySmall)
                }
                // Spinnhjulet vises mens vi venter på første svar fra Room.
                if (state.isLoading) CircularProgressIndicator()
                if (!state.loadFailed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(switchDescription, modifier = Modifier.weight(1f))
                        Switch(
                            // checked viser utkastet. onCheckedChange sender ønsket endring tilbake.
                            // Bryteren bestemmer ikke om Android faktisk har gitt varslingstillatelse.
                            checked = state.enabled, onCheckedChange = onEnabledChange,
                            enabled = controlsEnabled,
                            modifier = Modifier.testTag("reminder_enabled")
                                .semantics { contentDescription = switchDescription },
                        )
                    }
                    if (!state.notificationsAllowed) {
                        // Et lagret ønske om varsler hjelper ikke hvis telefonen blokkerer dem.
                        Text(stringResource(R.string.reminders_permission_blocked), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onOpenSystemSettings) {
                            Text(stringResource(R.string.reminders_open_system_settings))
                        }
                    }
                    Text(stringResource(R.string.reminders_count_label), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Lager de fem valgknappene fra samme oppskrift, med ett tall per knapp.
                        (1..5).forEach { count ->
                            FilterChip(
                                // Bare tallet som er lik utkastets antall markeres som valgt.
                                selected = state.remindersPerDay == count,
                                onClick = { onCountChange(count) },
                                enabled = controlsEnabled,
                                label = { Text(count.toString()) },
                                modifier = Modifier.weight(1f).testTag("reminder_count_$count"),
                            )
                        }
                    }
                    Text(stringResource(R.string.reminders_time_label), style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Feltene beholder teksten slik den skrives. Gyldig klokkeslett sjekkes ved Lagre.
                        // weight(1f) på begge gjør at de deler plassen likt i raden.
                        OutlinedTextField(
                            value = state.startTime, onValueChange = onStartChange,
                            label = { Text(stringResource(R.string.reminders_start)) },
                            singleLine = true, enabled = controlsEnabled,
                            modifier = Modifier.weight(1f).testTag("reminder_start"),
                        )
                        OutlinedTextField(
                            value = state.endTime, onValueChange = onEndChange,
                            label = { Text(stringResource(R.string.reminders_end)) },
                            singleLine = true, enabled = controlsEnabled,
                            modifier = Modifier.weight(1f).testTag("reminder_end"),
                        )
                    }
                    Text(stringResource(R.string.reminders_time_hint), style = MaterialTheme.typography.bodySmall)
                }
                // Feil og lagringsbekreftelse kommer fra ViewModel; teksten hentes fra strings.xml.
                state.errorMessageResId?.let {
                    Text(stringResource(it), color = MaterialTheme.colorScheme.error)
                }
                if (state.loadFailed) {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.water_retry)) }
                }
                if (state.saved) {
                    Text(stringResource(R.string.reminders_saved), modifier = Modifier.testTag("reminder_saved"))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onCancel, enabled = !state.isSaving) {
                        Text(stringResource(R.string.water_cancel))
                    }
                    // Ingen ulagrede endringer = ingen grunn til å lagre på nytt.
                    Button(onClick = onSave, enabled = controlsEnabled && state.hasChanges,
                        modifier = Modifier.testTag("reminder_save")) {
                        Text(stringResource(if (state.isSaving) R.string.water_saving else R.string.reminders_save))
                    }
                }
            }
        }
        // Å lukke boksen med overskriften er ikke det samme som Avbryt.
        // Utkastet beholdes, og denne teksten minner om at det ikke er lagret ennå.
        if (!expanded && state.hasChanges) {
            Text(stringResource(R.string.reminders_unsaved), modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
