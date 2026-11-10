package com.avanhq.tv

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView

/**
 * Moving an app along a row the Google TV way:
 * hold OK on an app and it lifts; Left and Right carry it along the row while the others slide
 * aside; OK puts it down there; Back puts it back where it was; Down asks to hide it (OK hides,
 * Back keeps it). Used by the Doomsday app's apps row and its Settings → Apps.
 *
 * The row is [strip]: one box per app, each tagged with its package. [face] is the box's
 * focusable tile, [caption] the name under it (the hint shows there while lifted).
 */
class LiftRow(
    private val strip: ViewGroup,
    private val face: (View) -> View?,
    private val caption: (View) -> TextView?,
    private val onDrop: (List<String>) -> Unit,
    private val onHide: (String) -> Unit,
    /** The box's normal focused look again, and the row scrolled to show it. */
    private val settle: (View) -> Unit,
) {
    private var lifted: View? = null
    private var before: List<View> = emptyList()
    private var asking = false
    // The OK that lifted the app is still held: its release mustn't drop it straight away.
    private var heldOk = false
    private var savedText: CharSequence? = null
    private var savedColor = Color.WHITE
    private var savedSize = 0f
    private var savedRing: Drawable? = null

    val active: Boolean get() = lifted != null

    /** The packages in the row as it stands, first to last. */
    fun order(): List<String> = (0 until strip.childCount).mapNotNull { strip.getChildAt(it).tag as? String }

    /** Lift [pkg]'s app (OK is being held on it). */
    fun lift(pkg: String) {
        val box = (0 until strip.childCount).map { strip.getChildAt(it) }.firstOrNull { it.tag == pkg } ?: return
        lifted = box
        before = (0 until strip.childCount).map { strip.getChildAt(it) }
        asking = false
        heldOk = true
        caption(box)?.let { c ->
            savedText = c.text
            savedColor = c.currentTextColor
            savedSize = c.textSize
        }
        face(box)?.let { savedRing = it.foreground }
        look()
    }

    /** A key while an app is lifted; true when it was used (every key is, while lifted). */
    fun key(e: KeyEvent): Boolean {
        val box = lifted ?: return false
        val down = e.action == KeyEvent.ACTION_DOWN
        when (e.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> if (down && !asking) move(if (e.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1)
            KeyEvent.KEYCODE_DPAD_DOWN -> if (down && !asking) { asking = true; look() }
            KeyEvent.KEYCODE_DPAD_UP -> if (down && asking) { asking = false; look() }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_BUTTON_A -> {
                if (down && e.repeatCount == 0) heldOk = false
                if (!down && !heldOk) {
                    val pkg = box.tag as? String
                    if (asking && pkg != null) { end(); onHide(pkg) } else { end(); onDrop(order()) }
                }
            }
            // On release, as the screens close on Back's release (else that would close one too).
            KeyEvent.KEYCODE_BACK -> if (!down) {
                if (asking) { asking = false; look() } else cancel()
            }
        }
        return true
    }

    /** Put it back where it was. */
    fun cancel() {
        val box = lifted ?: return
        if (order() != before.mapNotNull { it.tag as? String }) {
            val now = (0 until strip.childCount).map { strip.getChildAt(it) }
            val lefts = now.associateWith { it.left }
            // The others first, then the lifted one: removing the focused box would lose the focus.
            for (v in now) if (v !== box) strip.removeView(v)
            for ((i, v) in before.withIndex()) if (v !== box) strip.addView(v, i.coerceAtMost(strip.childCount))
            slide(lefts)
        }
        end()
    }

    private fun move(by: Int) {
        val box = lifted ?: return
        val i = strip.indexOfChild(box)
        val to = i + by
        if (to < 0 || to >= strip.childCount) return
        val lefts = (0 until strip.childCount).map { strip.getChildAt(it) }.associateWith { it.left }
        // The neighbour changes places, not the lifted (focused) app: it keeps the focus.
        val other = strip.getChildAt(to)
        strip.removeView(other)
        strip.addView(other, i)
        slide(lefts)
        settle(box)
        look()
    }

    /** After a change of places, each box glides from where it was to where it is now. */
    private fun slide(lefts: Map<View, Int>) {
        strip.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                strip.viewTreeObserver.removeOnPreDrawListener(this)
                for ((v, was) in lefts) {
                    val dx = (was - v.left).toFloat()
                    if (dx == 0f) continue
                    v.translationX = dx
                    v.animate().translationX(0f).setDuration(if (Ui.still) 0 else 240).setInterpolator(Ui.EASE).start()
                }
                return true
            }
        })
    }

    /** The lifted app: bigger, raised, a gold ring, the others dimmed, the hint under it. */
    private fun look() {
        val box = lifted ?: return
        for (k in 0 until strip.childCount) {
            val v = strip.getChildAt(k)
            face(v)?.animate()?.alpha(if (v === box) 1f else .5f)?.setDuration(200)?.start()
        }
        face(box)?.let { f ->
            f.foreground = GradientDrawable().apply {
                cornerRadius = 18f
                setColor(Color.TRANSPARENT)
                setStroke(6, if (asking) Color.rgb(255, 120, 100) else Ui.gold)
            }
            f.animate().scaleX(1.3f).scaleY(1.3f).translationY(-18f).setDuration(if (Ui.still) 0 else 200).setInterpolator(Ui.EASE).start()
        }
        caption(box)?.let { c ->
            c.text = if (asking) "OK hides it · Back keeps it" else "◀ ▶ move · OK drop · ▼ hide"
            c.setTextColor(if (asking) Color.rgb(255, 160, 140) else Ui.gold)
            c.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f)
            c.alpha = 1f
            c.visibility = View.VISIBLE
        }
    }

    private fun end() {
        val box = lifted ?: return
        lifted = null
        asking = false
        for (k in 0 until strip.childCount) face(strip.getChildAt(k))?.animate()?.alpha(1f)?.setDuration(200)?.start()
        face(box)?.let { it.foreground = savedRing }
        caption(box)?.let { c ->
            c.text = savedText
            c.setTextColor(savedColor)
            if (savedSize > 0f) c.setTextSize(TypedValue.COMPLEX_UNIT_PX, savedSize)
        }
        settle(box)
    }
}
