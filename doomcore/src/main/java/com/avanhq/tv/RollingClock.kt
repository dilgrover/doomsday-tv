package com.avanhq.tv

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * The home screens' clock with rolling digits: when the time changes,
 * each digit that changes rolls up and out as the new one rolls in from below. One TextView
 * per character, each in its own clipped box, made by [make] so it keeps the screen's font.
 */
class RollingClock(context: Context, private val make: () -> TextView, align: Int = Gravity.START) : LinearLayout(context) {
    private var shown = ""

    var text: CharSequence
        get() = shown
        set(value) = show(value.toString())

    init {
        orientation = HORIZONTAL
        gravity = align or Gravity.CENTER_VERTICAL
        clipChildren = true
    }

    private fun show(value: String) {
        if (value == shown) return
        val rebuild = childCount != value.length
        if (rebuild) {
            removeAllViews()
            for (ch in value) {
                val box = FrameLayout(context).apply { clipChildren = true; clipToPadding = true }
                box.addView(make().apply { text = ch.toString() }, FrameLayout.LayoutParams(-2, -2))
                addView(box, LayoutParams(-2, -2))
            }
            shown = value
            return
        }
        for ((i, ch) in value.withIndex()) {
            if (shown.getOrNull(i) == ch) continue
            val box = getChildAt(i) as FrameLayout
            // A roll still going (a new value came within 420 ms, as the Doomsday summoning's
            // spin does): finish it at once, so no digit is left half way (10 Nov, seen on TV).
            val roll = ((box.tag as? Int) ?: 0) + 1
            box.tag = roll
            while (box.childCount > 1) box.removeViewAt(0)
            val old = box.getChildAt(0) as TextView
            old.animate().cancel()
            old.translationY = 0f
            old.alpha = 1f
            if (Ui.still || !isAttachedToWindow || box.height == 0) {
                old.text = ch.toString()
                continue
            }
            val h = box.height.toFloat()
            val next = make().apply { text = ch.toString(); translationY = h }
            box.addView(next, FrameLayout.LayoutParams(-2, -2))
            if (FramePace.on) {
                // On a paced screen the roll keeps the screen's own frames (FramePace).
                FramePace.animate(420, { f ->
                    if (box.tag != roll) return@animate
                    val e = FramePace.ease(f)
                    old.translationY = -h * e; old.alpha = 1f - e; next.translationY = h * (1f - e)
                }) { if (box.tag == roll) box.removeView(old) }
                continue
            }
            old.animate().translationY(-h).alpha(0f).setDuration(420).withEndAction { if (box.tag == roll) box.removeView(old) }.start()
            next.animate().translationY(0f).setDuration(420).start()
        }
        shown = value
    }
}
