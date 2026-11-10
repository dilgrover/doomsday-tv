package com.avanhq.doomsday

import org.junit.Assert.assertEquals
import org.junit.Test

/** The apps row's order and hiding (AppOrder): yours first, then the usual favourites, then by name. */
class AppOrderTest {
    private val self = "com.avanhq.doomsday"
    private val apps = listOf(
        AppOrder.App("com.zeta", "Zeta"),
        AppOrder.App("com.netflix.ninja", "Netflix"),
        AppOrder.App("com.alpha", "Alpha"),
        AppOrder.App("com.google.android.youtube.tv", "YouTube"),
        AppOrder.App(self, "Doomsday"),
        AppOrder.App("com.android.vending", "Play Store"),
    )

    private fun pkgs(l: List<AppOrder.App>) = l.map { it.pkg }

    @Test fun usualOrderThenByName() {
        assertEquals(listOf("com.google.android.youtube.tv", "com.netflix.ninja", "com.alpha", "com.zeta"), pkgs(AppOrder.arrange(apps, emptyList(), emptySet(), self)))
    }

    @Test fun yourOrderFirst() {
        val got = AppOrder.arrange(apps, listOf("com.zeta", "com.netflix.ninja"), emptySet(), self)
        assertEquals(listOf("com.zeta", "com.netflix.ninja", "com.google.android.youtube.tv", "com.alpha"), pkgs(got))
    }

    @Test fun hiddenLeftOutButListedInSettings() {
        val hidden = setOf("com.alpha")
        assertEquals(listOf("com.google.android.youtube.tv", "com.netflix.ninja", "com.zeta"), pkgs(AppOrder.arrange(apps, emptyList(), hidden, self)))
        assertEquals(4, AppOrder.arrange(apps, emptyList(), hidden, self, all = true).size)
    }

    @Test fun moves() {
        val shown = listOf("a", "b", "c", "d")
        assertEquals(listOf("a", "c", "b", "d"), AppOrder.move(shown, "c", -1))
        assertEquals(listOf("a", "b", "d", "c"), AppOrder.move(shown, "c", 1))
        assertEquals(shown, AppOrder.move(shown, "a", -1)) // already first
        assertEquals(shown, AppOrder.move(shown, "d", 1)) // already last
        assertEquals(shown, AppOrder.move(shown, "x", 1)) // not in the row
        assertEquals(listOf("c", "a", "b", "d"), AppOrder.front(shown, "c"))
    }

    @Test fun hideAndShow() {
        assertEquals(setOf("a"), AppOrder.toggle(emptySet(), "a"))
        assertEquals(emptySet<String>(), AppOrder.toggle(setOf("a"), "a"))
    }

    @Test fun savedListRoundTrip() {
        assertEquals(listOf("a.b", "c.d"), AppOrder.decode(AppOrder.encode(listOf("a.b", "c.d"))))
        assertEquals(emptyList<String>(), AppOrder.decode(null))
        assertEquals(emptyList<String>(), AppOrder.decode(""))
    }
}
