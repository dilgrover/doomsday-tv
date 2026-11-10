package com.avanhq.doomsday

import org.junit.Assert.assertEquals
import org.junit.Test
import com.avanhq.tv.Countdown
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** The settings' choices (Options): a town kept and read back, Rotate's minutes, the clock's
 *  words, the night hours, and release day (Countdown.heading). */
class SettingsTest {
    @Test
    fun aTownIsKeptAndReadBack() {
        val back = Options.decode(Options.encode(Options.City("Saint-Denis|x", 48.9362, 2.3574)))!!
        assertEquals("Saint-Denis x", back.name)
        assertEquals(48.9362, back.lat, 1e-9)
        assertEquals(2.3574, back.lon, 1e-9)
        assertEquals(null, Options.decode(null))
        assertEquals(null, Options.decode("London"))
        assertEquals(null, Options.decode("London|north|0"))
        assertEquals("London", Options.LONDON.name)
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
