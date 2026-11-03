package no.gruppe7.amate.feature.weather.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun CloudWeatherIcon(kind: WeatherAnimation, modifier: Modifier = Modifier, animate: Boolean = true) {
    val running = rememberWeatherMotionAllowed(animate)
    if (running) {
        val transition = rememberInfiniteTransition(label = "sky og nedbør")
        val drift = transition.animateFloat(-1f, 1f,
            animationSpec = infiniteRepeatable(tween(4_000), RepeatMode.Reverse), label = "rolig sky")
        // Bare tre små partikler brukes. Vi trenger ikke et helt system for regn på skjermen.
        val precipitation = kind in listOf(WeatherAnimation.RAIN, WeatherAnimation.SNOW, WeatherAnimation.SLEET)
        val fall = if (precipitation) transition.animateFloat(0f, 1f,
            animationSpec = infiniteRepeatable(tween(
                if (kind == WeatherAnimation.SNOW) 3_600 else 1_800, easing = LinearEasing,
            )), label = "fallende nedbør") else null
        CloudDrawing(kind, modifier.testTag("weather_${kind.tag}_animated"),
            drift = { drift.value }, fall = { fall?.value ?: 0.4f }, moving = true)
    } else {
        // Samme illustrasjon, men uten løkke: gjelder blant annet ved lasting og nettfeil.
        CloudDrawing(kind, modifier.testTag("weather_${kind.tag}_static"),
            drift = { 0f }, fall = { 0.4f }, moving = false)
    }
}

@Composable
private fun CloudDrawing(
    kind: WeatherAnimation,
    modifier: Modifier,
    drift: () -> Float,
    fall: () -> Float,
    moving: Boolean,
) {
    // Alt må holde seg inni det lille ikonfeltet, også når dråpene beveger seg nedover.
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val side = maxWidth
        val sidePx = with(LocalDensity.current) { side.toPx() }
        val precipitation = kind in listOf(WeatherAnimation.RAIN, WeatherAnimation.SNOW, WeatherAnimation.SLEET)

        if (kind == WeatherAnimation.PARTLY_CLOUDY) {
            // Vi gjenbruker sola fra første runde. Skyen tegnes foran den etterpå.
            SunnyWeatherIcon(Modifier.size(side * 0.7f).align(Alignment.TopStart), animate = moving)
        }

        if (precipitation) {
            repeat(3) { index ->
                val snow = kind == WeatherAnimation.SNOW || (kind == WeatherAnimation.SLEET && index == 1)
                Icon(if (snow) Icons.Rounded.AcUnit else Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = if (snow) Color(0xFF6E9DB5) else Color(0xFF4C89AF),
                    modifier = Modifier.size(side * if (snow) 0.18f else 0.15f)
                        .align(Alignment.TopStart)
                        .offset(x = side * (0.23f + index * 0.22f), y = side * 0.45f)
                        .graphicsLayer {
                            // Ulik start på de tre partiklene gjør at de ikke faller i én klump.
                            val progress = (fall() + index / 3f) % 1f
                            translationY = sidePx * 0.43f * progress
                            translationX = if (snow) {
                                sin((progress + index) * 2f * PI).toFloat() * sidePx * 0.04f
                            } else -sidePx * 0.06f * progress
                            rotationZ = if (snow) progress * 60f else 12f
                            // Myk inn/ut-toning skjuler hoppet når en partikkel starter på nytt.
                            alpha = if (moving) (sin(progress * PI).toFloat() * 1.4f).coerceIn(0f, 1f) else 0.85f
                        })
            }
        }

        Icon(Icons.Rounded.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(side * 0.9f)
                .align(when {
                    precipitation -> Alignment.TopCenter
                    kind == WeatherAnimation.PARTLY_CLOUDY -> Alignment.BottomEnd
                    else -> Alignment.Center
                })
                .graphicsLayer {
                    // Bare tegningen flyttes litt. Kortets størrelse og tekst ligger helt i ro.
                    translationX = drift() * sidePx * 0.025f
                    translationY = drift() * sidePx * 0.02f
                })
    }
}
