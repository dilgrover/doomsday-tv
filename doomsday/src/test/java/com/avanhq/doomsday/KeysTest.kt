package com.avanhq.doomsday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Pictures screen's own keyboard (Keys): moving round it and what each key does. */
class KeysTest {
    /** Move to [key] the way the remote would (rows, then along), and press it. */
    private fun Keys.tap(key: String): Boolean {
        val r = Keys.ROWS.indexOfFirst { row -> row.any { it.first == key } }
        while (row != r) down()
        val c = Keys.ROWS[r].indexOfFirst { it.first == key }
        while (col != c) right()
        return press()
    }

    @Test fun typesACode() {
        val k = Keys("https://tinyurl.com/")
        for (ch in "my-hall") k.tap(ch.toString())
        assertEquals("https://tinyurl.com/my-hall", k.text)
        assertTrue(k.tap(Keys.DONE))
    }

    @Test fun deleteClearAndTinyurl() {
        val k = Keys("abc")
        assertFalse(k.tap(Keys.DELETE))
        assertEquals("ab", k.text)
        k.tap(Keys.CLEAR)
        assertEquals("", k.text)
        k.tap(Keys.TINY)
        assertEquals(Links.TINY, k.text)
        k.clear()
        assertEquals("", k.text)
    }

    @Test fun arrowsWrap() {
        val k = Keys()
        k.left()
        assertEquals("0", k.key)
        k.right()
        assertEquals("1", k.key)
        k.up()
        assertEquals(":", k.key)
        k.down()
        assertEquals("1", k.key)
    }

    @Test fun downOntoWideKeysAndBack() {
        val k = Keys()
        repeat(3) { k.down() }
        repeat(9) { k.right() }
        assertEquals("_", k.key)
        k.down()
        assertEquals(Keys.DONE, k.key)
        k.up()
        assertEquals("_", k.key) // straight back up from Done
        repeat(3) { k.left() }
        k.down()
        assertEquals(Keys.TINY, k.key)
    }

    @Test fun everyRowIsTenWide() {
        for (row in Keys.ROWS) assertEquals(Keys.UNITS, row.sumOf { it.second })
    }

    @Test fun fireTvShowsPhoneStyleAppsToo() {
        val tv = listOf("netflix", "youtube")
        val phone = listOf("youtube", "prime", "com.avanhq.doomsday")
        assertEquals(tv, Device.appsToShow(tv, phone, fire = false, self = "com.avanhq.doomsday"))
        assertEquals(listOf("netflix", "youtube", "prime"), Device.appsToShow(tv, phone, fire = true, self = "com.avanhq.doomsday"))
        assertEquals(listOf("youtube", "prime"), Device.appsToShow(emptyList(), phone, fire = false, self = "com.avanhq.doomsday"))
    }
}
