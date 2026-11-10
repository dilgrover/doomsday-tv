package com.avanhq.doomsday

import com.avanhq.tv.Countdown
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** The film's own countdown: months first, then days, hours, minutes and seconds. */
class CountdownTest {
    private val london = ZoneId.of("Europe/London")
    private fun at(y: Int, mo: Int, d: Int, h: Int = 0, mi: Int = 0, s: Int = 0) = ZonedDateTime.of(y, mo, d, h, mi, s, 0, london)

    @Test fun monthThenDays() {
        // 10 Nov 13:40:00 to 16 Dec 00:00: one month (to 10 Dec 13:40), then 5 days 10 h 20 min.
        assertArrayEquals(longArrayOf(1, 5, 10, 20, 0), Countdown.parts(at(2026, 11, 10, 13, 40)))
    }

    @Test fun underAMonth() {
        assertArrayEquals(longArrayOf(0, 25, 23, 59, 59), Countdown.parts(at(2026, 11, 20, 0, 0, 1)))
        assertArrayEquals(longArrayOf(0, 0, 0, 0, 1), Countdown.parts(at(2026, 12, 15, 23, 59, 59)))
    }

    @Test fun exactlyAMonth() {
        assertArrayEquals(longArrayOf(1, 0, 0, 0, 0), Countdown.parts(at(2026, 11, 16)))
    }

    @Test fun acrossTheClocksGoingBack() {
        // 1 Oct to 16 Dec crosses the end of summer time (25 Oct): months stay calendar months.
        assertArrayEquals(longArrayOf(2, 15, 0, 0, 0), Countdown.parts(at(2026, 10, 1)))
    }

    @Test fun fromAnotherZone() {
        // The same moment seen from Delhi counts the same.
        val delhi = at(2026, 11, 10, 13, 40).withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
        assertArrayEquals(longArrayOf(1, 5, 10, 20, 0), Countdown.parts(delhi))
    }

    @Test fun releaseDay() {
        assertArrayEquals(LongArray(5), Countdown.parts(at(2026, 12, 16)))
        assertArrayEquals(LongArray(5), Countdown.parts(at(2027, 1, 2)))
        assertEquals("Avengers: Doomsday is here", Countdown.heading(at(2026, 12, 16)))
    }

    @Test fun quietHours() {
        assertTrue(Countdown.quiet(23, 23, 7))
        assertTrue(Countdown.quiet(3, 23, 7))
        assertFalse(Countdown.quiet(7, 23, 7))
        assertFalse(Countdown.quiet(22, 23, 7))
        assertTrue(Countdown.quiet(22, 22, 8))
        assertFalse(Countdown.quiet(12, 9, 9))
        assertTrue(Countdown.quiet(10, 9, 17))
    }
}
