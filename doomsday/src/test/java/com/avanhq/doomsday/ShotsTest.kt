package com.avanhq.doomsday

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.avanhq.tv.Ui
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

/** The app's screens drawn off the TV (one still frame each), for looking at a change without
 *  the TV: build/shots/ (or -Dshots.dir). The countdown shows the drawn hall: no pictures here. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1920dp-h1080dp-land-mdpi")
class ShotsTest {
    private val out = File(System.getProperty("shots.dir") ?: "build/shots").apply { mkdirs() }

    /** The view showing [text] (a chip, a link box), for pressing it without the TV's focus. */
    private fun find(v: View, text: String): View? {
        if (v is TextView && v.text.toString().trim().endsWith(text)) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i), text)?.let { return it }
        return null
    }

    private fun shot(name: String, intent: Intent = Intent(), press: String? = null, focus: String? = null, act: (View) -> Unit = {}) {
        try {
            val controller = Robolectric.buildActivity(MainActivity::class.java, intent.setClassName("com.avanhq.doomsday", "com.avanhq.doomsday.MainActivity")).setup()
            val activity = controller.get()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1500))
            // Robolectric's window has no focus, so the remote's keys go nowhere: focus or press directly.
            focus?.let { find(activity.window.decorView, it)?.apply { isFocusableInTouchMode = true; requestFocus() } }
            press?.let { t ->
                find(activity.window.decorView, t)?.let { v ->
                    v.requestFocus()
                    v.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
                    v.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
                }
            }
            act(activity.window.decorView)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            val bmp = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
            activity.window.decorView.draw(Canvas(bmp))
            File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            controller.pause().stop().destroy()
        } catch (e: Throwable) {
            File(out, "$name.error.txt").writeText(e.stackTraceToString())
        }
    }

    @Test
    fun screens() {
        Ui.still = true
        // The first-start question, then answered (so it doesn't cover the other screens).
        shot("doomsday-app-welcome")
        org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("home_asked", true).commit()
        shot("doomsday-app")
        // Settings → Apps, and the menu from holding OK on an app (made-up apps, off the TV).
        fakeApps()
        shot("doomsday-app-apps", Intent().putExtra("apps", true))
        shot("doomsday-app-appmenu", Intent().putExtra("appmenu", "com.netflix.ninja"))
        // The bottom row with the remote on it (one step right from the chosen picture).
        shot("doomsday-app-chips", focus = "Mask")
        // Pictures with the keyboard open, part way through typing a short link.
        shot(
            "doomsday-app-keyboard",
            Intent().putExtra("pictures", 0).putExtra("type0", "https://tinyurl.com/my-ha"),
            press = "my-ha",
        )
        // Pictures, as it first opens.
        shot("doomsday-app-pictures", Intent().putExtra("pictures", 0))
        // Settings, with the town found automatically and the Home button's line.
        shot("doomsday-app-settings", Intent().putExtra("settings", true))
        // City: what a search for Richmond finds (the answer given here, no network off the TV).
        shot("doomsday-app-city", Intent().putExtra("city", true)) { root ->
            fun city(v: View): CityScreen? = v as? CityScreen ?: (v as? ViewGroup)?.let { g -> (0 until g.childCount).firstNotNullOfOrNull { city(g.getChildAt(it)) } }
            city(root)?.show("Richmond", Geo.results("""{"results":[
                {"name":"Richmond","latitude":51.461,"longitude":-0.303,"country":"United Kingdom","admin1":"England"},
                {"name":"Richmond","latitude":37.55,"longitude":-77.46,"country":"United States","admin1":"Virginia"},
                {"name":"Richmond","latitude":-33.6,"longitude":150.75,"country":"Australia","admin1":"New South Wales"}]}"""))
        }
    }

    /** A few apps for the row and Settings → Apps (Robolectric has none installed). */
    private fun fakeApps() {
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        val pm = shadowOf(app.packageManager)
        val tv = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val colours = listOf(0xFFE53935, 0xFF111111, 0xFF1A98FF, 0xFFEC008C, 0xFFD6E900, 0xFF1DB954, 0xFF6F2DBD)
        for ((n, p) in listOf(
            "com.google.android.youtube.tv" to "YouTube", "com.netflix.ninja" to "Netflix", "com.amazon.amazonvideo.livingroom" to "Prime Video",
            "uk.co.bbc.iplayer" to "BBC iPlayer", "air.ITVMobilePlayer" to "ITVX", "com.spotify.tv.android" to "Spotify", "com.example.tv" to "Another app",
        ).withIndex()) {
            val ai = android.content.pm.ActivityInfo().apply {
                packageName = p.first
                name = "${p.first}.Main"
                nonLocalizedLabel = p.second
                applicationInfo = android.content.pm.ApplicationInfo().apply { packageName = p.first }
            }
            val ri = android.content.pm.ResolveInfo().apply { activityInfo = ai; nonLocalizedLabel = p.second }
            pm.addResolveInfoForIntent(tv, ri)
            pm.addDrawableResolution(p.first, 0, android.graphics.drawable.ColorDrawable(colours[n].toInt()))
        }
    }
}
