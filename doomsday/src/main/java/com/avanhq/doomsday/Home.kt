package com.avanhq.doomsday

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Doomsday on the Home button. Two ways:
 *
 * - On a TV box that lets you choose the home app: the app offers itself as one (HomeAlias, on
 *   from the start; Settings → Home button can stop it), and Android asks which home to use.
 * - On Google TV, which has no such choice: [HomeWatcher], an accessibility service switched on
 *   in the TV's Accessibility settings, opens Doomsday when the TV's own home comes up. Fire TV
 *   updates have been blocking this kind of replacement, so there it may not work.
 *
 * Only one app should do this: if another app's home watcher is on (such as Avan), Doomsday's
 * stays quiet, so the two don't fight over the screen. The app never changes a TV setting.
 */
object Home {
    /** The TVs' own home screens (Google TV, older Android TV, Fire TV). */
    val HOMES = setOf(
        "com.google.android.apps.tv.launcherx",
        "com.google.android.tvlauncher",
        "com.google.android.leanbacklauncher",
        "com.amazon.tv.launcher",
    )

    /** Apps known to take over Home the same way, by their names. */
    private val OTHERS = mapOf(
        "com.avanhq.tv" to "Avan",
        "app.orbitlauncher" to "Orbit",
        "com.spocky.projengmenu" to "Projectivy",
        "flar2.homebutton" to "Button Mapper",
    )

    /** The other app already handling Home among the switched-on [enabled] services, or null. */
    fun other(enabled: String?, self: String): String? =
        services(enabled).map { it.substringBefore('/') }.firstOrNull { it != self && it in OTHERS }?.let { OTHERS[it] }

    /** Whether this app's own watcher is switched on. */
    fun mine(enabled: String?, self: String): Boolean = services(enabled).any { it.substringBefore('/') == self }

    private fun services(enabled: String?): List<String> = enabled.orEmpty().split(':').map { it.trim() }.filter { it.isNotEmpty() }

    fun enabled(context: Context): String? =
        runCatching { Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) }.getOrNull()

    private fun alias(context: Context) = ComponentName(context, "${context.packageName}.HomeAlias")

    /** Whether the app offers itself as a home app (on TV boxes that let you choose): on unless
     *  switched off in Settings (the manifest has it on). */
    fun offered(context: Context): Boolean =
        when (context.packageManager.getComponentEnabledSetting(alias(context))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> true
            else -> false
        }

    /** Whether to ask "Make Doomsday your home screen?" when it first opens: not if asked
     *  before, not if another app already handles Home (such as Avan), not if it does already. */
    fun shouldAsk(asked: Boolean, enabled: String?, self: String): Boolean =
        !asked && other(enabled, self) == null && !mine(enabled, self)

    /** Google TV (its own home app is installed): there Doomsday takes Home through Accessibility. */
    fun googleTv(context: Context): Boolean =
        runCatching { context.packageManager.getPackageInfo("com.google.android.apps.tv.launcherx", 0); true }.getOrDefault(false)

    /** Offer the app as a home app, or stop; only its own entry changes, nothing of the TV's. */
    fun offer(context: Context, on: Boolean) {
        val state = if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        context.packageManager.setComponentEnabledSetting(alias(context), state, PackageManager.DONT_KILL_APP)
    }
}

/** Opens Doomsday when the TV's own home screen comes up, unless another app's watcher is on. */
class HomeWatcher : AccessibilityService() {
    private var lastOpen = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() !in Home.HOMES) return
        if (Home.other(Home.enabled(this), packageName) != null) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastOpen < 1000) return
        lastOpen = now
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
    }

    override fun onInterrupt() = Unit
}
