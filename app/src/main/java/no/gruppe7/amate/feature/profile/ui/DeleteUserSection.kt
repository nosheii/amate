package no.gruppe7.amate.feature.profile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import no.gruppe7.amate.R

// Viser "Slett bruker" i innstillinger
@Composable
fun DeleteUserSection(onDelete: suspend () -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier.fillMaxWidth().testTag("delete_user_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        TextButton(
            // veksler mellom åpen og lukket.
            onClick = { expanded = !expanded; failed = false },
            enabled = !deleting,
            modifier = Modifier.testTag("delete_user_cancel"),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.profile_delete_title), modifier = Modifier.weight(1f))
                Text(if (expanded) "-" else "+")
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.profile_delete_warning), style = MaterialTheme.typography.bodySmall)
                if (failed) {
                    Text(stringResource(R.string.profile_delete_error), color = MaterialTheme.colorScheme.error)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(
                        onClick = { expanded = !expanded; failed = false },
                        enabled = !deleting,
                        modifier = Modifier.testTag("delete_user_cancel"),
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Button(
                        onClick = {
                            deleting = true
                            failed = false
                            scope.launch {
                                try {
                                    onDelete()
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (_: Exception) {
                                    failed = true
                                } finally {
                                    deleting = false
                                }
                            }
                        },
                        enabled = !deleting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("delete_user_confirm"),
                    ) {
                        Text(stringResource(if (deleting) R.string.profile_delete_deleting else R.string.profile_delete_confirm))
                    }
                }
            }
        }
    }
}