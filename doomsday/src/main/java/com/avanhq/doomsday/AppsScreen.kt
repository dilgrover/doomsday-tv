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
 * Settings → Apps: every app on the TV in the row's order. OK on an app lifts it;
 * the arrows carry it through the grid (Left and Right one place, Up and Down a row); OK puts it
 * down and saves; Back puts it back. Hide, Show hidden apps and Reset order are along the top.
 * On the countdown it's the same: hold OK on an app in its row ([com.avanhq.tv.LiftRow]).
 */
class AppsScreen(context: Context, private val settings: Settings, private val done: () -> Unit) : FrameLayout(context) {
    private companion object {
        const val PER_ROW = 6
        const val TILE_W = 240
        const val TILE_H = 135
    }

    private val grid = Ui.column(context, 26).apply { clipChildren = false }
    private val scroll = ScrollView(context).apply {
        isVerticalScrollBarEnabled = false
        clipToPadding = false
        setPadding(24, 30, 0, 60)
        clipChildren = false
        addView(grid)
    }
    private val picked = Ui.text(context, "", 28f, Ui.text, bold = true)
    private val hint = Ui.text(context, "OK on an app to move it", 22f, Ui.soft)
    private val hide = chip("Hide") { toggle() }
    private val showHidden = chip("") { showingHidden = !showingHidden; fill(pkg) }
    private val reset = chip("Reset order") { settings.appOrder = emptyList(); fill(pkg) }
    private var pkg: String? = null
    private var shown: List<String> = emptyList()
    private var showingHidden = true

