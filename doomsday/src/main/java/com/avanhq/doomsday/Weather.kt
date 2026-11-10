package com.avanhq.doomsday

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * The weather line under the clock ("London · Cloudy 10°"), from Open-Meteo (free, no key; D,
 * 10 Nov). Fetched when the countdown opens and every 30 minutes; offline, the line just goes.
 * The reading of the answer is plain functions, tested on the JVM (WeatherTest).
 */
class Weather(private val shown: (String?) -> Unit) {
    companion object {
        const val EVERY_MS = 30 * 60_000L

        fun url(c: Options.City) =
            "https://api.open-meteo.com/v1/forecast?latitude=${c.lat}&longitude=${c.lon}&current=temperature_2m,weather_code&timezone=auto"

        /** "London · Cloudy 10°" from Open-Meteo's answer, or null if it has no reading. */
        fun line(city: String, json: String): String? = runCatching {
            val now = JSONObject(json).getJSONObject("current")
            val temp = Math.round(now.getDouble("temperature_2m"))
            "$city  ·  ${words(now.getInt("weather_code"))} $temp°"
        }.getOrNull()

        /** The WMO weather code in words. */
        fun words(code: Int): String = when (code) {
            0 -> "Clear"
            1 -> "Mostly clear"
            2 -> "Partly cloudy"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            in 51..57 -> "Drizzle"
            in 61..67 -> "Rain"
            in 71..77 -> "Snow"
            in 80..82 -> "Showers"
            85, 86 -> "Snow showers"
            in 95..99 -> "Thunderstorms"
            else -> "Weather"
        }
    }

    private val main = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()
    private var city: Options.City? = null

    private val tick = object : Runnable {
        override fun run() {
            fetch()
            main.postDelayed(this, EVERY_MS)
        }
    }

    /** Show [c]'s weather now and every 30 minutes (null: off). */
    fun start(c: Options.City?) {
        stop()
        city = c
        if (c == null) shown(null) else main.post(tick)
    }

    fun stop() = main.removeCallbacks(tick)

    private fun fetch() {
        val c = city ?: return
        Thread {
            val got = runCatching {
                client.newCall(Request.Builder().url(url(c)).build()).execute().use { r ->
                    if (r.isSuccessful) line(c.name, r.body?.string().orEmpty()) else null
                }
            }.getOrNull()
            main.post { if (city === c) shown(got) }
        }.start()
    }
}
