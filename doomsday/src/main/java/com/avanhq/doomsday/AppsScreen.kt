package com.avanhq.doomsday

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.avanhq.tv.Fonts
import com.avanhq.tv.Ui

/**
 * Settings → Apps: every app on
 * the TV in the row's order, hidden ones too (dimmed). Select an app, then Move earlier, Move
 * later, To the front, or Hide / Show; it's saved as it's pressed ([AppOrder]). The same moves
 * are on the countdown screen: hold OK on an app in its row.
 */
class AppsScreen(context: Context, private val settings: Settings, private val done: () -> Unit) : FrameLayout(context) {
    private companion object {
        const val PER_ROW = 6
        const val TILE_W = 240
        const val TILE_H = 135
    }

    private val grid = Ui.column(context, 26)
    private val scroll = ScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        clipToPadding = false
        setPadding(0, 18, 0, 60)
        addView(grid)
    }
    private val picked = Ui.text(context, "", 28f, Ui.text, bold = true)
    private val earlier = chip("◀  Move earlier") { move(-1) }
    private val later = chip("Move later  ▶") { move(1) }
    private val first = chip("To the front") { toFront() }
    private val hide = chip("Hide") { toggle() }
    private var pkg: String? = null
    private var shown: List<String> = emptyList()

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
        addView(Ui.text(context, "Apps", 56f, Ui.text).apply { typeface = Fonts.of(context, Fonts.LIGHT) }, Ui.at(96, 40, 900))
        addView(Ui.text(context, "The apps in the row and their order. Select an app, then move it or hide it. On the countdown, hold OK on an app for the same.", 24f, Ui.soft, lines = 2), Ui.at(98, 116, 1500))
        addView(Ui.chip(context, "Done", 24f) { done() }.apply { background = glass() }, Ui.atRight(96, 48, 160, 64))
        val bar = Ui.row(context, 14)
        bar.addView(picked, LinearLayout.LayoutParams(420, -2))
        for (c in listOf(earlier, later, first, hide)) bar.addView(c, LinearLayout.LayoutParams(-2, 60))
        addView(bar, Ui.at(96, 196, -2, 64))
        addView(scroll, Ui.at(96, 280, Ui.W - 96 * 2 + 40, Ui.H - 280))
        fill()
    }

    /** The grid, in the row's order (hidden apps after, dimmed); [focus] keeps an app selected. */
    private fun fill(focus: String? = null) {
        grid.removeAllViews()
        val all = AppsRow.load(context, settings, all = true)
        val hidden = settings.hiddenApps
        shown = all.filter { it.pkg !in hidden }.map { it.pkg }
        val listed = all.filter { it.pkg !in hidden } + all.filter { it.pkg in hidden }
        var row: LinearLayout? = null
        var focusView: View? = null
        for ((n, a) in listed.withIndex()) {
            if (n % PER_ROW == 0) row = Ui.row(context, 28).also { grid.addView(it) }
            val t = tile(a, a.pkg in hidden)
            row!!.addView(t, LinearLayout.LayoutParams(TILE_W, TILE_H + 44))
            if (a.pkg == focus) focusView = (t as FrameLayout).getChildAt(0)
        }
        if (listed.isEmpty()) grid.addView(Ui.text(context, "No apps found on this TV.", 24f, Ui.soft))
        val p = focus ?: listed.firstOrNull()?.pkg
        pick(p, listed.firstOrNull { it.pkg == p }?.label)
        focusView?.requestFocus()
    }

    private fun tile(a: AppsRow.App, hidden: Boolean): View {
        val box = FrameLayout(context).apply { clipChildren = false; tag = a.pkg }
        val name = Ui.text(context, if (hidden) "${a.label} · hidden" else a.label, 19f, if (hidden) Ui.dim else Ui.soft, gravity = Gravity.CENTER)
        val face = FrameLayout(context).apply {
            background = Ui.panel(18f, Color.argb(160, 10, 18, 14), Color.argb(40, 255, 255, 255))
            clipToOutline = true
            isFocusable = true
            isClickable = true
            foreground = Ui.focusRing(18f)
            contentDescription = a.label
            alpha = if (hidden) .35f else 1f
            addView(ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageDrawable(a.banner) }, LayoutParams(-1, -1))
            setOnFocusChangeListener { v, has ->
                v.animate().scaleX(if (has) 1.08f else 1f).scaleY(if (has) 1.08f else 1f).setDuration(300).setInterpolator(Ui.EASE).start()
                if (has) pick(a.pkg, a.label)
            }
            // OK on an app: up to its moves.
            setOnClickListener { pick(a.pkg, a.label); earlier.requestFocus() }
        }
        box.addView(face, LayoutParams(TILE_W, TILE_H))
        box.addView(name, LayoutParams(TILE_W, 40, Gravity.BOTTOM))
        return box
    }

    private fun pick(p: String?, label: String?) {
        pkg = p
        picked.text = label ?: ""
        val hidden = p != null && p in settings.hiddenApps
        hide.text = if (hidden) "Show" else "Hide"
        for (c in listOf(earlier, later, first)) c.alpha = if (hidden) .4f else 1f
    }

    private fun move(by: Int) {
        val p = pkg ?: return
        if (p !in shown) return
        settings.appOrder = AppOrder.move(shown, p, by)
        fill(p)
    }

    private fun toFront() {
        val p = pkg ?: return
        if (p !in shown) return
        settings.appOrder = AppOrder.front(shown, p)
        fill(p)
    }

    private fun toggle() {
        val p = pkg ?: return
        settings.hiddenApps = AppOrder.toggle(settings.hiddenApps, p)
        fill(p)
        hide.requestFocus()
    }

    /** Where the remote starts: the first app. */
    fun focusFirst() {
        ((grid.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? FrameLayout)?.getChildAt(0)?.requestFocus()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) {
            done()
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun chip(label: String, onClick: () -> Unit): TextView = Ui.chip(context, label, 22f, onClick).apply { background = glass() }

    private fun glass() = Ui.focusable(40f, Color.argb(90, 4, 14, 9), Color.argb(190, 10, 38, 24))
}
