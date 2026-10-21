package no.gruppe7.amate.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.gruppe7.amate.R
import no.gruppe7.amate.feature.home.ui.HomeIcons
import no.gruppe7.amate.feature.home.ui.HomeScreen
import no.gruppe7.amate.feature.home.ui.HomeShellTheme

@Composable
fun AmateShell(
    waterContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
) {
    // En liten, midlertidig skjermvelger, ikke gruppas ferdige navigasjonsløsning.
    // rememberSaveable beholder valgt skjerm ved for eksempel rotasjon av telefonen.
    // Innholdet sendes inn, så selve skallet ikke trenger en database eller AppContainer.
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val goHome = { destination = AppDestination.HOME }
    BackHandler(enabled = destination != AppDestination.HOME, onBack = goHome)

    HomeShellTheme {
        Scaffold(
            topBar = {
                if (destination != AppDestination.HOME) {
                    Surface {
                        Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 56.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = goHome) {
                                Icon(HomeIcons.Back, stringResource(R.string.home_back))
                            }
                            Text(stringResource(destination.title()), fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    // Bare disse tre fanene står i mockupens bunnmeny.
                    listOf(AppDestination.HOME, AppDestination.PROGRESS, AppDestination.PROFILE).forEach { tab ->
                        NavigationBarItem(
                            selected = if (tab == AppDestination.HOME) {
                                destination in listOf(AppDestination.HOME, AppDestination.WATER, AppDestination.SETTINGS)
                            } else destination == tab,
                            onClick = { destination = tab },
                            icon = { Icon(when (tab) {
                                AppDestination.PROGRESS -> HomeIcons.Progress
                                AppDestination.PROFILE -> HomeIcons.Profile
                                else -> HomeIcons.Home
                            }, contentDescription = null) },
                            label = { Text(stringResource(tab.title())) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            modifier = Modifier.testTag("home_tab_${tab.route}"),
                        )
                    }
                }
            },
        ) { padding ->
            // Scaffold holder innhold unna systemlinjene og bunnmenyen.
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (destination) {
                    AppDestination.HOME -> HomeScreen(
                        onOpenWater = { destination = AppDestination.WATER },
                        onOpenSettings = { destination = AppDestination.SETTINGS },
                    )
                    AppDestination.WATER -> waterContent()
                    AppDestination.SETTINGS -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Column(Modifier.widthIn(max = 600.dp).fillMaxSize().verticalScroll(rememberScrollState())
                            .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(stringResource(R.string.home_settings_intro))
                            // Eksisterende vannpåminnelser, uten å endre lagring eller varselkode.
                            settingsContent()
                        }
                    }
                    else -> PlaceholderPage(destination)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderPage(destination: AppDestination) {
    // Profil og fremgang er bare tomme sider. Vi bygger ikke de andres funksjoner her.
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(destination.title()), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_placeholder_body), textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp))
    }
}

private fun AppDestination.title(): Int = when (this) {
    AppDestination.PROGRESS -> R.string.home_nav_progress
    AppDestination.PROFILE -> R.string.home_nav_profile
    AppDestination.WATER -> R.string.home_water_title
    AppDestination.SETTINGS -> R.string.home_settings_title
    else -> R.string.home_nav_home
}