    // The app being moved (OK lifted it): its order before, to put back on Back.
    private var lifted: String? = null
    private var liftedFrom: List<String> = emptyList()
    private var heldOk = false

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
        addView(Ui.text(context, "The apps in the row and their order. OK on an app picks it up: move it with the arrows, OK puts it down. On the countdown, hold OK on an app.", 24f, Ui.soft, lines = 2), Ui.at(98, 116, 1500))
        addView(Ui.chip(context, "Done", 24f) { done() }.apply { background = glass() }, Ui.atRight(96, 48, 160, 64))
        val bar = Ui.row(context, 14)
        bar.addView(picked, LinearLayout.LayoutParams(360, -2))
        bar.addView(hide, LinearLayout.LayoutParams(-2, 60))
        bar.addView(showHidden, LinearLayout.LayoutParams(-2, 60))
        bar.addView(reset, LinearLayout.LayoutParams(-2, 60))
        bar.addView(hint, LinearLayout.LayoutParams(-2, -2).apply { marginStart = 18 })
        addView(bar, Ui.at(96, 196, -2, 64))
        addView(scroll, Ui.at(96, 270, Ui.W - 96 * 2 + 40, Ui.H - 270))
        fill()
    }

    /** The grid, in the row's order (hidden apps after, dimmed); [focus] keeps an app selected. */
    private fun fill(focus: String? = null, order: List<String>? = null) {
        grid.removeAllViews()
        val all = AppsRow.load(context, settings, all = true)
        val hidden = settings.hiddenApps
        val byPkg = all.associateBy { it.pkg }
        shown = order ?: all.filter { it.pkg !in hidden }.map { it.pkg }
        val listed = shown.mapNotNull { byPkg[it] } + (if (showingHidden) all.filter { it.pkg in hidden } else emptyList())
        var row: LinearLayout? = null
        var focusView: View? = null
        for ((n, a) in listed.withIndex()) {
            // Not clipped: a lifted app is bigger than its place.
            if (n % PER_ROW == 0) row = Ui.row(context, 28).also { it.clipChildren = false; it.clipToPadding = false; grid.addView(it) }
            val t = tile(a, a.pkg in hidden)
            row!!.addView(t, LinearLayout.LayoutParams(TILE_W, TILE_H + 44))
            if (a.pkg == focus) focusView = (t as FrameLayout).getChildAt(0)
        }
        if (listed.isEmpty()) grid.addView(Ui.text(context, "No apps found on this TV.", 24f, Ui.soft))
        showHidden.text = if (showingHidden) "Hidden apps: shown" else "Hidden apps: not shown"
        val p = focus ?: listed.firstOrNull()?.pkg
        pick(p, listed.firstOrNull { it.pkg == p }?.label)
        focusView?.requestFocus()
    }

    private fun tile(a: AppsRow.App, hidden: Boolean): View {
        val box = FrameLayout(context).apply { clipChildren = false; tag = a.pkg }
        val up = a.pkg == lifted
        val name = Ui.text(context, if (hidden) "${a.label} · hidden" else a.label, 19f, if (up) Ui.gold else if (hidden) Ui.dim else Ui.soft, gravity = Gravity.CENTER)
        val face = FrameLayout(context).apply {
            background = Ui.panel(18f, Color.argb(160, 10, 18, 14), Color.argb(40, 255, 255, 255))
            clipToOutline = true
            isFocusable = true
            isClickable = true
            foreground = if (up) GradientDrawable().apply { cornerRadius = 18f; setColor(Color.TRANSPARENT); setStroke(6, Ui.gold) } else Ui.focusRing(18f)
            contentDescription = a.label
            alpha = if (hidden) .35f else if (lifted != null && !up) .5f else 1f
            addView(ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setImageDrawable(a.banner) }, LayoutParams(-1, -1))
            if (up) { scaleX = 1.15f; scaleY = 1.15f; translationY = -10f }
            setOnFocusChangeListener { v, has ->
                if (up) return@setOnFocusChangeListener
                v.animate().scaleX(if (has) 1.08f else 1f).scaleY(if (has) 1.08f else 1f).setDuration(300).setInterpolator(Ui.EASE).start()
                if (has) pick(a.pkg, a.label)
            }
            // OK: a shown app lifts to be moved; a hidden one comes back into the row.
            setOnClickListener { if (hidden) toggle() else liftUp(a.pkg) }
        }
        box.addView(face, LayoutParams(TILE_W, TILE_H))
        box.addView(name, LayoutParams(TILE_W, 40, Gravity.BOTTOM))
        return box
    }

    private fun pick(p: String?, label: String?) {
        pkg = p
        picked.text = label ?: ""
        hide.text = if (p != null && p in settings.hiddenApps) "Show" else "Hide"
    }

    private fun liftUp(p: String) {
        lifted = p
        liftedFrom = shown
        heldOk = true
        hint.text = "◀ ▶ ▲ ▼ move · OK puts it down · Back puts it back"
        hint.setTextColor(Ui.gold)
        fill(p, shown)
    }

    private fun drop(keep: Boolean) {
        val p = lifted ?: return
        lifted = null
        if (keep) settings.appOrder = shown
        hint.text = "OK on an app to move it"
        hint.setTextColor(Ui.soft)
        fill(p, if (keep) null else liftedFrom)
    }

    private fun toggle() {
        val p = pkg ?: return
        settings.hiddenApps = AppOrder.toggle(settings.hiddenApps, p)
        fill(p)
    }

    /** Where the remote starts: the first app. */
    fun focusFirst() {
        ((grid.getChildAt(0) as? LinearLayout)?.getChildAt(0) as? FrameLayout)?.getChildAt(0)?.requestFocus()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val p = lifted
        if (p != null) {
            val down = e.action == KeyEvent.ACTION_DOWN
            val by = when (e.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> -1
                KeyEvent.KEYCODE_DPAD_RIGHT -> 1
                KeyEvent.KEYCODE_DPAD_UP -> -PER_ROW
                KeyEvent.KEYCODE_DPAD_DOWN -> PER_ROW
                else -> 0
            }
            when {
                by != 0 -> if (down) {
                    val i = shown.indexOf(p)
                    val to = (i + by).coerceIn(0, shown.size - 1)
                    if (to != i) fill(p, AppOrder.move(shown, p, to - i))
                }
                e.keyCode == KeyEvent.KEYCODE_BACK -> if (!down) drop(keep = false)
                e.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || e.keyCode == KeyEvent.KEYCODE_ENTER ||
                    e.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || e.keyCode == KeyEvent.KEYCODE_BUTTON_A -> {
                    // The OK that lifted it is released first: only a new press puts it down.
                    if (down && e.repeatCount == 0) heldOk = false
                    if (!down && !heldOk) drop(keep = true)
                }
            }
            return true
        }
        if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) {
            done()
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun chip(label: String, onClick: () -> Unit): TextView = Ui.chip(context, label, 22f, onClick).apply { background = glass() }

    private fun glass() = Ui.focusable(40f, Color.argb(90, 4, 14, 9), Color.argb(190, 10, 38, 24))
}
