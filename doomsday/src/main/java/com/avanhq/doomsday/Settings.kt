package com.avanhq.doomsday

import android.content.Context
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The app's few settings: how often Rotate
 * changes picture, the clock's hours, the weather line with its city, the effects (full or
 * calm) and dimming at night. The choices and the
 * clock's words are plain functions ([Options]), tested on the JVM (SettingsTest).
 */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var rotateMinutes: Int
        get() = prefs.getInt("rotate", 1).let { if (it in Options.ROTATE_MINUTES) it else 1 }
        set(v) = prefs.edit().putInt("rotate", v).apply()

    var clock24: Boolean
        get() = prefs.getBoolean("clock24", true)
        set(v) = prefs.edit().putBoolean("clock24", v).apply()

    var weatherOn: Boolean
        get() = prefs.getBoolean("weather", true)
        set(v) = prefs.edit().putBoolean("weather", v).apply()

    var city: Options.City
        get() = Options.city(prefs.getString("city", null))
        set(v) = prefs.edit().putString("city", v.name).apply()

    /** Effects: full, or calm (light, mist and dust; no jumps, no camera move). */
    var calm: Boolean
        get() = prefs.getBoolean("calm", false)
        set(v) = prefs.edit().putBoolean("calm", v).apply()

    /** The countdown's soft tick each second (never at night, never in the screensaver). */
    var tick: Boolean
        get() = prefs.getBoolean("tick", true)
        set(v) = prefs.edit().putBoolean("tick", v).apply()

    /** Dim at night: softer and calm from 11 pm to 7 am. */
    var dimNight: Boolean
        get() = prefs.getBoolean("dim", false)
        set(v) = prefs.edit().putBoolean("dim", v).apply()
}

object Options {
    class City(val name: String, val lat: Double, val lon: Double)

    val ROTATE_MINUTES = listOf(1, 5, 15)

    val CITIES = listOf(
        City("London", 51.5072, -0.1276),
        City("Birmingham", 52.4862, -1.8904),
        City("Manchester", 53.4808, -2.2426),
        City("Edinburgh", 55.9533, -3.1883),
        City("Delhi", 28.6139, 77.2090),
        City("Dubai", 25.2048, 55.2708),
    )

    /** The city called [name], or London. */
    fun city(name: String?): City = CITIES.firstOrNull { it.name == name } ?: CITIES[0]

    private val H24 = DateTimeFormatter.ofPattern("HH:mm")
    private val H12 = DateTimeFormatter.ofPattern("h:mm")

    /** The night hours for Dim at night: 11 pm to 7 am. */
    fun night(t: LocalTime): Boolean = t.hour >= 23 || t.hour < 7

    /** The clock's words: "21:05", or "9:05" with the 12-hour clock. */
    fun clock(t: LocalTime, h24: Boolean): String = t.format(if (h24) H24 else H12)
}
