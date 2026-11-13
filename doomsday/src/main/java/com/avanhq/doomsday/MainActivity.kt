package com.avanhq.doomsday

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.avanhq.tv.Ui

/**
 * The Doomsday app: opens straight on the countdown; Pictures holds the four links.
 * The screens are laid out for 1920 x 1080 and scaled to the TV.
 *
 * For the VM's tests and screenshots (adb `am start ... --es`/`--ei`/`--ez`):
 * `--ez settings true` opens Settings, `--ez city true` its City screen, `--ez dream true` previews the screensaver, `--ei pictures N` opens Pictures on slot N, `--es type<N> TEXT` fills slot N's link (no
 * download), `--ez summon true` summons Doom as if OK were held.
 */
class MainActivity : Activity() {
    private lateinit var pictures: Pictures
    private lateinit var countdown: DoomsdayScreen
    private var links: PicturesScreen? = null
    private var options: SettingsScreen? = null
    private var town: CityScreen? = null
    private var preview: View? = null
    private lateinit var settings: Settings
    private lateinit var stage: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pictures = Pictures(this)
        settings = Settings(this)
        val root = FrameLayout(this)
        stage = FrameLayout(this).apply { clipChildren = false }
        root.addView(stage, FrameLayout.LayoutParams(Ui.W, Ui.H, Gravity.TOP or Gravity.START))
        root.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
            val scale = minOf((r - l) / Ui.W.toFloat(), (b - t) / Ui.H.toFloat())
            stage.pivotX = 0f
            stage.pivotY = 0f
            stage.scaleX = scale
            stage.scaleY = scale
        }
        setContentView(root)
        countdown = DoomsdayScreen(this, pictures, settings, { openPictures(it) }, { openSettings() })
        stage.addView(countdown, FrameLayout.LayoutParams(Ui.W, Ui.H))
        countdown.post { countdown.focusDefault() }
        // The first time: "Make Doomsday your home screen?" (not if another app has Home).
        val canAsk = BuildConfig.HOME_WATCHER || !(Home.googleTv(this) || Device.fireTv(this)) // standard: only where a home choice exists
        if ((canAsk && Home.shouldAsk(settings.homeAsked, Home.enabled(this), packageName)) || (BuildConfig.DEBUG && intent.getBooleanExtra("welcome", false))) openWelcome()
        handle(intent)
        // An app installed or updated: the row shows it (and its new banner) straight away.
        packages = com.avanhq.tv.PackageWatch.register(this) { AppsRow.refreshAll() }
    }

    private var packages: android.content.BroadcastReceiver? = null

    override fun onDestroy() {
        packages?.let { runCatching { unregisterReceiver(it) } }
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: Intent) {
        // Shortcuts for testing on a TV (adb). Not in the released app: any app on the TV can
        // start this one, so a release build ignores them.
        if (!BuildConfig.DEBUG) return
        if (i.getBooleanExtra("settings", false)) openSettings()
        if (i.getBooleanExtra("city", false)) { openSettings(); openCity() }
        if (i.getBooleanExtra("apps", false)) { openSettings(); openApps() }
        i.getStringExtra("lift")?.let { pkg -> countdown.post { countdown.liftApp(pkg) } }
        if (i.getBooleanExtra("dream", false)) openPreview()
        if (i.hasExtra("pictures")) openPictures(i.getIntExtra("pictures", 0))
        for (n in Pictures.SLOTS.indices) i.getStringExtra("type$n")?.let { text -> links?.type(n, text) }
        if (i.getBooleanExtra("summon", false)) countdown.postDelayed({ countdown.summon() }, 3000)
    }

    private fun openPictures(slot: Int) {
        if (links == null) {
            val p = PicturesScreen(this, pictures) { closePictures(it) }
            links = p
            stage.addView(p, FrameLayout.LayoutParams(Ui.W, Ui.H))
            countdown.visibility = View.INVISIBLE // its pictures rest while Pictures is open
        }
        links?.post { links?.focus(slot) }
    }

    private fun closePictures(slot: Int) {
        links?.let { stage.removeView(it) }
        links = null
        if (options != null) { options?.focusFirst(); return } // opened from Settings: back there
        countdown.visibility = View.VISIBLE
        countdown.backTo(slot)
    }

    private var welcome: HomeWelcome? = null

    private fun openWelcome() {
        if (welcome != null) return
        val w = HomeWelcome(this, settings) { closeWelcome() }
        welcome = w
        stage.addView(w, FrameLayout.LayoutParams(Ui.W, Ui.H))
        w.post { w.focusFirst() }
    }

    private fun closeWelcome() {
        welcome?.let { stage.removeView(it) }
        welcome = null
        countdown.focusDefault()
    }

    /** Settings → Apps: the row's apps, their order and which are hidden. */
    private var appsList: AppsScreen? = null

    private fun openApps() {
        if (appsList == null) {
            val a = AppsScreen(this, settings) { closeApps() }
            appsList = a
            stage.addView(a, FrameLayout.LayoutParams(Ui.W, Ui.H))
        }
        appsList?.post { appsList?.focusFirst() }
    }

    private fun closeApps() {
        appsList?.let { stage.removeView(it) }
        appsList = null
        options?.refresh()
        options?.focusFirst()
    }

    private fun openSettings() {
        if (options == null) {
            val s = SettingsScreen(this, settings, pictures, { openPictures(0) }, { openPreview() }, { openCity() }, { openApps() }) { closeSettings() }
            options = s
            stage.addView(s, FrameLayout.LayoutParams(Ui.W, Ui.H))
            countdown.visibility = View.INVISIBLE
        }
        options?.post { options?.focusFirst() }
    }

    private fun closeSettings() {
        options?.let { stage.removeView(it) }
        options = null
        countdown.visibility = View.VISIBLE
        countdown.reload()
        countdown.applySettings()
    }

    /** Settings → City: Automatic, or any town by name. */
    private fun openCity() {
        if (town == null) {
            val c = CityScreen(this, settings) { closeCity() }
            town = c
            stage.addView(c, FrameLayout.LayoutParams(Ui.W, Ui.H))
        }
        town?.post { town?.focusFirst() }
    }

    private fun closeCity() {
        town?.let { stage.removeView(it) }
        town = null
        options?.refresh()
        options?.focusFirst()
    }

    /** Back from the TV's Accessibility settings: the Home button's line as it is now. */
    override fun onResume() {
        super.onResume()
        options?.refresh()
    }

    /** The screensaver as it will look (DoomDream's own view), until Back or OK. */
    private fun openPreview() {
        if (preview != null) return
        val v = DoomDream.view(this).apply { isFocusable = true; isFocusableInTouchMode = true }
        preview = v
        (window.decorView as android.view.ViewGroup).addView(v, android.view.ViewGroup.LayoutParams(-1, -1))
        v.requestFocus()
        v.setOnKeyListener { _, code, e ->
            if (e.action == android.view.KeyEvent.ACTION_UP && (code == android.view.KeyEvent.KEYCODE_BACK || code == android.view.KeyEvent.KEYCODE_DPAD_CENTER || code == android.view.KeyEvent.KEYCODE_BUTTON_A)) { closePreview(); true } else true
        }
    }

    private fun closePreview() {
        preview?.let { (window.decorView as android.view.ViewGroup).removeView(it) }
        preview = null
        countdown.resumeLook()
        options?.focusFirst() ?: countdown.focusDefault()
    }

    override fun onBackPressed() {
        when {
            preview != null -> closePreview()
            town != null -> closeCity()
            links != null -> closePictures(0)
            options != null -> closeSettings()
            else -> super.onBackPressed()
        }
    }
}
