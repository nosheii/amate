package no.gruppe7.amate.feature.weather.ui

import android.animation.ValueAnimator
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import no.gruppe7.amate.feature.home.ui.HomeShellTheme
import no.gruppe7.amate.feature.weather.data.WeatherForecast
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherIconTest {
    @get:Rule val compose = createComposeRule()
    private val forecast = WeatherForecast(WeatherPlace.OSLO, 6.5, "clearsky_day",
        Instant.parse("2026-11-01T12:00:00Z"), Instant.parse("2026-11-01T10:00:00Z"))

    @Test fun sunnyDay_animatesWithoutChangingTemperature() {
        // Hvis telefonens animasjoner er slått av, skal sola med vilje stå stille.
        assumeTrue(ValueAnimator.areAnimatorsEnabled())
        // Testklokken må styres manuelt fra starten, ellers stopper Compose uendelige testanimasjoner.
        compose.mainClock.autoAdvance = false
        compose.setContent { HomeShellTheme { WeatherCard(WeatherUiState(isLoading = false, forecast = forecast)) } }
        compose.mainClock.advanceTimeBy(32)
        compose.onNodeWithTag("weather_sun_animated").assertIsDisplayed()
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
        val before = compose.onNodeWithTag("weather_sun_animated").captureToImage().asAndroidBitmap()
        compose.mainClock.advanceTimeBy(1_200)
        val after = compose.onNodeWithTag("weather_sun_animated").captureToImage().asAndroidBitmap()
        assertFalse("Sola skal faktisk bevege seg, ikke bare ha et animasjonsnavn", before.sameAs(after))
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
    }

    @Test fun newWeatherReplacesSun_andNightNeverGetsAnAnimatedSun() {
        val state = mutableStateOf(WeatherUiState(isLoading = false, forecast = forecast))
        compose.setContent { HomeShellTheme { WeatherCard(state.value) } }
        val examples = listOf("clearsky_night" to "symbol", "cloudy" to "cloud", "rain" to "rain",
            "snow" to "snow", "unknown" to "symbol", null to "symbol")
        for ((symbol, tag) in examples) {
            compose.runOnIdle { state.value = state.value.copy(forecast = forecast.copy(symbolCode = symbol)) }
            if (tag == "symbol") compose.onNodeWithTag("weather_symbol_static").assertIsDisplayed()
            else {
                // På telefoner med avslåtte animasjoner er det riktig å få et stillbilde i stedet.
                val suffix = if (ValueAnimator.areAnimatorsEnabled()) "animated" else "static"
                compose.onNodeWithTag("weather_${tag}_$suffix").assertIsDisplayed()
            }
            compose.onNodeWithTag("weather_sun_animated").assertDoesNotExist()
            compose.onNodeWithTag("weather_sun_static").assertDoesNotExist()
        }
    }

    @Test fun loadingOrFailedRefresh_keepsSunStillAndPreservesData() {
        val state = mutableStateOf(WeatherUiState(isLoading = true, forecast = forecast))
        compose.setContent { HomeShellTheme { WeatherCard(state.value) } }
        compose.onNodeWithTag("weather_sun_static").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(isLoading = false, loadFailed = true) }
        compose.onNodeWithTag("weather_sun_static").assertIsDisplayed()
        compose.onNodeWithTag("weather_sun_animated").assertDoesNotExist()
        compose.onNodeWithTag("weather_error").assertIsDisplayed()
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
    }

    @Test fun animationStopsWhenScreenIsNotResumed_andRestartsOnReturn() {
        assumeTrue(ValueAnimator.areAnimatorsEnabled())
        lateinit var owner: TestOwner
        compose.runOnIdle { owner = TestOwner().apply { registry.currentState = Lifecycle.State.RESUMED } }
        compose.setContent { HomeShellTheme {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) { SunnyWeatherIcon(Modifier.size(36.dp)) }
        } }
        compose.onNodeWithTag("weather_sun_animated").assertIsDisplayed()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        compose.onNodeWithTag("weather_sun_static").assertIsDisplayed()
        compose.onNodeWithTag("weather_sun_animated").assertDoesNotExist()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.onNodeWithTag("weather_sun_animated").assertIsDisplayed()
    }

    @Test fun weatherDetailsContainNoManualWeatherControls() {
        compose.setContent { HomeShellTheme {
            WeatherCard(WeatherUiState(isLoading = false, forecast = forecast.copy(symbolCode = "cloudy")))
        } }
        compose.onNodeWithTag("weather_details_toggle").performClick()
        // Også debug-appen skal nå bare vise ekte vær. Testknappene skal ikke komme tilbake.
        compose.onNodeWithTag("weather_details").assertIsDisplayed()
        compose.onNodeWithText("Test væranimasjoner").assertDoesNotExist()
        compose.onNodeWithText("Skjul væranimasjoner").assertDoesNotExist()
        compose.onNodeWithTag("weather_animation_preview_toggle").assertDoesNotExist()
        compose.onNodeWithTag("weather_animation_preview").assertDoesNotExist()
        compose.onNodeWithTag("weather_current_condition").assertTextEquals("Skyet")
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
        compose.onNodeWithTag("weather_details_toggle").performClick()
        compose.onNodeWithTag("weather_details").assertDoesNotExist()
        compose.onNodeWithText("Skyet").assertIsDisplayed()
    }

    @Test fun cloudsRainSnowAndSleetActuallyMove() {
        assumeTrue(ValueAnimator.areAnimatorsEnabled())
        val symbol = mutableStateOf("cloudy")
        compose.mainClock.autoAdvance = false
        compose.setContent { HomeShellTheme { WeatherIcon(symbol.value, animate = true, modifier = Modifier.size(64.dp)) } }
        val examples = listOf("cloudy" to "cloud", "rain" to "rain", "snow" to "snow",
            "sleet" to "sleet", "partlycloudy_day" to "partly_cloudy")
        for ((code, tag) in examples) {
            compose.runOnIdle { symbol.value = code }
            compose.mainClock.advanceTimeBy(64)
            val node = compose.onNodeWithTag("weather_${tag}_animated")
            val before = node.captureToImage().asAndroidBitmap()
            compose.mainClock.advanceTimeBy(1_200)
            val after = node.captureToImage().asAndroidBitmap()
            assertFalse("$code skal ha synlig bevegelse", before.sameAs(after))
        }
    }

    @Test fun allCloudTypesCanBeStill_andPausedScreenStopsThem() {
        lateinit var owner: TestOwner
        compose.runOnIdle { owner = TestOwner().apply { registry.currentState = Lifecycle.State.STARTED } }
        val symbol = mutableStateOf("cloudy")
        val animate = mutableStateOf(true)
        compose.setContent { HomeShellTheme {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                WeatherIcon(symbol.value, animate.value, Modifier.size(36.dp))
            }
        } }
        val examples = listOf("cloudy" to "cloud", "rain" to "rain", "snow" to "snow",
            "sleet" to "sleet", "partlycloudy_day" to "partly_cloudy")
        for ((code, tag) in examples) {
            compose.runOnIdle { symbol.value = code }
            compose.onNodeWithTag("weather_${tag}_static").assertIsDisplayed()
        }
        compose.runOnIdle {
            // Selv med aktiv skjerm kan værkortet be om stillbilde ved lasting eller feil.
            animate.value = false
            owner.registry.currentState = Lifecycle.State.RESUMED
        }
        compose.onNodeWithTag("weather_partly_cloudy_static").assertIsDisplayed()
        compose.onNodeWithTag("weather_partly_cloudy_animated").assertDoesNotExist()
    }

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
}
