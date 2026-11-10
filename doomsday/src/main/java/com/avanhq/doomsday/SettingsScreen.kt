package com.avanhq.doomsday

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.avanhq.tv.Fonts
import com.avanhq.tv.Ui

/**
 * Settings: the pictures, how often Rotate
 * changes, the clock, the weather and its town (Automatic or chosen: [CityScreen]), the effects,
 * dimming at night, the screensaver, the Home button ([Home]) and which version this is. Each
 * choice is a row of chips, the chosen one marked; it's saved as it's pressed.
 */
class SettingsScreen(
    context: Context,
    private val settings: Settings,
    private val pictures: Pictures,
    private val openPictures: () -> Unit,
    private val openPreview: () -> Unit,
    private val openCity: () -> Unit,
    private val openApps: () -> Unit,
    private val done: () -> Unit,
) : FrameLayout(context) {
    private val groups = ArrayList<() -> Unit>() // each row's "mark the chosen chip"
    private var first: View? = null
    private lateinit var cityNote: TextView
    private lateinit var appsNote: TextView
    private lateinit var homeNote: TextView
    private lateinit var offerChip: TextView

    init {
        setBackgroundColor(Color.rgb(5, 9, 7))
        addView(View(context).apply {
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 1300f
                setGradientCenter(.2f, 0f)
                colors = intArrayOf(Color.argb(90, 40, 120, 70), Color.TRANSPARENT)
            }
        }, LayoutParams(Ui.W, Ui.H))
        addView(Ui.text(context, "Settings", 56f, Ui.text).apply { typeface = Fonts.of(context, Fonts.LIGHT) }, Ui.at(96, 40, 900))
        addView(Ui.chip(context, "Done", 24f) { done() }.apply { background = glass() }, Ui.atRight(96, 48, 160, 64))

        val added = Pictures.SLOTS.indices.count { pictures.has(it) }
        section(0, "Pictures", "$added of ${Pictures.SLOTS.size} added (Hall, Mask, Tapestry, Throne)") {
            listOf(chip("Open Pictures") { openPictures() })
        }
        appsNote = section(1, "Apps", "") { listOf(chip("Order and hide") { openApps() }) }
        choice(2, "Rotate every", Options.ROTATE_MINUTES.map { "$it min" to it }, { settings.rotateMinutes }) { settings.rotateMinutes = it }
        choice(3, "Clock", listOf("24-hour" to true, "12-hour" to false), { settings.clock24 }) { settings.clock24 = it }
        choice(4, "Weather", listOf("On" to true, "Off" to false), { settings.weatherOn }) { settings.weatherOn = it }
        cityNote = section(5, "City", "") { listOf(chip("Choose a town") { openCity() }) }
        choice(6, "Effects", listOf("Full" to false, "Calm" to true), { settings.calm }, "Calm: light, mist and dust; no jumps or camera move") { settings.calm = it }
        choice(7, "Dim at night", listOf("Off" to false, "11 pm to 7 am" to true), { settings.dimNight }, "Softer and calm at night") { settings.dimNight = it }
        choice(8, "Ticking sound", listOf("On" to true, "Off" to false), { settings.tick }, "A soft tick each second; never 11 pm to 7 am") { settings.tick = it }
        if (Device.fireTv(context)) {
            section(9, "Screensaver", "Fire TV doesn't allow other screensavers") { listOf(chip("Preview") { openPreview() }) }
        } else {
            section(9, "Screensaver", "In the TV's settings: Screen saver → Doomsday") {
                listOf(chip("Open TV settings") { openDreamSettings() }, chip("Preview") { openPreview() })
            }
        }
        homeNote = section(10, "Home button", "") {
            listOf(
                chip("Open Accessibility") { openAccessibility() },
                chip("Offer as home app") { Home.offer(context, !Home.offered(context)); refresh() }.also { offerChip = it },
            )
        }
        refresh()
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        section(11, "About", "Doomsday ${version?.versionName ?: ""}, build ${version?.longVersionCode ?: 0}. Pictures only from your links, kept on this TV.") { emptyList() }
    }

    /** The town and the Home button as they are now (after the City screen, or Accessibility). */
    fun refresh() {
        val hidden = settings.hiddenApps.size
        appsNote.text = if (settings.appOrder.isEmpty() && hidden == 0) "The usual order, all shown" else "Your order" + if (hidden > 0) " · $hidden hidden" else ""
        cityNote.text = if (settings.cityAuto) "Automatic: ${settings.foundCity?.name ?: "found when the weather next loads"}" else "${settings.chosenCity?.name ?: Options.LONDON.name} (chosen)"
        val other = Home.other(Home.enabled(context), context.packageName)
        homeNote.text = when {
            other != null -> "$other already opens with Home on this TV, so Doomsday leaves it"
            Home.mine(Home.enabled(context), context.packageName) -> "On: Doomsday opens when Home is pressed"
            Device.fireTv(context) -> "Switch on Doomsday in Accessibility to try; Fire TV updates may block it"
            else -> "Switch on Doomsday in the TV's Accessibility settings"
        }
        offerChip.text = (if (Home.offered(context)) "●  " else "") + "Offer as home app"
    }

    /** The TV's Accessibility settings, where Doomsday's Home watcher is switched on (by you). */
    private fun openAccessibility() {
        val tries = listOf(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS, android.provider.Settings.ACTION_SETTINGS)
        for (a in tries) {
            if (runCatching { context.startActivity(android.content.Intent(a).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }

    /** The TV's own screensaver settings (the app can't pick itself; the TV asks you). */
    private fun openDreamSettings() {
        val tries = listOf(android.provider.Settings.ACTION_DREAM_SETTINGS, android.provider.Settings.ACTION_DISPLAY_SETTINGS, android.provider.Settings.ACTION_SETTINGS)
        for (a in tries) {
            if (runCatching { context.startActivity(android.content.Intent(a).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess) return
        }
    }

    /** A row: its name, its chips, and a short note after them (returned, to change later). */
    private fun section(n: Int, name: String, note: String?, chips: () -> List<TextView>): TextView {
        val y = 120 + n * 76 // twelve rows on one screen
        addView(Ui.text(context, name, 28f, Ui.text, bold = true), Ui.at(96, y + 12, 330))
        val row = Ui.row(context, 14).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        for (c in chips()) {
            row.addView(c, LinearLayout.LayoutParams(-2, 64))
            if (first == null) first = c
        }
        val noteView = Ui.text(context, note.orEmpty(), 21f, Ui.soft)
        if (note != null) row.addView(noteView, LinearLayout.LayoutParams(-2, -2).apply { marginStart = if (row.childCount > 0) 14 else 0 })
        addView(row, Ui.at(440, y, -2, 64))
        return noteView
    }

    private fun <T> choice(n: Int, name: String, options: List<Pair<String, T>>, now: () -> T, note: String? = null, pick: (T) -> Unit) {
        val made = ArrayList<Pair<TextView, Pair<String, T>>>()
        val mark = { for ((c, o) in made) c.text = (if (now() == o.second) "●  " else "") + o.first }
        section(n, name, note) {
            options.map { o -> chip(o.first) { pick(o.second); groups.forEach { it() } }.also { made += it to o } }
        }
        groups += mark
        mark()
    }

    private fun chip(label: String, onClick: () -> Unit): TextView = Ui.chip(context, label, 24f, onClick).apply { background = glass() }

    /** Where the remote starts. */
    fun focusFirst() { first?.requestFocus() }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) {
            done()
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun glass() = Ui.focusable(40f, Color.argb(90, 4, 14, 9), Color.argb(190, 10, 38, 24))
}
