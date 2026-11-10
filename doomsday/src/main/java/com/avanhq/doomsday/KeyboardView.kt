package com.avanhq.doomsday

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import android.view.View
import com.avanhq.tv.Countdown
import com.avanhq.tv.Fonts
import com.avanhq.tv.Ui

/**
 * The on-screen keyboard for a picture's link ([Keys]): it slides up over the bottom of
 * Pictures, shows what's typed so far, and takes the remote's arrows and OK itself, so typing
 * doesn't depend on the TV having a keyboard of its own. A long press on ⌫ clears the link;
 * Back closes it and keeps what was typed (the Pictures screen sends Back here).
 */
class KeyboardView(context: Context) : View(context) {
    companion object {
        const val WIDTH = 1728
        const val HEIGHT = 560
        private const val TOP = 116
        private const val KEY_H = 76f
        private const val GAP = 12f
    }

    var changed: (String) -> Unit = {}
    var finished: (String) -> Unit = {}
    var closed: (String) -> Unit = {}

    private var keys = Keys()
    private var title = ""
    private var held = false

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val box = RectF()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        visibility = GONE
    }

    /** Open for the link of [name], starting from [typed]. */
    fun open(name: String, typed: String) {
        keys = Keys(typed)
        title = "$name link"
        visibility = VISIBLE
        translationY = HEIGHT.toFloat()
        animate().translationY(0f).setDuration(if (Ui.still) 0 else 320).setInterpolator(Ui.EASE).start()
        requestFocus()
        invalidate()
    }

    fun close() {
        animate().translationY(HEIGHT.toFloat()).setDuration(if (Ui.still) 0 else 240).withEndAction { visibility = GONE }.start()
    }

    val open: Boolean get() = visibility == VISIBLE

    /** Back: close, keeping the link. */
    fun back() { close(); closed(keys.text) }

    private fun ok(code: Int) = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER ||
        code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A

    override fun onKeyDown(code: Int, e: KeyEvent): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> keys.left()
            KeyEvent.KEYCODE_DPAD_RIGHT -> keys.right()
            KeyEvent.KEYCODE_DPAD_UP -> keys.up()
            KeyEvent.KEYCODE_DPAD_DOWN -> keys.down()
            else -> {
                if (!ok(code)) return super.onKeyDown(code, e)
                // Held on ⌫: the whole link goes.
                if (e.repeatCount > 0 && !held && keys.key == Keys.DELETE) { held = true; keys.clear(); changed(keys.text) }
            }
        }
        invalidate()
        return true
    }

    override fun onKeyUp(code: Int, e: KeyEvent): Boolean {
        if (!ok(code)) return super.onKeyUp(code, e)
        if (held) { held = false; return true }
        if (keys.press()) { close(); finished(keys.text) } else changed(keys.text)
        invalidate()
        return true
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        // Near-solid dark glass, a fine gold edge: the links behind mustn't show through.
        box.set(0f, 0f, w, height.toFloat())
        fill.color = Color.argb(246, 6, 12, 9)
        c.drawRoundRect(box, 28f, 28f, fill)
        line.strokeWidth = 2f
        line.color = Color.argb(150, 240, 200, 120)
        c.drawRoundRect(box, 28f, 28f, line)

        // What's typed so far, with a cursor.
        text.textAlign = Paint.Align.LEFT
        text.typeface = Fonts.of(context, Fonts.REGULAR)
        text.textSize = 22f
        text.color = Ui.soft
        c.drawText(title, 36f, 46f, text)
        text.textSize = 34f
        text.color = Color.WHITE
        val shown = keys.text.ifEmpty { " " }
        val tw = text.measureText(shown)
        val max = w - 72f
        val x0 = if (tw > max) 36f + max - tw else 36f
        c.save()
        c.clipRect(36f, 54f, w - 36f, 104f)
        c.drawText(shown, x0, 92f, text)
        fill.color = Countdown.GREEN
        c.drawRect(x0 + text.measureText(keys.text) + 4f, 62f, x0 + text.measureText(keys.text) + 7f, 98f, fill)
        c.restore()

        // The keys.
        val unit = (w - 72f - GAP * (Keys.UNITS - 1)) / Keys.UNITS
        text.textAlign = Paint.Align.CENTER
        for ((r, row) in Keys.ROWS.withIndex()) {
            val y = TOP + r * (KEY_H + GAP)
            for ((col, key) in row.withIndex()) {
                val x = 36f + keys.start(r, col) * (unit + GAP)
                box.set(x, y, x + key.second * unit + (key.second - 1) * GAP, y + KEY_H)
                val on = r == keys.row && col == keys.col
                fill.color = if (on) Color.rgb(16, 52, 32) else Color.rgb(18, 28, 23)
                c.drawRoundRect(box, 16f, 16f, fill)
                line.strokeWidth = if (on) 4f else 1.5f
                line.color = if (on) Countdown.GREEN else Color.argb(40, 255, 255, 255)
                c.drawRoundRect(box, 16f, 16f, line)
                text.typeface = Fonts.of(context, if (key.first.length > 1) Fonts.BOLD else Fonts.REGULAR)
                text.textSize = if (key.first.length > 1) 24f else 32f
                text.color = if (key.first == Keys.DONE) Countdown.GREEN else Color.WHITE
                c.drawText(key.first, box.centerX(), box.centerY() - (text.descent() + text.ascent()) / 2, text)
            }
        }
    }
}
