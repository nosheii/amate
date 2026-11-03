package no.gruppe7.amate.feature.weather.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import no.gruppe7.amate.R

// Små visningstyper for ikonene, ikke nye værdata eller noe vi lagrer i databasen.
internal enum class WeatherAnimation(val tag: String) {
    SUN("sun"), CLOUD("cloud"), PARTLY_CLOUDY("partly_cloudy"),
    RAIN("rain"), SNOW("snow"), SLEET("sleet"), STILL("symbol"),
}

internal fun weatherAnimationFor(symbolCode: String?): WeatherAnimation = when (conditionFor(symbolCode)) {
    R.string.weather_clear -> if (symbolCode == "clearsky_day") WeatherAnimation.SUN else WeatherAnimation.STILL
    R.string.weather_partly_cloudy -> if (symbolCode?.endsWith("_day") == true) {
        WeatherAnimation.PARTLY_CLOUDY
    } else WeatherAnimation.CLOUD // Ingen sol bak skyen om natten eller i polarskumringen.
    R.string.weather_cloudy -> WeatherAnimation.CLOUD
    R.string.weather_rain -> WeatherAnimation.RAIN
    R.string.weather_snow -> WeatherAnimation.SNOW
    R.string.weather_sleet -> WeatherAnimation.SLEET
    else -> WeatherAnimation.STILL // Tåke, torden og ukjent vær er rolige stillbilder foreløpig.
}

@Composable
internal fun WeatherIcon(symbolCode: String?, animate: Boolean, modifier: Modifier = Modifier) {
    // MET bestemmer været, ikke animasjonen. Jeg viser ikke en sol om natten eller ved ukjent vær.
    when (val kind = weatherAnimationFor(symbolCode)) {
        WeatherAnimation.SUN -> SunnyWeatherIcon(modifier, animate)
        WeatherAnimation.STILL -> Icon(when (conditionFor(symbolCode)) {
                R.string.weather_clear -> if (symbolCode?.endsWith("_night") == true) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny
                R.string.weather_thunder -> Icons.Rounded.Thunderstorm
                R.string.weather_unknown -> Icons.Rounded.CloudOff
                else -> Icons.Rounded.Cloud
            }, contentDescription = null, tint = MaterialTheme.colorScheme.secondary,
                modifier = modifier.testTag("weather_symbol_static"))
        else -> CloudWeatherIcon(kind, modifier, animate)
    }
}

@Composable
internal fun rememberWeatherMotionAllowed(animate: Boolean): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    var systemAllowsMotion by remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        // Sjekker igjen etter et besøk i telefonens innstillinger. Avslåtte animasjoner respekteres.
        systemAllowsMotion = ValueAnimator.areAnimatorsEnabled()
    }
    // Samme regel for alle ikonene. De skal ikke fortsette å bevege seg i bakgrunnen.
    return animate && systemAllowsMotion && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
}

@Composable
internal fun SunnyWeatherIcon(modifier: Modifier = Modifier, animate: Boolean = true) {
    val running = rememberWeatherMotionAllowed(animate)
    if (running) {
        // Én liten løkke for denne sola. Den fjernes når kortet forsvinner eller appen går i bakgrunnen.
        val transition = rememberInfiniteTransition(label = "sol")
        val rotation = transition.animateFloat(0f, 360f,
            animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "solstråler")
        val glow = transition.animateFloat(0.15f, 0.42f,
            animationSpec = infiniteRepeatable(tween(3_000), RepeatMode.Reverse), label = "solglød")
        SunDrawing(modifier.testTag("weather_sun_animated"),
            rotation = { rotation.value }, glow = { glow.value })
    } else {
        // Stillbildet har samme størrelse. Kortet hopper ikke når animasjonen stopper.
        SunDrawing(modifier.testTag("weather_sun_static"), rotation = { 0f }, glow = { 0.2f })
    }
}

@Composable
private fun SunDrawing(modifier: Modifier, rotation: () -> Float, glow: () -> Float) {
    val glowBrush = remember {
        Brush.radialGradient(listOf(Color(0xFFFFD56A), Color.Transparent))
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        // Bare gløden endrer styrke. graphicsLayer leser animasjonen under tegningen,
        // så resten av værkortet slipper å bygges på nytt for hvert bilde i animasjonen.
        Box(Modifier.matchParentSize().graphicsLayer { alpha = glow() }
            .background(glowBrush, CircleShape))
        // Samme Material-ikon som før, ikke en ny vektortegning eller en GIF.
        Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = Color(0xFFCB850E),
            modifier = Modifier.fillMaxSize().padding(4.dp).graphicsLayer { rotationZ = rotation() })
    }
}
