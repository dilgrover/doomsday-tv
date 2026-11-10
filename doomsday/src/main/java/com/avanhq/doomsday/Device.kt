package com.avanhq.doomsday

import android.content.Context
import android.os.Build

/**
 * What kind of TV the app is on. An Amazon Fire TV stick lists many apps only as phone-style
 * launchers and doesn't let other apps be its screensaver.
 */
object Device {
    fun fireTv(context: Context): Boolean =
        Build.MANUFACTURER.equals("Amazon", ignoreCase = true) ||
            context.packageManager.hasSystemFeature("amazon.hardware.fire_tv")

    /** The apps for the row: the TV apps, and on a Fire TV (or a TV that lists none) the apps
     *  that only have a phone-style launcher too. Packages, in order, no repeats, not this app. */
    fun appsToShow(tvApps: List<String>, launcherApps: List<String>, fire: Boolean, self: String): List<String> {
        val all = if (fire || tvApps.isEmpty()) tvApps + launcherApps else tvApps
        return all.filter { it != self }.distinct()
    }
}
