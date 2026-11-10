package com.avanhq.doomsday

import org.json.JSONObject
import java.net.URLEncoder

/**
 * Where the weather is for: Automatic finds the town from the internet connection (ipwho.is,
 * then ipapi.co if that fails; https, no key, no account), and Choose a city searches any town
 * by name (Open-Meteo's place search, the same service as the weather). Only the reading of
 * their answers lives here, plain, so it's tested on the JVM (GeoTest).
 */
object Geo {
    const val IP = "https://ipwho.is/?fields=success,city,latitude,longitude"
    const val IP_SPARE = "https://ipapi.co/json/"

    /** Found once a day; a TV doesn't move. */
    const val DAY_MS = 24 * 60 * 60_000L

    fun searchUrl(name: String): String =
        "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(name.trim(), "UTF-8")}&count=8&language=en&format=json"

    /** The town in an IP-location answer (ipwho.is or ipapi.co), or null if it found none. */
    fun fromIp(json: String): Options.City? = runCatching {
        val o = JSONObject(json)
        if (o.has("success") && !o.optBoolean("success")) return null
        if (o.optBoolean("error")) return null
        val name = o.optString("city").trim()
        if (name.isEmpty() || !o.has("latitude") || !o.has("longitude")) return null
        Options.City(name, o.getDouble("latitude"), o.getDouble("longitude"))
    }.getOrNull()

    /** A place found by name, with its region and country for telling same-named towns apart. */
    class Found(val city: Options.City, val label: String)

    /** The places in Open-Meteo's search answer, best first (none: an empty list). */
    fun results(json: String): List<Found> = runCatching {
        val list = JSONObject(json).optJSONArray("results") ?: return emptyList()
        (0 until list.length()).mapNotNull { i ->
            val o = list.getJSONObject(i)
            val name = o.optString("name").trim()
            if (name.isEmpty() || !o.has("latitude") || !o.has("longitude")) return@mapNotNull null
            val region = o.optString("admin1").trim().takeIf { it.isNotEmpty() && it != name }
            val country = o.optString("country").trim().takeIf { it.isNotEmpty() }
            Found(Options.City(name, o.getDouble("latitude"), o.getDouble("longitude")), listOfNotNull(name, region, country).joinToString(", "))
        }
    }.getOrDefault(emptyList())
}
