package com.avanhq.doomsday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The picture links (Links): what's typed, what may be fetched, what counts as a picture. */
class LinksTest {
    @Test
    fun aBareCodeIsATinyurlOne() {
        assertEquals("https://tinyurl.com/abc123x", Links.normalise("abc123x"))
        assertEquals("https://tinyurl.com/abc123x", Links.normalise("  https://tinyurl.com/abc123x "))
        assertEquals("https://bit.ly/abc123", Links.normalise("https://bit.ly/abc123"))
    }

    @Test
    fun theStartAloneIsNothingToFetch() {
        assertNull(Links.normalise(""))
        assertNull(Links.normalise(Links.TINY))
        assertNull(Links.normalise(Links.BITLY))
        assertNull(Links.normalise("https://"))
    }

    @Test
    fun aLinkWithoutItsStartGetsHttps() {
        assertEquals("https://example.com/a.jpg", Links.normalise("example.com/a.jpg"))
    }

    @Test
    fun httpOnlyFromAComputerAtHome() {
        assertNull(Links.problem("https://example.com/a.jpg"))
        assertNull(Links.problem("http://192.168.1.20:8080/hall.jpg"))
        assertNull(Links.problem("http://10.0.0.5/a.png"))
        assertNotNull(Links.problem("http://example.com/a.jpg"))
        assertNotNull(Links.problem("ftp://example.com/a.jpg"))
        assertNotNull(Links.problem("not a link"))
        assertTrue(Links.isHome("172.20.1.1"))
        assertFalse(Links.isHome("172.40.1.1"))
        assertFalse(Links.isHome("192.168.example.com"))
    }

    @Test
    fun picturesAreKnownByTheirFirstBytes() {
        assertEquals("jpeg", Links.kind(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())))
        assertEquals("png", Links.kind(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10)))
        assertEquals("webp", Links.kind("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray(Charsets.US_ASCII)))
        assertNull(Links.kind("<!DOCTYPE html>".toByteArray()))
    }

    @Test
    fun aRefusalInPlainWords() {
        assertEquals("The site didn't let the TV download it (403).", Links.refused(403))
        assertEquals("Nothing at that link (404).", Links.refused(404))
    }

    @Test
    fun theSiteName() {
        assertEquals("example.com", Links.host("https://www.example.com/images/a.jpg"))
    }
}

/** the four known pictures are known by their fingerprint; anything else gets the places for any picture. */
class PlacesTest {
    @Test
    fun dsPicturesGetTheirMeasuredPlaces() {
        val throne = com.avanhq.tv.DoomFx.placesFor("c2e59dd721cf1b3255ca70d8dda6cb7c43d58d30c42745f91e02d5a562c4a005")
        assertTrue(throne === com.avanhq.tv.DoomFx.SCENES[3])
        assertTrue(throne.eyes.isNotEmpty())
    }

    @Test
    fun anotherPictureHasNoEyesToLight() {
        val other = com.avanhq.tv.DoomFx.placesFor("0".repeat(64))
        assertTrue(other === com.avanhq.tv.DoomFx.ANY)
        assertTrue(other.eyes.isEmpty() && other.hands.isEmpty())
    }
}
