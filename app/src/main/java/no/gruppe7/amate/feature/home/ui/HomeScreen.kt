package no.gruppe7.amate.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
) {
    // Bare visning her: ingen ViewModel, Room eller oppdiktede brukerdata.
    // Callbackene sier fra om knappetrykk. Den som viser skjermen bestemmer hvor vi går.
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
                HomeCard {
                    Text(stringResource(R.string.home_encouragement),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(6.dp))
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    // Streker i kortene betyr ikke at brukeren har null registreringer.
                    Text(stringResource(R.string.home_preview_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                HomeCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconTile(HomeIcons.Sun, Color(0xFFFFE8AC), Color(0xFFA36C15))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.home_weather), fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.home_coming_later), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.home_today), color = Moss,
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                HomeCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconTile(HomeIcons.Steps, Color(0xFFE4EBCB), Moss)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.home_steps), fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge)
                            // Tomt spor, ikke en falsk fremdriftsprosent. Skrittkoblingen kommer senere.
                            Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFE9E4D3), CircleShape))
                            Text(stringResource(R.string.home_not_connected), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            item {
                HomeCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconTile(HomeIcons.Water, Color(0xFFDCECF4), WaterBlue)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.home_water), color = WaterBlue, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.home_water_hint), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    // Bare en snarvei. Den gamle vannskjermen henter og lagrer fortsatt dataene.
                    Button(onClick = onOpenWater, modifier = Modifier.fillMaxWidth().testTag("home_open_water"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAA5B12)),
                        shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(14.dp)) {
                        Text(stringResource(R.string.home_register_water))
                    }
                }
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
    HomeShellTheme { HomeScreen() }
}
