package no.gruppe7.amate.feature.home.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny

// Her velger vi ferdige ikoner fra Material-biblioteket, i stedet for å tegne dem selv.
// Rounded gir dem samme myke stil. Farge og størrelse bestemmes fortsatt av skjermen.
// Vi beholder disse enkle navnene, så skjermene slipper å endres når vi bytter ikon.
internal object HomeIcons {
    val Home = Icons.Rounded.Home
    val Progress = Icons.Rounded.BarChart
    val Profile = Icons.Rounded.Person
    val Water = Icons.Rounded.WaterDrop
    val Steps = Icons.AutoMirrored.Rounded.DirectionsWalk
    val Sun = Icons.Rounded.WbSunny
    val Flame = Icons.Rounded.LocalFireDepartment
    val Trophy = Icons.Rounded.EmojiEvents
    val Settings = Icons.Rounded.Settings
    // Speilvendes automatisk dersom appen senere brukes med et høyre-til-venstre-språk.
    val Back = Icons.AutoMirrored.Rounded.ArrowBack
}
