package no.gruppe7.amate.feature.weather.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import no.gruppe7.amate.feature.home.ui.HomeScreen
import no.gruppe7.amate.feature.home.ui.HomeShellTheme
import no.gruppe7.amate.feature.weather.data.WeatherForecast
import no.gruppe7.amate.feature.weather.data.WeatherPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeatherCardTest {
    @get:Rule val compose = createComposeRule()
    private val forecast = WeatherForecast(WeatherPlace.OSLO, 6.5, "cloudy",
        Instant.parse("2026-10-27T12:00:00Z"), Instant.parse("2026-10-27T10:00:00Z"))

    @Test fun loading_doesNotPretendTemperatureIsZero() {
        compose.setContent { HomeShellTheme { WeatherCard(WeatherUiState()) } }
        compose.onNodeWithText("Henter værvarsel…").assertIsDisplayed()
        compose.onNodeWithTag("weather_temperature").assertDoesNotExist()
        compose.onNodeWithTag("weather_refresh").assertIsNotEnabled()
    }

    @Test fun success_displaysForecastTemperatureTimeAndCredit() {
        compose.setContent { HomeShellTheme { WeatherCard(WeatherUiState(isLoading = false, forecast = forecast)) } }
        compose.onNodeWithText("Vær i Oslo").assertIsDisplayed()
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
        compose.onNodeWithText("Varsel for 27.10 13:00 (Oslo-tid)").assertIsDisplayed()
        compose.onNodeWithText("MET Norway").assertIsDisplayed()
        compose.onNodeWithText("CC BY 4.0").assertIsDisplayed()
    }

    @Test fun error_retryCallsCallbackAndCanShowSuccessfulResult() {
        val state = mutableStateOf(WeatherUiState(isLoading = false, loadFailed = true))
        var retries = 0
        compose.setContent { HomeShellTheme { WeatherCard(state.value, onRefresh = {
            retries++
            state.value = WeatherUiState(isLoading = false, forecast = forecast)
        }) } }
        compose.onNodeWithTag("weather_error").assertIsDisplayed()
        compose.onNodeWithTag("weather_temperature").assertDoesNotExist()
        compose.onNodeWithText("Prøv igjen").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
        compose.onNodeWithTag("weather_error").assertDoesNotExist()
    }

    @Test fun failedRefresh_keepsPreviousTemperatureWithWarning() {
        compose.setContent { HomeShellTheme {
            WeatherCard(WeatherUiState(isLoading = false, forecast = forecast, loadFailed = true))
        } }
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
        compose.onNodeWithText("Kunne ikke oppdatere. Viser forrige varsel, som kan være utdatert.").assertIsDisplayed()
    }

    @Test fun compactCard_keepsEssentialsAndDetailsCanExpandInsideIt() {
        compose.setContent { HomeShellTheme {
            // Tilsvarer omtrent plassen inne i et hjemmekort på en liten telefon.
            Box(Modifier.width(248.dp)) {
                WeatherCard(WeatherUiState(isLoading = false, forecast = forecast))
            }
        } }
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
        compose.onNodeWithText("Skyet").assertIsDisplayed()
        // Denne grensen gjelder innholdet, før hjemskjermens vanlige kortpadding.
        val bounds = compose.onNodeWithTag("weather_card").getUnclippedBoundsInRoot()
        val compactHeight = bounds.bottom - bounds.top
        assertTrue("Værkortet bør holde seg lite: $compactHeight", compactHeight <= 140.dp)
        compose.onNodeWithTag("weather_details").assertDoesNotExist()
        compose.onNodeWithTag("weather_details_toggle").performClick()
        compose.onNodeWithText("Laget av MET 27.10 11:00").assertIsDisplayed()
        compose.onNodeWithTag("weather_details_toggle").performClick()
        compose.onNodeWithTag("weather_details").assertDoesNotExist()
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
    }

    @Test fun home_displaysInjectedWeatherWithoutDoingItsOwnNetworkCall() {
        // Vi sender inn ferdige data. Ingen av UI-testene trenger å vente på ekte MET-svar.
        compose.setContent { HomeShellTheme {
            HomeScreen(weatherContent = { WeatherCard(WeatherUiState(isLoading = false, forecast = forecast)) })
        } }
        compose.onNodeWithTag("home_list").performScrollToNode(hasTestTag("weather_card"))
        compose.onNodeWithText("Vær i Oslo").assertIsDisplayed()
        compose.onNodeWithText("6,5 °C").assertIsDisplayed()
    }
}
