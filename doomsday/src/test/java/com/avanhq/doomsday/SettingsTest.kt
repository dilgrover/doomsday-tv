package com.avanhq.doomsday

import org.junit.Assert.assertEquals
import org.junit.Test
import com.avanhq.tv.Countdown
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** The settings' choices (Options): the cities, Rotate's minutes, the clock's words, the night
 *  hours, and release day (Countdown.heading). */
class SettingsTest {
    @Test
    fun anUnknownCityIsLondon() {
        assertEquals("London", Options.city(null).name)
        assertEquals("London", Options.city("Atlantis").name)
        assertEquals("Birmingham", Options.city("Birmingham").name)
    }

    @Test
    fun rotateChoices() {
        assertEquals(listOf(1, 5, 15), Options.ROTATE_MINUTES)
    }

    @Test
    fun theClockIn24Or12Hours() {
        assertEquals("21:05", Options.clock(LocalTime.of(21, 5), true))
        assertEquals("9:05", Options.clock(LocalTime.of(21, 5), false))
        assertEquals("00:30", Options.clock(LocalTime.of(0, 30), true))
        assertEquals("12:30", Options.clock(LocalTime.of(0, 30), false))
    }

    @Test
    fun nightIsElevenToSeven() {
        assertTrue(Options.night(LocalTime.of(23, 0)))
        assertTrue(Options.night(LocalTime.of(3, 30)))
        assertTrue(Options.night(LocalTime.of(6, 59)))
        assertFalse(Options.night(LocalTime.of(7, 0)))
        assertFalse(Options.night(LocalTime.of(22, 59)))
    }

    @Test
    fun releaseDaySaysItIsHere() {
        val london = ZoneId.of("Europe/London")
        val before = ZonedDateTime.of(2026, 12, 15, 23, 59, 59, 0, london)
        val day = ZonedDateTime.of(2026, 12, 16, 0, 0, 0, 0, london)
        val after = ZonedDateTime.of(2027, 1, 2, 12, 0, 0, 0, london)
        assertEquals("Avengers: Doomsday", Countdown.heading(before))
        assertEquals(listOf(0L, 0L, 0L, 1L), Countdown.left(before).toList())
        assertEquals("Avengers: Doomsday is here", Countdown.heading(day))
        assertEquals("Avengers: Doomsday is here", Countdown.heading(after))
        assertEquals(listOf(0L, 0L, 0L, 0L), Countdown.left(after).toList())
    }
}
