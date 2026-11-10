package com.avanhq.doomsday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Home button (Home): Doomsday stays quiet when another app already opens with Home. */
class HomeTest {
    private val self = "com.avanhq.doomsday"

    @Test fun avansWatcherWins() {
        val on = "com.avanhq.tv/com.avanhq.tv.HomeWatcher:com.avanhq.doomsday/com.avanhq.doomsday.HomeWatcher"
        assertEquals("Avan", Home.other(on, self))
        assertTrue(Home.mine(on, self))
    }

    @Test fun aloneItsOurs() {
        val on = "com.avanhq.doomsday/com.avanhq.doomsday.HomeWatcher"
        assertNull(Home.other(on, self))
        assertTrue(Home.mine(on, self))
    }

    @Test fun nothingOn() {
        assertNull(Home.other(null, self))
        assertNull(Home.other("", self))
        assertFalse(Home.mine(null, self))
        assertNull(Home.other("com.google.android.marvin.talkback/.TalkBackService", self))
    }

    @Test fun theTvsOwnHomes() {
        assertTrue("com.google.android.apps.tv.launcherx" in Home.HOMES)
        assertTrue("com.amazon.tv.launcher" in Home.HOMES)
    }
}
