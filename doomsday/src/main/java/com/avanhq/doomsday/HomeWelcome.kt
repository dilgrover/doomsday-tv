package com.avanhq.doomsday

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.provider.Settings as Android
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.avanhq.tv.Ui

/**
 * The one question when the app first opens: "Make Doomsday your home screen?". Yes takes you to where the TV lets you choose (the
 * app can't switch it on itself): Google TV's Accessibility with two short steps, a TV box's own
 * home choice, or on Fire TV an honest "it may be blocked". Not now: never asked again (Settings
 * → Home button still has it). Not asked at all when another app already handles Home ([Home]).
 */
class HomeWelcome(context: Context, private val settings: Settings, private val done: () -> Unit) : FrameLayout(context) {
    private val text = Ui.text(context, "", 26f, Ui.soft, lines = 4)
    private val yes: TextView
    private var steps = false // Google TV: the two steps are showing, Yes now opens Accessibility

    init {
        setBackgroundColor(Color.argb(170, 0, 0, 0))
        isClickable = true
        val panel = Ui.column(context, 18).apply {
            background = Ui.panel(30f, Color.argb(240, 6, 12, 9), Color.argb(120, 240, 192, 96))
            setPadding(48, 40, 48, 40)
        }
        panel.addView(Ui.text(context, "Make Doomsday your home screen?", 40f, Ui.text, bold = true))
        text.text = when {
            Device.fireTv(context) -> "The countdown and your apps on the Home button. Fire TV updates may block other home screens, but you can try."
            else -> "The countdown and your apps, every time you press Home."
        }
        panel.addView(text)
        val row = Ui.row(context, 16)
        yes = Ui.chip(context, "Yes, set it up", 26f) { yes() }.apply { background = glass() }
        row.addView(yes, LinearLayout.LayoutParams(-2, 68))
        row.addView(Ui.chip(context, "Not now", 26f) { finish() }.apply { background = glass() }, LinearLayout.LayoutParams(-2, 68))
        panel.addView(row, LinearLayout.LayoutParams(-2, -2).apply { topMargin = 10 })
        addView(panel, Ui.at(360, 300, 1200))
    }

    fun focusFirst() { yes.requestFocus() }

    private fun yes() {
        val c = context
        when {
            Home.googleTv(c) || Device.fireTv(c) -> {
                if (!steps) {
                    // The TV only lets you switch it on yourself: say how, then open the place.
                    steps = true
                    text.text = "Two steps in the TV's Accessibility settings:\n1.  Choose “Doomsday opens when you press Home”.\n2.  Switch it on, then press Back."
                    yes.text = "Open Accessibility"
                    return
                }
                open(Android.ACTION_ACCESSIBILITY_SETTINGS)
            }
            // A TV box with a home choice: its own setting, or Android's "which home?" question.
            else -> if (!open(Android.ACTION_HOME_SETTINGS)) {
                runCatching { c.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        finish()
    }

    private fun open(action: String): Boolean =
        runCatching { context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess

    private fun finish() {
        settings.homeAsked = true
        done()
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) {
            finish()
            return true
        }
        return super.dispatchKeyEvent(e)
    }

    private fun glass() = Ui.focusable(40f, Color.argb(90, 4, 14, 9), Color.argb(190, 10, 38, 24))
}
