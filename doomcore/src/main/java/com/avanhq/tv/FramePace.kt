package com.avanhq.tv

import android.view.Choreographer

/**
 * One steady frame rate for a heavy screen. The Doomsday screen asks a Google TV's graphics for more
 * than it can draw sixty times a second; anything that redrew at 60 (the orb, the rolling
 * digits) dragged the whole screen to an uneven ~40 (9 Nov, measured). While [on], everything
 * that animates draws on the same frames, a steady 30 a second.
 */
object FramePace {
    @JvmStatic var on = false
    private var last = 0L

    /** Whether to draw on the frame at [frameTimeNanos]: every view asking in the same frame
     *  gets the same answer. */
    fun due(frameTimeNanos: Long): Boolean {
        if (!on || frameTimeNanos == last) return true
        if (frameTimeNanos - last >= 30_000_000L) { last = frameTimeNanos; return true }
        return false
    }

    /** Runs [frame] (0 to 1 over [ms]) on the paced frames, then [done]. */
    fun animate(ms: Long, frame: (Float) -> Unit, done: () -> Unit) {
        val start = System.nanoTime()
        Choreographer.getInstance().postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(now: Long) {
                val f = ((now - start) / (ms * 1e6)).toFloat().coerceIn(0f, 1f)
                if (f >= 1f) { frame(1f); done(); return }
                if (due(now)) frame(f)
                Choreographer.getInstance().postFrameCallback(this)
            }
        })
    }

    /** A view's own decelerating move, as ViewPropertyAnimator's default does it. */
    fun ease(f: Float) = 1f - (1f - f) * (1f - f)
}
