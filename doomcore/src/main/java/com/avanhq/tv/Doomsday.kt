package com.avanhq.tv

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/** The date and the colours of the Doomsday countdown, for the launcher's Doomsday screen
 *  (Countdown.kt) and the Doomsday app (tv/doomsday). */
object Countdown {
    const val TITLE = "Avengers: Doomsday"
    const val WHEN = "Wednesday 16 December"
    // The video D sent on 31 Oct (youtu.be/X1aFkAkFASk).
    const val VIDEO = "X1aFkAkFASk"
    val AT: ZonedDateTime = ZonedDateTime.of(2026, 12, 16, 0, 0, 0, 0, ZoneId.of("Europe/London"))

    /** The pictures (their slot numbers) and their names on the option chips. */
    val OPTIONS = listOf("hall" to "Hall", "mask" to "Mask", "tapestry" to "Tapestry", "throne" to "Throne", "rotate" to "Rotate")
    const val ROTATE_MS = 60_000L

    /** The screen's own colour for the orb and the focus rings: Doom's green fire, not the
     *  accent. */
    val GREEN: Int = 0xFF60EC96.toInt() // rgb(96, 236, 150); a plain number so the JVM tests can load this

    /** The heading: the film's name, or "… is here" from release day on. */
    fun heading(now: ZonedDateTime = ZonedDateTime.now(AT.zone)): String =
        if (left(now).all { it == 0L }) "$TITLE is here" else TITLE

    /** Months, days, hours, minutes and seconds left, as the film's own clock shows them
     *: whole calendar months first, then the rest; all zero once it's out. */
    fun parts(now: ZonedDateTime = ZonedDateTime.now(AT.zone)): LongArray {
        val from = now.withZoneSameInstant(AT.zone)
        if (!from.isBefore(AT)) return LongArray(5)
        var months = 0L
        while (!from.plusMonths(months + 1).isAfter(AT)) months++
        val s = Duration.between(from.plusMonths(months), AT).seconds
        return longArrayOf(months, s / 86_400, s % 86_400 / 3_600, s % 3_600 / 60, s % 60)
    }

    /** Quiet hours, when no sound plays: [from] (inclusive) to [to] hours, across midnight. */
    fun quiet(hour: Int, from: Int, to: Int): Boolean = if (from > to) hour >= from || hour < to else hour in from until to

    /** Days, hours, minutes and seconds left; all zero once it's out. */
    fun left(now: ZonedDateTime = ZonedDateTime.now(AT.zone)): LongArray {
        val s = Duration.between(now, AT).seconds.coerceAtLeast(0)
        return longArrayOf(s / 86_400, s % 86_400 / 3_600, s % 3_600 / 60, s % 60)
    }
}
