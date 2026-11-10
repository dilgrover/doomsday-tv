package com.avanhq.tv

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * The countdown in the film's own style:
 * "DOOMSDAY IS COMING" over one line of numbers, 01 : 05 : 10 : 22 : 36, with MONTH, DAYS,
 * HOURS, MINUTES and SECONDS under each pair. Kept to the space the tiles took, left of Doom
 *, on a soft dark shade with a gold hairline above and below,
 * and the seconds glow green on each tick. The digits roll as the clock's do. From release day
 * it reads "Avengers: Doomsday is here" and "In cinemas now". Shared by the launcher's
 * Doomsday screen and the Doomsday app.
 */
class DoomCounter(context: Context) : FrameLayout(context) {
    companion object {
        const val W = 820
        private const val PAIR = 136 // one pair of digits
        private const val GAP = 32 // a colon
        const val H = 236
        private val GOLD = Color.rgb(240, 200, 120)
        private val LABELS = listOf("Month", "Days", "Hours", "Minutes", "Seconds")
    }

    private val heading = Ui.text(context, "DOOMSDAY IS COMING", 30f, Color.rgb(246, 236, 210), bold = true, spacing = 0.22f, gravity = Gravity.CENTER)
    private val pairs = Array(5) {
        RollingClock(context, {
            Ui.text(context, "", 80f, Ui.text, gravity = Gravity.CENTER).apply {
                typeface = Fonts.of(context, Fonts.REGULAR)
                fontFeatureSettings = "tnum" // every digit the same width: the line doesn't jiggle
                letterSpacing = 0.04f
            }
        }, Gravity.CENTER)
    }
    private val line = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
    private val labels = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
    private val here = Ui.text(context, "In cinemas now", 64f, Ui.text, gravity = Gravity.CENTER).apply {
        typeface = Fonts.of(context, Fonts.LIGHT)
        visibility = View.GONE
    }
    private val glow = View(context).apply {
        background = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = 90f
            colors = intArrayOf(Color.argb(150, 96, 236, 150), Color.TRANSPARENT)
        }
        alpha = 0f
    }

    init {
        clipChildren = false
        // A soft dark shade so the numbers read over any picture (no live blur on the TV).
        background = GradientDrawable().apply {
            cornerRadius = 30f
            colors = intArrayOf(Color.argb(150, 3, 6, 5), Color.argb(110, 3, 6, 5))
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
        }
        addView(hairline(), LayoutParams(W - 120, 2).apply { leftMargin = 60; topMargin = 18 })
        addView(heading, LayoutParams(W, 44).apply { topMargin = 32 })
        // The green pulse sits behind the seconds.
        val side = (W - 5 * PAIR - 4 * GAP) / 2
        addView(glow, LayoutParams(220, 150).apply { leftMargin = side + 4 * (PAIR + GAP) + PAIR / 2 - 110; topMargin = 60 })
        for ((i, p) in pairs.withIndex()) {
            if (i > 0) line.addView(Ui.text(context, ":", 64f, Color.argb(200, 240, 200, 120), gravity = Gravity.CENTER).apply {
                typeface = Fonts.of(context, Fonts.LIGHT)
            }, LinearLayout.LayoutParams(GAP, -2))
            line.addView(p, LinearLayout.LayoutParams(PAIR, -2))
        }
        addView(line, LayoutParams(W, 112).apply { topMargin = 78 })
        addView(here, LayoutParams(W, 112).apply { topMargin = 82 })
        for ((i, word) in LABELS.withIndex()) {
            if (i > 0) labels.addView(View(context), LinearLayout.LayoutParams(GAP, 1))
            labels.addView(Ui.text(context, word.uppercase(), 16f, Ui.soft, bold = true, spacing = 0.14f, gravity = Gravity.CENTER), LinearLayout.LayoutParams(PAIR, -2))
        }
        addView(labels, LayoutParams(W, 26).apply { topMargin = 188 })
        addView(hairline(), LayoutParams(W - 120, 2).apply { leftMargin = 60; topMargin = 222 })
        show(Countdown.parts())
    }

    private fun hairline() = View(context).apply {
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.TRANSPARENT, GOLD, Color.TRANSPARENT))
        alpha = .55f
    }

    /** The time left now: rolls the digits that changed and glows the seconds. */
    fun tick() {
        val parts = Countdown.parts()
        show(parts)
        if (!Ui.still && parts.any { it != 0L }) {
            glow.animate().cancel()
            glow.alpha = .9f
            glow.animate().alpha(0f).setDuration(800).start()
        }
    }

    private fun show(parts: LongArray) {
        val out = parts.all { it == 0L }
        heading.text = if (out) "${Countdown.TITLE.uppercase()} IS HERE" else "DOOMSDAY IS COMING"
        line.visibility = if (out) View.INVISIBLE else View.VISIBLE
        labels.visibility = if (out) View.INVISIBLE else View.VISIBLE
        here.visibility = if (out) View.VISIBLE else View.GONE
        for (i in 0..4) pairs[i].text = parts[i].toString().padStart(2, '0')
    }

    /** The summoning's spin: every digit somewhere else for a moment (the next tick lands it). */
    fun scramble() {
        for (p in pairs) p.text = p.text.map { '0' + kotlin.random.Random.nextInt(10) }.joinToString("")
    }

    /** Where it sits for DoomBolt.avoid, given its top-left on the 1920 x 1080 screen. */
    fun area(x: Int, y: Int) = android.graphics.RectF(x - 16f, y - 16f, x + W + 16f, y + H + 16f)
}
