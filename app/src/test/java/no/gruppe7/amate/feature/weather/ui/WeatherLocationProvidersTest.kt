package no.gruppe7.amate.feature.weather.ui

import no.gruppe7.amate.feature.weather.location.weatherLocationProviders
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherLocationProvidersTest {
    @Test fun modernPhoneWithNetworkLocation_prefersFused() {
        assertEquals(listOf("fused", "network"), weatherLocationProviders(31, networkEnabled = true))
    }

    @Test fun emulatorWithoutNetworkLocation_triesCoarseGpsFirst() {
        assertEquals(listOf("gps", "fused", "network"), weatherLocationProviders(37, networkEnabled = false))
    }

    @Test fun olderAndroid_doesNotRequestGpsWithOnlyCoarsePermission() {
        listOf(26, 28, 30).forEach { version ->
            assertEquals(listOf("network"), weatherLocationProviders(version, networkEnabled = true))
            assertEquals(listOf("network"), weatherLocationProviders(version, networkEnabled = false))
        }
    }
}
