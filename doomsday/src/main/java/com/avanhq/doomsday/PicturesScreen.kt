package com.avanhq.doomsday

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.avanhq.tv.Fonts
import com.avanhq.tv.Ui

/**
 * Pictures: a link for each of the four (Hall, Mask, Tapestry, Throne), typed on the app's own
 * keyboard ([KeyboardView]: OK on a link opens it; the TV may have none, D 10 Nov). The link is
 * a plain box, not a text field: a text field brought up the TV's own "use the keyboard on your
 * phone" bar over the keyboard. A link starts as
 * "https://tinyurl.com/" so only the short code is typed; the chips above it switch
 * to bit.ly or a full link, and a bare code counts as a tinyurl one. Nothing is fetched until
 * Download is pressed; the line under each link says where it came from, or what went wrong.
 */
class PicturesScreen(context: Context, private val pictures: Pictures, private val done: (Int) -> Unit) : FrameLayout(context) {
    private class Row(val field: TextView, val status: TextView, val thumb: ImageView, val download: TextView)

    private val rows = ArrayList<Row>()
    private var last = 0
    private val keyboard = KeyboardView(context)
    private val veil = View(context).apply { setBackgroundColor(Color.argb(120, 0, 0, 0)); visibility = View.GONE; isFocusable = false }

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
        addView(Ui.text(context, "Pictures", 56f, Ui.text).apply { typeface = Fonts.of(context, Fonts.LIGHT) }, Ui.at(96, 40, 900))
        addView(Ui.text(context, "Select a link and press OK to type it. Nothing downloads until you press Download; the pictures stay on this TV.", 24f, Ui.soft, lines = 2), Ui.at(98, 116, 1300))
        addView(Ui.chip(context, "Done", 24f) { done(last) }.apply { background = glass() }, Ui.atRight(96, 48, 160, 64))

        for ((i, s) in Pictures.SLOTS.withIndex()) {
            val y = 230 + i * 200
            addView(Ui.text(context, s.name, 28f, Ui.text, bold = true), Ui.at(96, y + 6, 260))
            val field = TextView(context).apply {
                setTextColor(Color.WHITE)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 26f)
                typeface = Fonts.of(context, Fonts.REGULAR)
                isSingleLine = true
                ellipsize = android.text.TextUtils.TruncateAt.START
                background = Ui.focusable(16f, Color.argb(160, 10, 18, 14), Color.argb(220, 14, 34, 24))
                setPadding(22, 0, 22, 0)
                gravity = Gravity.CENTER_VERTICAL
                isFocusable = true
                isClickable = true
                text = pictures.link(i).ifEmpty { Links.TINY }
                setOnFocusChangeListener { _, has -> if (has) last = i }
                setOnKeyListener { _, code, ev ->
                    if (!okKey(code)) return@setOnKeyListener false
                    if (ev.action == KeyEvent.ACTION_UP) type(i)
                    true
                }
                setOnClickListener { type(i) }
            }
            val status = Ui.text(context, "", 20f, Ui.soft)
            val thumb = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = Ui.panel(12f, Color.argb(120, 10, 18, 14), Color.argb(50, 255, 255, 255))
                clipToOutline = true
            }
            val download = Ui.chip(context, "Download", 22f) { get(i) }.apply { background = glass() }
            val remove = Ui.chip(context, "Remove", 22f) { remove(i) }.apply { background = glass() }
            rows += Row(field, status, thumb, download)

            // Quick starts for the link: tinyurl, bit.ly, or a whole link.
            val quick = Ui.row(context, 10)
            for ((label, start) in listOf("tinyurl.com/" to Links.TINY, "bit.ly/" to Links.BITLY, "Full link" to "")) {
                quick.addView(Ui.chip(context, label, 18f) {
                    field.text = start
                    pictures.setLink(i, start)
                    field.requestFocus()
                }.apply { background = glass() }, LinearLayout.LayoutParams(-2, 44))
            }
            addView(quick, Ui.at(380, y, 900))
            addView(field, Ui.at(96, y + 56, 1020, 64))
            addView(download, Ui.at(1136, y + 56, -2, 64))
            addView(remove, Ui.at(1318, y + 56, -2, 64))
            addView(status, Ui.at(100, y + 128, 1300))
            addView(thumb, Ui.at(1590, y + 8, 234, 132))
            show(i)
        }
        // The screen dims a little behind the keyboard, so the keys are what you see.
        addView(veil, LayoutParams(Ui.W, Ui.H))
        addView(keyboard, Ui.at(96, Ui.H - KeyboardView.HEIGHT - 24, KeyboardView.WIDTH, KeyboardView.HEIGHT))
    }

    private fun okKey(code: Int) = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER ||
        code == KeyEvent.KEYCODE_NUMPAD_ENTER || code == KeyEvent.KEYCODE_BUTTON_A

    /** The keyboard, for slot [i]'s link: it types into the link as it goes; Done keeps it and
     *  goes to Download, Back keeps it and goes back to the link. */
    private fun type(i: Int) {
        val field = rows[i].field
        last = i
        keyboard.changed = { field.text = it }
        keyboard.finished = { veil.visibility = View.GONE; pictures.setLink(i, it); rows[i].download.requestFocus() }
        keyboard.closed = { veil.visibility = View.GONE; pictures.setLink(i, it); field.requestFocus() }
        veil.visibility = View.VISIBLE
        keyboard.open(Pictures.SLOTS[i].name, field.text.toString())
    }

    /** Download slot [i]'s link (the user pressed Download). */
    private fun get(i: Int) {
        val r = rows[i]
        last = i
        val typed = r.field.text.toString()
        r.status.setTextColor(Ui.soft)
        r.status.text = Links.normalise(typed)?.let { "Getting it from ${Links.host(it)}…" } ?: "Type a link first."
        pictures.download(i, typed) { problem ->
            if (problem == null) {
                show(i)
            } else {
                r.status.setTextColor(Color.rgb(255, 150, 120))
                r.status.text = "Couldn't get it: $problem" + if (pictures.has(i)) " The picture you had stays." else ""
            }
            r.download.requestFocus()
        }
    }

    private fun remove(i: Int) {
        pictures.remove(i)
        last = i
        show(i)
    }

    /** Slot [i]'s line and thumbnail as they are now. */
    private fun show(i: Int) {
        val r = rows[i]
        r.status.setTextColor(Ui.soft)
        val from = pictures.from(i)
        r.status.text = when {
            !pictures.has(i) -> "No picture yet."
            from.isNotEmpty() -> "On this TV · from $from"
            else -> "On this TV"
        }
        Thread {
            val bmp: Bitmap? = pictures.load(i)?.first?.let { Bitmap.createScaledBitmap(it, 234, 132, true) }
            post { r.thumb.setImageBitmap(bmp) }
        }.start()
    }

    /** Back closes Pictures (the remote's Back, when the keyboard isn't up). */
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK) {
            if (e.action == KeyEvent.ACTION_UP) { if (keyboard.open) keyboard.back() else done(last) }
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    /** Focus slot [i]'s link (opened from the countdown's card or chip). */
    fun focus(i: Int) {
        last = i
        rows.getOrNull(i)?.field?.requestFocus()
    }

    /** For tests and screenshots: put [text] in slot [i]'s link, without downloading. */
    fun type(i: Int, text: String) {
        rows.getOrNull(i)?.field?.apply { this.text = text; requestFocus() }
    }

    /** The chips as dark green glass. */
    private fun glass() = Ui.focusable(40f, Color.argb(90, 4, 14, 9), Color.argb(190, 10, 38, 24))
}
