package com.avanhq.doomsday

import android.content.Context
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The app's few settings: how often Rotate
 * changes picture, the clock's hours, the weather line and its town (found automatically or
 * chosen, [Geo]), the effects (full or calm) and dimming at night. The choices and the
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

    /** The weather's town: found from the internet connection (Automatic), or one chosen. */
    var cityAuto: Boolean
        get() = prefs.getBoolean("city_auto", true)
        set(v) = prefs.edit().putBoolean("city_auto", v).apply()

    var chosenCity: Options.City?
        get() = Options.decode(prefs.getString("city_chosen", null))
        set(v) = prefs.edit().putString("city_chosen", v?.let(Options::encode)).apply()

    /** The town Automatic last found, and when (found again once a day). */
    var foundCity: Options.City?
        get() = Options.decode(prefs.getString("city_found", null))
        set(v) = prefs.edit().putString("city_found", v?.let(Options::encode)).putLong("city_found_at", System.currentTimeMillis()).apply()

    val foundAt: Long get() = prefs.getLong("city_found_at", 0L)

    /** The town the weather is for now, as far as is known without asking the internet. */
    val city: Options.City
        get() = (if (cityAuto) foundCity else chosenCity) ?: chosenCity ?: Options.LONDON

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

    /** Until the town is found, or if it can't be. */
    val LONDON = City("London", 51.5072, -0.1276)

    /** A town saved as "name|lat|lon" (a name has no "|"). */
    fun encode(c: City): String = "${c.name.replace('|', ' ')}|${c.lat}|${c.lon}"

    fun decode(s: String?): City? = s?.split('|')?.takeIf { it.size == 3 }?.let { p ->
        val lat = p[1].toDoubleOrNull()
        val lon = p[2].toDoubleOrNull()
        if (p[0].isBlank() || lat == null || lon == null) null else City(p[0], lat, lon)
    }

    private val H24 = DateTimeFormatter.ofPattern("HH:mm")
    private val H12 = DateTimeFormatter.ofPattern("h:mm")

    /** The night hours for Dim at night: 11 pm to 7 am. */
    fun night(t: LocalTime): Boolean = t.hour >= 23 || t.hour < 7

    /** The clock's words: "21:05", or "9:05" with the 12-hour clock. */
    fun clock(t: LocalTime, h24: Boolean): String = t.format(if (h24) H24 else H12)
}
