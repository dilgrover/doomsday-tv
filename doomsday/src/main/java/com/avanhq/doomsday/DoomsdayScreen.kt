package com.avanhq.doomsday

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.avanhq.tv.Countdown
import com.avanhq.tv.DoomBolt
import com.avanhq.tv.DoomCounter
import com.avanhq.tv.DoomTick
import com.avanhq.tv.DoomFx
import com.avanhq.tv.DoomLight
import com.avanhq.tv.DoomStage
import com.avanhq.tv.Fonts
import com.avanhq.tv.FramePace
import com.avanhq.tv.Ui
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * The countdown, on its own: the
 * rolling days, hours, minutes and seconds to Avengers: Doomsday, the clock, and the four
 * pictures brought to life (DoomStage): Hall, Mask, Tapestry and Throne, or each in turn
 * (Rotate: the ones there are, a new one each minute). A picture not added yet shows a hall drawn in code and a card pointing to Pictures.
 * The fire leaps to what the remote selects; holding OK for 2 seconds summons Doom. The weather
 * sits under the clock, and the TV's apps wait out of sight along the bottom until the remote
 * goes down to them. Settings holds the few choices (SettingsScreen).
 */
class DoomsdayScreen(
    context: Context,
    private val pictures: Pictures,
    private val settings: Settings,
    private val openPictures: (Int) -> Unit,
    private val openSettings: () -> Unit,
    /** The screensaver (DoomDream): the countdown and the pictures only, nothing to select. */
    private val ambient: Boolean = false,
) : FrameLayout(context) {
    private companion object {
        const val SHORT_MS = 600L
        const val ROTATE = 4
        const val COUNTER_X = 96
        const val COUNTER_Y = 270
        const val CHIPS_Y = 976 // one row along the bottom
        private val DAY = DateTimeFormatter.ofPattern("EEEE d MMMM")
    }

    private val prefs = context.getSharedPreferences("screen", Context.MODE_PRIVATE)
    private val ticker = Handler(Looper.getMainLooper())
    private val stage = DoomStage(context, List(Pictures.SLOTS.size) { DoomFx.HALL })
    private val glow = DoomLight(context, stage)
    private val bolt = DoomBolt(context, stage)
    private val hall: Bitmap by lazy { DoomFx.hall() }
    private val real = BooleanArray(Pictures.SLOTS.size) // a picture of the user's, not the drawn hall
    private var slot = -1
    private var slotAt = 0L
    private var openedAt = 0L

    private val counter = DoomCounter(context)
    private var tick: DoomTick? = null // made while it's on screen, released after
    private val clock = Ui.text(context, "", 84f, Ui.text, gravity = Gravity.END).apply { typeface = Fonts.of(context, Fonts.LIGHT) }
    // Today's date on top, the film's day under it.
    private val title = Ui.text(context, "", 66f, Ui.text).apply { typeface = Fonts.of(context, Fonts.LIGHT) }
    private val chips = ArrayList<TextView>()
    private val rotate: TextView = Ui.chip(context, "Rotate", 24f) { choose(ROTATE) }.apply { background = glass() }
    private val more: TextView = Ui.chip(context, "Pictures", 24f) { openPictures(current()) }.apply { background = glass() }
    private val play: TextView = Ui.chip(context, "▶  Trailer", 24f) { openTrailer() }.apply { background = glass() }
    private val gear: TextView = Ui.chip(context, "Settings", 24f) { openSettings() }.apply { background = glass() }
    private val outside = Ui.text(context, "", 26f, Ui.soft, gravity = Gravity.END)
    private val weather = Weather { line -> outside.text = line.orEmpty(); outside.visibility = if (line == null) View.GONE else View.VISIBLE }
    private val apps = AppsRow(context)
    private val opts = Ui.row(context, 14)
    private val night = View(context).apply { setBackgroundColor(Color.BLACK); alpha = 0f; isFocusable = false }

    // The card for a picture not added yet.
    private val cardTitle = Ui.text(context, "", 32f, Ui.text, bold = true)
    private val cardText = Ui.text(context, "", 24f, Ui.soft, lines = 3)
    private val card = Ui.column(context, 10).apply {
        background = Ui.panel(26f, Color.argb(200, 6, 14, 10), Color.argb(70, 240, 192, 96))
        setPadding(32, 26, 32, 28)
        addView(cardTitle)
        addView(cardText)
        addView(Ui.chip(context, "Add a link", 24f) { openPictures(current()) }.apply { background = glass() }, LinearLayout.LayoutParams(-2, 64).apply { topMargin = 8 })
        visibility = View.GONE
    }

    // A move of the remote: the fire leaps to what's selected (not on the first focus as it opens).
    private val focusWatch = ViewTreeObserver.OnGlobalFocusChangeListener { _, now ->
        if (now == null || visibility != View.VISIBLE) return@OnGlobalFocusChangeListener
        apps.animate().alpha(if (apps.holds(now)) 1f else 0f).setDuration(350).start()
        if (!stage.calm && System.currentTimeMillis() - openedAt > 900) {
            stage.surge()
            bolt.fire(now)
        }
    }

    init {
        clipChildren = false
        addView(stage, LayoutParams(Ui.W, Ui.H))
        addView(glow, LayoutParams(Ui.W / DoomLight.SCALE, Ui.H / DoomLight.SCALE)) // its light, small, added on top
        addView(View(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.argb(200, 3, 5, 4), Color.argb(120, 3, 5, 4), Color.TRANSPARENT, Color.TRANSPARENT))
        }, LayoutParams(Ui.W, Ui.H))
        // A soft shade behind the clock, for pictures bright up there.
        addView(View(context).apply {
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = 620f
                setGradientCenter(1f, 0f)
                colors = intArrayOf(Color.argb(200, 3, 5, 4), Color.argb(110, 3, 5, 4), Color.TRANSPARENT)
            }
        }, LayoutParams(900, 720).apply { leftMargin = Ui.W - 900 })
        addView(title, Ui.at(96, 52, 1300))
        addView(Ui.text(context, "${Countdown.TITLE}  ·  ${Countdown.WHEN}", 28f, Ui.soft), Ui.at(98, 136, 1200))
        // A shade along the bottom for the buttons.
        addView(View(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(Color.argb(170, 3, 5, 4), Color.argb(80, 3, 5, 4), Color.TRANSPARENT))
        }, LayoutParams(Ui.W, 240).apply { topMargin = Ui.H - 240 })
        addView(clock, Ui.atRight(96, 40, 400))
        addView(outside, Ui.atRight(96, 140, 700))
        // The countdown in the film's own style, left of Doom (DoomCounter).
        addView(counter, Ui.at(COUNTER_X, COUNTER_Y, DoomCounter.W, DoomCounter.H))
        // The four pictures, Rotate, then Pictures (the links) and the trailer.
        for ((i, s) in Pictures.SLOTS.withIndex()) {
            val c = Ui.chip(context, s.name, 22f) { choose(i) }.apply { background = glass() }
            chips += c
            opts.addView(c, LinearLayout.LayoutParams(-2, 60))
        }
        for (c in listOf(rotate, more, gear, play)) opts.addView(c, LinearLayout.LayoutParams(-2, 60))
        addView(opts, Ui.at(96, CHIPS_Y))
        addView(card, Ui.at(96, 540, 820))
        // The TV's apps, above the buttons and out of sight until the remote goes up to them.
        addView(apps, Ui.at(0, CHIPS_Y - AppsRow.HEIGHT - 22, Ui.W, AppsRow.HEIGHT))
        addView(bolt, LayoutParams(Ui.W, Ui.H)) // on top: the fire reaches what's selected
        bolt.avoid = counter.area(COUNTER_X, COUNTER_Y) // round the countdown, not through it
        addView(night, LayoutParams(Ui.W, Ui.H)) // Dim at night: a soft dark veil over it all
        if (ambient) for (v in listOf(opts, card, apps, bolt)) v.visibility = View.GONE
        mark()
        tock()
    }

    /** The slot on screen now (Rotate: the one it's showing). */
    private fun current() = slot.coerceAtLeast(0)

    private fun choice() = prefs.getInt("slot", 3) // Throne first

    private fun choose(i: Int) {
        prefs.edit().putInt("slot", i).apply()
        mark()
        show(if (i == ROTATE) next() else i)
    }

    /** The next picture for Rotate: the next one added (the drawn hall only if none is). */
    private fun next(): Int {
        val n = Pictures.SLOTS.size
        for (k in 1..n) { val i = (current() + k) % n; if (real[i]) return i }
        return current()
    }

    private fun mark() {
        val on = choice()
        for ((i, c) in chips.withIndex()) c.text = (if (on == i) "●  " else "") + Pictures.SLOTS[i].name
        rotate.text = (if (on == ROTATE) "●  " else "") + "Rotate"
    }

    private fun show(i: Int) {
        if (i != slot) {
            slot = i
            slotAt = System.currentTimeMillis()
            stage.show(i)
        }
        val name = Pictures.SLOTS[i].name
        card.visibility = if (real[i] || ambient) View.GONE else View.VISIBLE
        cardTitle.text = "No picture yet for $name"
        cardText.text = "Add a link in Pictures: the TV downloads it when you press Download, and it comes to life here."
    }

    /** Load every slot's picture (the user's, or the drawn hall), off the main thread. */
    fun reload() {
        Thread {
            val got = Pictures.SLOTS.indices.map { pictures.load(it) }
            post {
                for ((i, p) in got.withIndex()) {
                    real[i] = p != null
                    if (p != null) stage.picture(i, p.first, p.second) else stage.picture(i, hall, DoomFx.HALL)
                }
                val c = choice()
                slot = -1
                show(if (c == ROTATE) (if (real.any { it }) real.indexOfFirst { it } else 3) else c)
            }
        }.start()
    }

    // Hold OK to summon (DoomStage.charge, blast): a short press still presses what's selected.
    private var holdFrom = 0L
    private var summoned = false

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val ok = e.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || e.keyCode == KeyEvent.KEYCODE_ENTER ||
            e.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || e.keyCode == KeyEvent.KEYCODE_BUTTON_A
        val f = findFocus()
        if (!ok || f == null) return super.dispatchKeyEvent(e)
        val t = System.currentTimeMillis()
        if (e.action == KeyEvent.ACTION_DOWN) {
            if (e.repeatCount == 0) { holdFrom = t; summoned = false; stage.charge(true) }
            else if (!summoned && t - holdFrom >= DoomStage.HOLD_MS) { summoned = true; goOff() }
            return true
        }
        if (e.action == KeyEvent.ACTION_UP) {
            stage.charge(false)
            if (!summoned && t - holdFrom < SHORT_MS) f.performClick()
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    /** The summoning without the remote (`--ez summon true`, for recordings). */
    fun summon() {
        stage.charge(true)
        ticker.postDelayed({ goOff() }, DoomStage.HOLD_MS)
    }

    private fun goOff() {
        stage.blast()
        // As it goes off, the countdown spins and lands back on the time left.
        ticker.postDelayed({
            counter.scramble()
            ticker.postDelayed({ tock() }, 450)
        }, DoomStage.IGNITE_MS)
    }

    /** The chips as dark green glass, so the picture shows through. */
    private fun glass() = Ui.focusable(40f, Color.argb(90, 4, 14, 9), Color.argb(190, 10, 38, 24))

    private val second = object : Runnable {
        override fun run() {
            tock()
            if (choice() == ROTATE && System.currentTimeMillis() - slotAt > settings.rotateMinutes * 60_000L) show(next())
            ticker.postDelayed(this, 1_000 - System.currentTimeMillis() % 1_000)
        }
    }

    private fun tock() {
        counter.tick()
        title.text = LocalDate.now().format(DAY)
        val now = LocalTime.now()
        // The soft tick: only while it's on screen and in front, never at night or as the screensaver.
        if (settings.tick && !ambient && isShown && hasWindowFocus() && !Countdown.quiet(now.hour, 23, 7) && Countdown.left().any { it != 0L }) tick?.tick()
        clock.text = Options.clock(now, settings.clock24)
        // Effects and night: calm when chosen, and softer and calm at night if Dim at night is on.
        val dark = settings.dimNight && Options.night(now)
        stage.calm = settings.calm || dark
        night.alpha = if (dark) .35f else 0f
    }

    private fun openTrailer() {
        val app = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${Countdown.VIDEO}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${Countdown.VIDEO}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(app) }.recoverCatching { context.startActivity(web) }
    }

    /** Where the remote starts: the chosen picture's chip. */
    fun focusDefault() {
        val c = choice()
        (if (c == ROTATE) rotate else chips[c]).requestFocus()
    }

    /** Its look back on after another screen (the screensaver preview) let it go. */
    fun resumeLook() {
        FramePace.on = true
        Ui.ringColour(Countdown.GREEN)
    }

    /** Back from Settings: its choices take effect (clock, weather, effects, apps). */
    fun applySettings() {
        resumeLook()
        tock()
        weather.start(if (settings.weatherOn) settings.city else null)
        apps.refresh()
        gear.requestFocus()
    }

    /** Back from Pictures to slot [i]: show it, focus its chip. */
    fun backTo(i: Int) {
        reload()
        chips.getOrNull(i)?.requestFocus()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        openedAt = System.currentTimeMillis()
        if (!ambient) tick = DoomTick()
        FramePace.on = true
        Ui.ringColour(Countdown.GREEN)
        viewTreeObserver.addOnGlobalFocusChangeListener(focusWatch)
        ticker.post(second)
        stage.open()
        reload()
        weather.start(if (settings.weatherOn) settings.city else null)
    }

    override fun onDetachedFromWindow() {
        viewTreeObserver.removeOnGlobalFocusChangeListener(focusWatch)
        FramePace.on = false
        ticker.removeCallbacks(second)
        weather.stop()
        tick?.release()
        tick = null
        stage.release()
        super.onDetachedFromWindow()
    }
}
