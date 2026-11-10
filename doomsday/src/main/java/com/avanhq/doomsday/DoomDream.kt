package com.avanhq.doomsday

import android.content.Context
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.avanhq.tv.Ui

/**
 * Doomsday as the TV's screensaver: the countdown over the pictures with their
 * light and motion, nothing to select. Chosen in the TV's own settings (Screen saver →
 * Doomsday); Settings in the app points there. The same view previews it in the app
 * (MainActivity, Settings → Preview).
 */
class DoomDream : DreamService() {
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        setContentView(DoomDream.view(this))
    }

    companion object {
        /** The screensaver's screen, laid out for 1920 x 1080 and scaled to the TV. */
        fun view(context: Context): View {
            val root = FrameLayout(context).apply { setBackgroundColor(android.graphics.Color.BLACK) }
            val stage = FrameLayout(context)
            root.addView(stage, FrameLayout.LayoutParams(Ui.W, Ui.H, Gravity.TOP or Gravity.START))
            root.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
                val scale = minOf((r - l) / Ui.W.toFloat(), (b - t) / Ui.H.toFloat())
                stage.pivotX = 0f
                stage.pivotY = 0f
                stage.scaleX = scale
                stage.scaleY = scale
            }
            val screen = DoomsdayScreen(context, Pictures(context), Settings(context), {}, {}, ambient = true)
            stage.addView(screen, FrameLayout.LayoutParams(Ui.W, Ui.H))
            return root
        }
    }
}
