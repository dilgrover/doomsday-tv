package com.avanhq.doomsday

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import com.avanhq.tv.Ui

/**
 * The TV's apps along the bottom, each with its own
 * banner, by name. A lean apps row: it stays out of sight until the remote
 * goes down to it, so it doesn't cover the picture. OK opens the app.
 */
class AppsRow(context: Context) : HorizontalScrollView(context) {
    companion object {
        const val TILE_W = 240
        const val TILE_H = 135
        const val HEIGHT = TILE_H + 70
    }

    private class App(val label: String, val banner: Drawable?, val launch: Intent)

    private val strip = Ui.row(context, 28)

    init {
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        clipChildren = false
        setPadding(96, 14, 96, 14)
        strip.clipChildren = false
        strip.clipToPadding = false
        addView(strip)
        alpha = 0f
        refresh()
    }

    /** Whether the focus is on one of the apps. */
    fun holds(v: View?): Boolean = v != null && generateSequence(v.parent) { it.parent }.any { it === this }

    fun refresh() {
        strip.removeAllViews()
        for (a in apps()) strip.addView(tile(a), android.widget.LinearLayout.LayoutParams(TILE_W, TILE_H + 40))
    }

    private fun apps(): List<App> {
        val pm = context.packageManager
        val tv = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val phone = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val tvApps = pm.queryIntentActivities(tv, 0)
        val phoneApps = pm.queryIntentActivities(phone, 0)
        val byPackage = (tvApps + phoneApps).groupBy { it.activityInfo.packageName }
        // A Fire TV lists many apps only as phone-style launchers (Device.appsToShow).
        val shown = Device.appsToShow(tvApps.map { it.activityInfo.packageName }, phoneApps.map { it.activityInfo.packageName }, Device.fireTv(context), context.packageName)
        return shown.mapNotNull { pkg ->
            val ri = byPackage[pkg]?.firstOrNull() ?: return@mapNotNull null
            val ai = ri.activityInfo
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
                ?: Intent(Intent.ACTION_MAIN).setClassName(pkg, ai.name)
            App(ri.loadLabel(pm).toString(), ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm) ?: ri.loadIcon(pm), launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.sortedBy { it.label.lowercase() }
    }

    private fun tile(a: App): View {
        val box = FrameLayout(context).apply { clipChildren = false }
        val face = FrameLayout(context).apply {
            background = Ui.panel(18f, Color.argb(160, 10, 18, 14), Color.argb(40, 255, 255, 255))
            clipToOutline = true
            isFocusable = true
            isClickable = true
            foreground = Ui.focusRing(18f)
            contentDescription = a.label
            setOnClickListener { runCatching { context.startActivity(a.launch) } }
            addView(ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageDrawable(a.banner)
            }, FrameLayout.LayoutParams(-1, -1))
        }
        val name: TextView = Ui.text(context, a.label, 20f, Ui.soft, gravity = Gravity.CENTER)
        face.setOnFocusChangeListener { v, has ->
            v.animate().scaleX(if (has) 1.1f else 1f).scaleY(if (has) 1.1f else 1f).setDuration(350).setInterpolator(Ui.EASE).start()
            name.visibility = if (has) View.VISIBLE else View.INVISIBLE
            if (has) smoothScrollTo((v.parent as View).left - 96 * 4, 0)
        }
        name.visibility = View.INVISIBLE
        box.addView(face, FrameLayout.LayoutParams(TILE_W, TILE_H))
        box.addView(name, FrameLayout.LayoutParams(TILE_W, 36, Gravity.BOTTOM))
        return box
    }
}
