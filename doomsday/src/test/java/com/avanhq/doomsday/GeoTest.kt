package com.avanhq.doomsday

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Where the weather is for (Geo): the town from the internet connection, and a search by name. */
class GeoTest {
    @Test fun ipwhoFindsTheTown() {
        val c = Geo.fromIp("""{"success":true,"city":"Leeds","latitude":53.8,"longitude":-1.55}""")!!
        assertEquals("Leeds", c.name)
        assertEquals(53.8, c.lat, 1e-9)
    }

    @Test fun ipapiFindsItToo() {
        val c = Geo.fromIp("""{"ip":"1.2.3.4","city":"Leeds","region":"England","latitude":53.79,"longitude":-1.54}""")!!
        assertEquals("Leeds", c.name)
    }

    @Test fun noTownNoAnswer() {
        assertNull(Geo.fromIp("""{"success":false,"message":"Reserved range"}"""))
        assertNull(Geo.fromIp("""{"error":true,"reason":"RateLimited"}"""))
        assertNull(Geo.fromIp("""{"success":true,"city":"","latitude":1,"longitude":2}"""))
        assertNull(Geo.fromIp("not json"))
    }

    @Test fun searchListsTownsWithWhereTheyAre() {
        val json = """{"results":[
            {"name":"Richmond","latitude":51.461,"longitude":-0.303,"country":"United Kingdom","admin1":"England"},
            {"name":"Richmond","latitude":37.55,"longitude":-77.46,"country":"United States","admin1":"Virginia"},
            {"name":"Monaco","latitude":43.73,"longitude":7.42,"country":"Monaco","admin1":"Monaco"},
            {"name":"","latitude":1,"longitude":1}]}"""
        val found = Geo.results(json)
        assertEquals(listOf("Richmond, England, United Kingdom", "Richmond, Virginia, United States", "Monaco, Monaco"), found.map { it.label })
        assertEquals(37.55, found[1].city.lat, 1e-9)
    }

    @Test fun nothingFoundIsAnEmptyList() {
        assertEquals(0, Geo.results("""{"generationtime_ms":0.3}""").size)
        assertEquals(0, Geo.results("oops").size)
    }

    @Test fun theSearchAskIsEncoded() {
        val u = Geo.searchUrl("  New York ")
        assertTrue(u, u.startsWith("https://geocoding-api.open-meteo.com/") && "name=New+York&" in u && "count=8" in u)
    }
}
