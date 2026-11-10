package com.avanhq.doomsday

import com.avanhq.tv.Debounce
import org.junit.Assert.assertEquals
import org.junit.Test

/** Several package changes close together (an update is a removal and an addition) make one refresh. */
class DebounceTest {
    private class Clock {
        var now = 0L
        val queue = mutableListOf<Pair<Long, Runnable>>()
        fun advance(ms: Long) {
            now += ms
            queue.filter { it.first <= now }.forEach { queue.remove(it); it.second.run() }
        }
    }

    @Test
    fun manyPokesMakeOneRunAfterTheLast() {
        val clock = Clock()
        var runs = 0
        val d = Debounce(1_000, { r, ms -> clock.queue += (clock.now + ms) to r }, { r -> clock.queue.removeAll { it.second === r } }) { runs++ }
        d.poke()
        clock.advance(400)
        d.poke()
        clock.advance(400)
        d.poke()
        clock.advance(900)
        assertEquals(0, runs)
        clock.advance(200)
        assertEquals(1, runs)
        clock.advance(5_000)
        assertEquals(1, runs)
        d.poke()
        clock.advance(1_000)
        assertEquals(2, runs)
    }
}
