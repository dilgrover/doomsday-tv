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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * The weather's town: Automatic, found from the internet connection, or any town searched
 * by name on the app's own keyboard ([Keys.CITY_ROWS]) and picked from what Open-Meteo finds
 * ([Geo]). Same-named towns show their region and country.
 */
class CityScreen(context: Context, private val settings: Settings, private val done: () -> Unit) : FrameLayout(context) {
    private val keyboard = KeyboardView(context)
    private val veil = View(context).apply { setBackgroundColor(Color.argb(120, 0, 0, 0)); visibility = View.GONE; isFocusable = false }
    private val auto: TextView
    private val search: TextView
    private val now = Ui.text(context, "", 22f, Ui.soft)
    private val status = Ui.text(context, "", 24f, Ui.soft)
    private val list = Ui.column(context, 12)
    private var typed = ""
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).build()

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
        addView(Ui.text(context, "City", 56f, Ui.text).apply { typeface = Fonts.of(context, Fonts.LIGHT) }, Ui.at(96, 40, 900))
        addView(Ui.text(context, "The town for the weather. Automatic finds it from your internet connection, or search for any town.", 24f, Ui.soft, lines = 2), Ui.at(98, 116, 1400))
        addView(Ui.chip(context, "Done", 24f) { done() }.apply { background = glass() }, Ui.atRight(96, 48, 160, 64))

        val row = Ui.row(context, 14).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        auto = chip("Automatic") { settings.cityAuto = true; mark(); done() }
        search = chip("Search for a town") { type() }
        row.addView(auto, LinearLayout.LayoutParams(-2, 64))
        row.addView(search, LinearLayout.LayoutParams(-2, 64))
        row.addView(now, LinearLayout.LayoutParams(-2, -2).apply { marginStart = 14 })
        addView(row, Ui.at(96, 200, -2, 64))
        addView(status, Ui.at(98, 300, 1700))
        addView(list, Ui.at(96, 350, 1300))
        addView(Ui.text(context, "Only ipwho.is or ipapi.co (to find your town) and Open-Meteo (the weather and the town search) are asked. No account.", 20f, Ui.dim, lines = 2), Ui.at(98, 1010, 1700))
        addView(veil, LayoutParams(Ui.W, Ui.H))
        addView(keyboard, Ui.at(96, Ui.H - KeyboardView.HEIGHT - 24, KeyboardView.WIDTH, KeyboardView.HEIGHT))
        mark()
    }

    /** The chosen way marked, and the town it gives now. */
    private fun mark() {
        auto.text = (if (settings.cityAuto) "●  " else "") + "Automatic"
        now.text = if (settings.cityAuto) "Now: ${settings.foundCity?.name ?: "finding it…"}" else "Now: ${settings.chosenCity?.name ?: Options.LONDON.name}"
    }

    /** The keyboard, for a town's name; Done searches for it. */
    private fun type() {
        keyboard.changed = { typed = it; status.text = it }
        keyboard.finished = { veil.visibility = View.GONE; typed = it; find(it) }
        keyboard.closed = { veil.visibility = View.GONE; typed = it; search.requestFocus() }
        veil.visibility = View.VISIBLE
        keyboard.open("Town", typed, label = "Town's name", rows = Keys.CITY_ROWS)
    }

    /** Search for [name] and list what's found. */
    private fun find(name: String) {
        list.removeAllViews()
        if (name.isBlank()) { status.text = "Type a town's name first."; search.requestFocus(); return }
        status.setTextColor(Ui.soft)
        status.text = "Looking for “${name.trim()}”…"
        Thread {
            val got = runCatching {
                client.newCall(Request.Builder().url(Geo.searchUrl(name)).header("User-Agent", "Doomsday-TV").build()).execute().use { r ->
                    if (r.isSuccessful) Geo.results(r.body?.string().orEmpty()) else null
                }
            }.getOrNull()
            post { show(name.trim(), got) }
        }.start()
    }

    /** The towns found for [name] (null: the search couldn't be made). */
    fun show(name: String, found: List<Geo.Found>?) {
        list.removeAllViews()
        when {
            found == null -> { status.setTextColor(Color.rgb(255, 150, 120)); status.text = "Couldn't search: is the TV online?" }
            found.isEmpty() -> { status.setTextColor(Ui.soft); status.text = "No town called “$name”. Try another spelling." }
            else -> {
                status.setTextColor(Ui.soft)
                status.text = "Pick “$name”:"
                for (f in found) list.addView(chip(f.label) { settings.chosenCity = f.city; settings.cityAuto = false; done() }, LinearLayout.LayoutParams(-2, 60))
            }
        }
        (list.getChildAt(0) ?: search).requestFocus()
    }

    private fun chip(label: String, onClick: () -> Unit): TextView = Ui.chip(context, label, 24f, onClick).apply { background = glass() }

    /** Where the remote starts. */
    fun focusFirst() { (if (settings.cityAuto) search else auto).requestFocus() }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK) {
            if (e.action == KeyEvent.ACTION_UP) { if (keyboard.open) keyboard.back() else done() }
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun glass() = Ui.focusable(40f, Color.argb(90, 4, 14, 9), Color.argb(190, 10, 38, 24))
}
