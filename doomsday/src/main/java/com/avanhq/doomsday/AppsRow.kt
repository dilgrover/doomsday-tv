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
 * The TV's apps, each with its own banner, in your order ([AppOrder]: Settings → Apps, or hold
 * OK on an app to move or hide it). It stays out of sight until the remote goes to it, so it
 * doesn't cover the picture.
 * OK opens the app.
 */
class AppsRow(context: Context, private val settings: Settings) : HorizontalScrollView(context) {
    class App(val pkg: String, val label: String, val banner: Drawable?, val launch: Intent)

    private val strip = Ui.row(context, 28)

    /** The packages as they're shown now, first to last. */
    var shown: List<String> = emptyList()
        private set


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
        val list = load(context, settings, all = false)
        shown = list.map { it.pkg }
        for (a in list) strip.addView(tile(a), android.widget.LinearLayout.LayoutParams(TILE_W, TILE_H + 40))
    }

    /** The app whose tile [v] is (the focused one), or null. */
    fun appOf(v: View?): String? {
        var x: View? = v
        while (x != null && x !== this) {
            (x.tag as? String)?.let { return it }
            x = x.parent as? View
        }
        return null
    }

    /** [pkg]'s name, as the row shows it. */
    fun label(pkg: String): String {
        for (i in 0 until strip.childCount) {
            val box = strip.getChildAt(i)
            if (box.tag == pkg) return (box as? FrameLayout)?.getChildAt(0)?.contentDescription?.toString() ?: pkg
        }
        return pkg
    }

    /** Focus [pkg]'s tile (after it moved). */
    fun focus(pkg: String) {
        for (i in 0 until strip.childCount) {
            val box = strip.getChildAt(i)
            if (box.tag == pkg) { (box as? FrameLayout)?.getChildAt(0)?.requestFocus(); return }
        }
    }

    companion object Load {
        const val TILE_W = 240
        const val TILE_H = 135
        const val HEIGHT = TILE_H + 70

        /** The TV's apps in your order; with [all], hidden ones too (for Settings → Apps). */
        fun load(context: Context, settings: Settings, all: Boolean): List<App> {
            val found = apps(context)
            val order = AppOrder.arrange(found.map { AppOrder.App(it.pkg, it.label) }, settings.appOrder, settings.hiddenApps, context.packageName, all)
            val byPkg = found.associateBy { it.pkg }
            return order.mapNotNull { byPkg[it.pkg] }
        }

        private fun apps(context: Context): List<App> {
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
                App(pkg, ri.loadLabel(pm).toString(), ai.loadBanner(pm) ?: ai.applicationInfo.loadBanner(pm) ?: ri.loadIcon(pm), launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    private fun tile(a: App): View {
        val box = FrameLayout(context).apply { clipChildren = false; tag = a.pkg }
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
