package com.avanhq.doomsday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The weather line (Weather): Open-Meteo's answer in words. */
class WeatherTest {
    @Test
    fun theLineReadsWell() {
        val json = """{"latitude":51.5,"current":{"time":"2026-11-10T13:00","temperature_2m":9.6,"weather_code":3}}"""
        assertEquals("London  ·  Cloudy 10°", Weather.line("London", json))
    }

    @Test
    fun belowZeroAndRain() {
        val json = """{"current":{"temperature_2m":-1.4,"weather_code":63}}"""
        assertEquals("Edinburgh  ·  Rain -1°", Weather.line("Edinburgh", json))
    }

    @Test
    fun noReadingNoLine() {
        assertNull(Weather.line("London", "{}"))
        assertNull(Weather.line("London", "not json"))
    }

    @Test
    fun codesInWords() {
        assertEquals("Clear", Weather.words(0))
        assertEquals("Partly cloudy", Weather.words(2))
        assertEquals("Fog", Weather.words(45))
        assertEquals("Snow", Weather.words(73))
        assertEquals("Thunderstorms", Weather.words(95))
        assertEquals("Weather", Weather.words(42))
    }

    @Test
    fun theAskIsForTheCity() {
        val url = Weather.url(Options.City("Delhi", 28.6139, 77.209))
        assertTrue(url, url.startsWith("https://api.open-meteo.com/") && "latitude=28.6139" in url && "longitude=77.209" in url)
    }
}
