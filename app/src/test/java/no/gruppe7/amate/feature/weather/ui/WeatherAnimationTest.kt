package no.gruppe7.amate.feature.weather.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherAnimationTest {
    @Test fun metCodesSelectTheRightAnimation() {
        assertEquals(WeatherAnimation.SUN, weatherAnimationFor("clearsky_day"))
        assertEquals(WeatherAnimation.PARTLY_CLOUDY, weatherAnimationFor("fair_day"))
        assertEquals(WeatherAnimation.PARTLY_CLOUDY, weatherAnimationFor("partlycloudy_day"))
        assertEquals(WeatherAnimation.CLOUD, weatherAnimationFor("cloudy"))
        assertEquals(WeatherAnimation.RAIN, weatherAnimationFor("lightrainshowers_day"))
        assertEquals(WeatherAnimation.RAIN, weatherAnimationFor("heavyrain"))
        assertEquals(WeatherAnimation.SNOW, weatherAnimationFor("lightsnowshowers_night"))
        assertEquals(WeatherAnimation.SLEET, weatherAnimationFor("heavysleet"))
    }

    @Test fun nightAndPolarTwilightDoNotGetSunshine() {
        assertEquals(WeatherAnimation.STILL, weatherAnimationFor("clearsky_night"))
        assertEquals(WeatherAnimation.STILL, weatherAnimationFor("clearsky_polartwilight"))
        assertEquals(WeatherAnimation.CLOUD, weatherAnimationFor("partlycloudy_night"))
        assertEquals(WeatherAnimation.CLOUD, weatherAnimationFor("fair_polartwilight"))
    }

    @Test fun thunderFogAndUnknownRemainStatic() {
        assertEquals(WeatherAnimation.STILL, weatherAnimationFor("heavyrainandthunder"))
        assertEquals(WeatherAnimation.STILL, weatherAnimationFor("snowandthunder"))
        assertEquals(WeatherAnimation.STILL, weatherAnimationFor("fog"))
        assertEquals(WeatherAnimation.STILL, weatherAnimationFor("new_weather_code"))
        assertEquals(WeatherAnimation.STILL, weatherAnimationFor(null))
    }
}
