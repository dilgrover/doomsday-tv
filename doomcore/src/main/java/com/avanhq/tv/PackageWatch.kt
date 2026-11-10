package com.avanhq.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * An app installed, updated or removed: the apps row shows it, and its new banner, straight
 * away. Several changes close together (an update is a removal and an addition) make one
 * refresh, a second after the last ([Debounce]).
 */
object PackageWatch {
    fun register(context: Context, onChange: () -> Unit): BroadcastReceiver {
        val handler = Handler(Looper.getMainLooper())
        val later = Debounce(1_000, { r, ms -> handler.postDelayed(r, ms) }, { handler.removeCallbacks(it) }, onChange)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) = later.poke()
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        else context.registerReceiver(receiver, filter)
        return receiver
    }
}

/** Runs [action] once, [delayMs] after the last [poke]: many pokes close together make one run. */
class Debounce(
    private val delayMs: Long,
    private val schedule: (Runnable, Long) -> Unit,
    private val unschedule: (Runnable) -> Unit,
    private val action: () -> Unit,
) {
    private val run = Runnable { action() }

    fun poke() {
        unschedule(run)
        schedule(run, delayMs)
    }
}
