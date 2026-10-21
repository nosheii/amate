package no.gruppe7.amate.feature.home.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// Små, lokale vektorikoner til prototypen. Ingen bildefiler, avatarer eller nye biblioteker.
// Koordinatene tegner på et 24 x 24-rutenett. Compose skalerer til størrelsen vi ber om.
internal object HomeIcons {
    val Home = icon("Home", "M3,10 L12,3 L21,10 V21 H15 V14 H9 V21 H3 Z")
    val Progress = icon("Progress", "M4,13 H8 V21 H4 Z M10,8 H14 V21 H10 Z M16,3 H20 V21 H16 Z")
    val Profile = icon("Profile", "M12,3 A4,4 0,1 1,12,11 A4,4 0,1 1,12,3 M4,21 V19 C4,12 20,12 20,19 V21 Z")
    val Water = icon("Water", "M12,2 C9,7 5,11 5,15 A7,7 0,0 0,19,15 C19,11 15,7 12,2 Z")
    val Steps = icon("Steps", "M4,4 H10 L11,10 L14,12 L21,14 V20 H3 V11 L4,4 Z")
    val Sun = icon("Sun", "M12,6 A6,6 0,1 1,12,18 A6,6 0,1 1,12,6 M11,0 H13 V4 H11 Z M11,20 H13 V24 H11 Z M0,11 H4 V13 H0 Z M20,11 H24 V13 H20 Z M3,2 L6,5 L5,6 L2,3 Z M18,19 L19,18 L22,21 L21,22 Z M18,5 L21,2 L22,3 L19,6 Z M2,21 L5,18 L6,19 L3,22 Z")
    val Flame = icon("Flame", "M13,1 C16,8 23,12 19,19 C16,25 4,23 4,15 C4,11 6,8 8,6 C7,11 10,12 10,9 C11,6 12,4 13,1 Z")
    val Trophy = icon("Trophy", "M7,3 H17 V5 H22 V9 C22,13 19,15 16,15 L13,18 V20 H17 V22 H7 V20 H11 V18 L8,15 C5,15 2,13 2,9 V5 H7 Z M4,7 V9 C4,11 5,12 7,12 V7 Z M17,7 V12 C19,12 20,11 20,9 V7 Z")
    val Settings = icon("Settings", "M9,2 H15 L16,6 L20,5 L23,10 L20,13 L21,17 L16,20 L13,18 L10,22 L5,19 L5,15 L1,13 L3,7 L7,7 Z M12,8 A4,4 0,1 0,12,16 A4,4 0,1 0,12,8 Z")
    val Back = icon("Back", "M12,4 L4,12 L12,20 L14,18 L9,13 H21 V11 H9 L14,6 Z")

    private fun icon(name: String, path: String): ImageVector = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd).build()
}
