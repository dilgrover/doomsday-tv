package com.avanhq.tv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.view.Choreographer
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The Doomsday screen's moving pictures. The light is the picture's own: its bright parts (windows,
 * torches, the beam) are lifted out when it loads and breathe, the torch flames are stretched
 * and swayed so they burn, mist drifts across in two layers, and in the hall green energy runs
 * through the carved lines of the floor circle. Doom's eyes glow in their slots, green fire
 * turns and crackles in his hands, his power rises by itself every 20-30 s, the camera pushes
 * in slowly, and when the remote moves the fire leaps to what's selected (DoomBolt, over the
 * words). Places are measured on the 1920 x 1080 pictures. Drawn at a steady 30 frames a
 * second (FramePace) with the soft light at a quarter size (DoomLight): a Google TV drew the first
 * version at an uneven 28.
 */
object DoomFx {
    /** One picture's places. torches: x, flame base, flame height. eyes: x, y, w, h.
     *  hands: x, y, radius. circle: cx, cy, rx, ry. feet: left, top, right, bottom (kept out of
     *  the circle's lines). beams: x, y, angle, width, length, warmth. */
    class Scene(
        val focus: FloatArray,
        val torches: List<FloatArray> = emptyList(),
        val eyes: List<FloatArray> = emptyList(),
        val hands: List<FloatArray> = emptyList(),
        val source: FloatArray,
        val circle: FloatArray? = null,
        val feet: FloatArray? = null,
        val beams: List<FloatArray> = emptyList(),
        val mist: Float = 1f,
        val mistTop: Float = 500f,
        val debris: Boolean = false,
        val motes: Boolean = true,
        val glow: Float = 1f,
        val push: Float = .05f,
        val crackle: Float = 1f,
        val eyeGlow: Float = 1f,
    )

    private fun f(vararg v: Float) = v

    // The four picture slots: 0 the hall, 1 the mask, 2 the tapestry, 3 the throne.
    val SCENES = listOf(
        Scene(
            focus = f(960f, 760f),
            torches = listOf(f(797f, 500f, 34f), f(1122f, 500f, 34f)),
            eyes = listOf(f(957f, 698f, 6f, 3f), f(971f, 698f, 6f, 3f)),
            hands = listOf(f(905f, 838f, 24f), f(1027f, 820f, 24f)),
            source = f(1027f, 820f),
            circle = f(960f, 1045f, 432f, 84f),
            feet = f(880f, 910f, 1050f, 1020f),
            beams = listOf(f(470f, -60f, -27f, 170f, 1150f, .6f), f(1450f, -60f, 27f, 170f, 1150f, .6f), f(962f, -40f, 0f, 120f, 760f, .8f)),
            mistTop = 560f,
        ),
        Scene(
            focus = f(920f, 300f),
            eyes = listOf(f(877f, 256f, 50f, 15f), f(962f, 245f, 56f, 19f)),
            source = f(920f, 252f),
            beams = listOf(f(1560f, -80f, 38f, 300f, 1400f, .5f)),
            mist = .45f,
            mistTop = 0f,
            debris = true,
            motes = false,
            glow = .35f,
        ),
        Scene(
            focus = f(960f, 640f),
            torches = listOf(f(638f, 626f, 46f), f(1277f, 626f, 46f)),
            hands = listOf(f(865f, 712f, 18f), f(1050f, 712f, 20f)),
            source = f(865f, 712f),
            beams = listOf(f(955f, -30f, 0f, 260f, 980f, 1f)),
            mist = .7f,
            mistTop = 620f,
        ),
        // The throne: a cinema-wide frame, scaled 1.25 and its top and bottom carried on into the dark.
        Scene(
            focus = f(935f, 470f),
            eyes = listOf(f(924f, 421f, 5f, 2f), f(938f, 421f, 5f, 2f)),
            hands = listOf(f(906f, 478f, 12f), f(970f, 476f, 12f)),
            source = f(970f, 476f),
            circle = f(960f, 838f, 400f, 58f),
            beams = listOf(f(1110f, -40f, -24f, 220f, 1050f, .55f), f(770f, -40f, 22f, 180f, 900f, .7f), f(935f, -30f, 0f, 110f, 560f, .85f)),
            mist = .8f,
            mistTop = 700f,
            // He sits far off: the camera goes further in towards him, his hands' power is
            // drawn at his size, and his eyes burn brighter so they read across the room.
            push = .12f,
            crackle = .5f,
            eyeGlow = 1.8f,
        ),
    )

    /** A picture the Doomsday app doesn't know (a link D or anyone pasted): nobody's eyes or
     *  hands to light, so only what comes from the picture itself: its own light breathing,
     *  mist, dust, the camera's push, Summon's rings low in the middle, the fire from below. */
    val ANY = Scene(focus = f(960f, 560f), source = f(960f, 1040f), mist = .8f, mistTop = 560f)

    /** The places in the hall drawn for an empty picture ([hall]). */
    val HALL = Scene(
        focus = f(960f, 600f),
        source = f(960f, 700f),
        circle = f(960f, 905f, 330f, 48f),
        beams = listOf(f(960f, -40f, 0f, 200f, 900f, .8f), f(560f, -40f, -18f, 140f, 900f, .5f), f(1360f, -40f, 18f, 140f, 900f, .5f)),
        mistTop = 640f,
    )

    /** Four known pictures, recognised by their SHA-256 once downloaded, so their measured places
     *  apply: the hall, the mask, the tapestry, the throne. Only fingerprints, never the pictures. */
    val KNOWN = mapOf(
        "4301fba5761b901c658b8837b8c881585a9b9a997589ed7f219509d72fa008a8" to 0,
        "794c92ff6ec432a3098d8ab5b6a73cdfd4c856b0a168359f2cab2d9f5c389d94" to 1,
        "3596f6d0cdd1e4148e1dc3222efa573dbe0cc4c710cfeee732bfe56058750eb2" to 2,
        "c2e59dd721cf1b3255ca70d8dda6cb7c43d58d30c42745f91e02d5a562c4a005" to 3,
    )

    /** The places for a picture with this fingerprint: measured if it's a known one, else [ANY]. */
    fun placesFor(sha256: String): Scene = KNOWN[sha256]?.let { SCENES[it] } ?: ANY

    /**
     * A hall drawn in code for a picture not there yet (no one's art, no Marvel): a dark
     * stone hall, five tall windows in green and gold, steps up to them, and a carved circle
     * on the floor, so the light, mist and Summon have something to live in.
     */
    fun hall(): Bitmap {
        val w = 1920
        val h = 1080
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(Color.rgb(6, 12, 9), Color.rgb(12, 22, 16), Color.rgb(4, 8, 6)), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        // The windows: tall panes with their lead lines, the middle one gold.
        val wins = listOf(Triple(370f, 240f, false), Triple(630f, 160f, false), Triple(960f, 60f, true), Triple(1290f, 160f, false), Triple(1550f, 240f, false))
        for ((x, top, gold) in wins) {
            val ww = if (gold) 150f else 90f
            val bottom = 640f
            val a = if (gold) Color.rgb(226, 178, 92) else Color.rgb(70, 190, 120)
            val b = if (gold) Color.rgb(120, 150, 70) else Color.rgb(20, 90, 55)
            p.shader = LinearGradient(0f, top, 0f, bottom, a, b, Shader.TileMode.CLAMP)
            c.drawRect(x - ww / 2, top, x + ww / 2, bottom, p)
            p.shader = null
            p.color = Color.rgb(8, 14, 10)
            c.drawRect(x - 3f, top, x + 3f, bottom, p)
            var y = top + 52f
            while (y < bottom) { c.drawRect(x - ww / 2, y - 3f, x + ww / 2, y + 3f, p); y += 52f }
        }
        // Steps up to the windows.
        for (i in 0 until 7) {
            val y = 640f + i * 22f
            val half = 380f + i * 40f
            p.color = Color.rgb(16 + i * 2, 24 + i * 2, 19 + i * 2)
            c.drawRect(960f - half, y, 960f + half, y + 22f, p)
            p.color = Color.argb(60, 200, 170, 110)
            c.drawRect(960f - half, y, 960f + half, y + 2f, p)
        }
        // The carved circle on the floor: rings and spokes, faintly lit.
        p.style = Paint.Style.STROKE
        p.color = Color.argb(90, 120, 220, 150)
        for (k in 1..4) {
            p.strokeWidth = if (k == 4) 4f else 2.5f
            c.drawOval(960f - 150f * k, 905f - 24f * k, 960f + 150f * k, 905f + 24f * k, p)
        }
        p.strokeWidth = 2f
        for (k in 0 until 12) {
            val ang = k * PI.toFloat() / 6
            c.drawLine(960f + cos(ang) * 150f, 905f + sin(ang) * 24f, 960f + cos(ang) * 600f, 905f + sin(ang) * 96f, p)
        }
        p.style = Paint.Style.FILL
        // Dark at the edges, as if lit only by the windows.
        p.shader = RadialGradient(960f, 520f, 1150f, intArrayOf(Color.TRANSPARENT, Color.argb(225, 0, 0, 0)), floatArrayOf(.35f, 1f), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        return bmp
    }

    /** The picture's light, lifted out at half size: its bright parts, softened, and for the
     *  hall the floor circle's carved lines in green. Built off the main thread. */
    class Maps(val glow: Bitmap, val floor: Bitmap?, val flames: List<Bitmap> = emptyList())

    fun maps(picture: Bitmap, scene: Scene): Maps {
        val w = 960
        val h = 540
        val half = Bitmap.createScaledBitmap(picture, w, h, true)
        val px = IntArray(w * h)
        half.getPixels(px, 0, w, 0, 0, w, h)
        if (half !== picture) half.recycle()
        val r = FloatArray(w * h)
        val g = FloatArray(w * h)
        val b = FloatArray(w * h)
        val lum = FloatArray(w * h)
        for (i in px.indices) {
            val c = px[i]
            val cr = (c shr 16 and 255) / 255f
            val cg = (c shr 8 and 255) / 255f
            val cb = (c and 255) / 255f
            val l = max(cr, max(cg, cb))
            lum[i] = l
            val m = smooth(.40f, .85f, l)
            r[i] = cr * m; g[i] = cg * m; b[i] = cb * m
        }
        for (ch in arrayOf(r, g, b)) repeat(3) { blur(ch, w, h, 3) }
        val glow = toBitmap(w, h) { i -> rgb(r[i] * 1.3f, g[i] * 1.3f, b[i] * 1.3f) }

        val floor = scene.circle?.let { c ->
            // The lines are the darker grooves between the lit stones: brighter round about
            // than here. Kept inside the circle's ellipse, and off Doom's own feet.
            val mean = lum.copyOf()
            repeat(2) { blur(mean, w, h, 3) }
            val cx = c[0] / 2; val cy = c[1] / 2; val rx = c[2] / 2 * 1.75f; val ry = c[3] / 2 * 1.9f
            val v = FloatArray(w * h)
            for (y in 0 until h) for (x in 0 until w) {
                val i = y * w + x
                val dx = (x - cx) / rx
                val dy = (y - cy) / ry
                val inside = 1f - smooth(.75f, 1f, sqrt(dx * dx + dy * dy))
                if (inside <= 0f) continue
                val feet = scene.feet?.let { f -> if (x * 2 in f[0].toInt()..f[2].toInt() && y * 2 in f[1].toInt()..f[3].toInt()) 0f else 1f } ?: 1f
                val groove = ((mean[i] - lum[i]) * 7f).coerceIn(0f, 1f) * smooth(.08f, .18f, mean[i])
                val stone = smooth(.14f, .34f, lum[i]) * .30f
                v[i] = ((groove + stone) * inside * feet).coerceIn(0f, 1f)
            }
            blur(v, w, h, 1)
            toBitmap(w, h) { i -> rgb(.25f * v[i], 1f * v[i], .58f * v[i]) }
        }
        // Each torch's flame, cut from the light with a soft teardrop edge: a plain box showed
        // as a bright rectangle where the wall behind is lit (the tapestry, 9 Nov on the TV).
        val flames = scene.torches.map { tp ->
            val x0 = ((tp[0] - tp[2]) / 2).toInt().coerceIn(0, w - 2)
            val y0 = ((tp[1] - tp[2] * 2.2f) / 2).toInt().coerceIn(0, h - 2)
            val x1 = ((tp[0] + tp[2]) / 2).toInt().coerceIn(x0 + 1, w)
            val y1 = ((tp[1] + tp[2] * .15f) / 2).toInt().coerceIn(y0 + 1, h)
            val fw = x1 - x0
            val fh = y1 - y0
            val cx = (tp[0] / 2 - x0) / fw
            toBitmap(fw, fh) { i ->
                val x = i % fw
                val y = i / fw
                val u = (x + .5f) / fw - cx
                val v = (y + .5f) / fh // 0 top (the tip) to 1 (the base)
                // Narrow at the tip, widest just above the base, gone at the edges.
                val half = .14f + .36f * smooth(0f, .8f, v)
                val across = 1f - smooth(.55f, 1f, kotlin.math.abs(u) / half)
                val along = smooth(0f, .35f, v) * (1f - smooth(.9f, 1f, v))
                val k = across * along
                val j = (y0 + y) * w + x0 + x
                Color.argb((k * 255).toInt(), (r[j] * 1.3f * 255).toInt().coerceIn(0, 255), (g[j] * 1.3f * 255).toInt().coerceIn(0, 255), (b[j] * 1.3f * 255).toInt().coerceIn(0, 255))
            }
        }
        return Maps(glow, floor, flames)
    }

    private fun rgb(r: Float, g: Float, b: Float) =
        Color.rgb((r.coerceIn(0f, 1f) * 255).toInt(), (g.coerceIn(0f, 1f) * 255).toInt(), (b.coerceIn(0f, 1f) * 255).toInt())

    private inline fun toBitmap(w: Int, h: Int, color: (Int) -> Int): Bitmap {
        val out = IntArray(w * h) { color(it) }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    fun smooth(a: Float, b: Float, x: Float): Float {
        val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
        return t * t * (3 - 2 * t)
    }

    /** A box blur of radius [r], across then down. */
    private fun blur(v: FloatArray, w: Int, h: Int, r: Int) {
        val tmp = FloatArray(v.size)
        val n = 2f * r + 1
        for (y in 0 until h) {
            val row = y * w
            var s = 0f
            for (x in -r..r) s += v[row + x.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                tmp[row + x] = s / n
                s += v[row + (x + r + 1).coerceAtMost(w - 1)] - v[row + (x - r).coerceAtLeast(0)]
            }
        }
        for (x in 0 until w) {
            var s = 0f
            for (y in -r..r) s += tmp[y.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                v[y * w + x] = s / n
                s += tmp[(y + r + 1).coerceAtMost(h - 1) * w + x] - tmp[(y - r).coerceAtLeast(0) * w + x]
            }
        }
    }

    /** Soft cloudy noise that tiles (white, its strength in the alpha), for mist and fire. */
    fun noise(size: Int, seed: Int, octaves: Int, contrast: Float): Bitmap {
        val rnd = Random(seed)
        val px = IntArray(size * size)
        val sum = FloatArray(size * size)
        var amp = 1f
        var total = 0f
        var cells = 4
        repeat(octaves) {
            val grid = FloatArray(cells * cells) { rnd.nextFloat() }
            fun at(i: Int, j: Int) = grid[(j % cells) * cells + (i % cells)]
            for (y in 0 until size) for (x in 0 until size) {
                val gx = x.toFloat() / size * cells
                val gy = y.toFloat() / size * cells
                val x0 = floor(gx).toInt(); val y0 = floor(gy).toInt()
                val fx = gx - x0; val fy = gy - y0
                val sx = fx * fx * (3 - 2 * fx); val sy = fy * fy * (3 - 2 * fy)
                val top = at(x0, y0) + (at(x0 + 1, y0) - at(x0, y0)) * sx
                val bottom = at(x0, y0 + 1) + (at(x0 + 1, y0 + 1) - at(x0, y0 + 1)) * sx
                sum[y * size + x] += (top + (bottom - top) * sy) * amp
            }
            total += amp
            amp *= .5f
            cells *= 2
        }
        for (i in px.indices) {
            val v = ((sum[i] / total - .5f) * contrast + .5f).coerceIn(0f, 1f)
            px[i] = Color.argb((v * v * 255).toInt(), 255, 255, 255)
        }
        return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
    }

    /** Broken stone and metal for the mask picture: six irregular pieces in a row (64 px
     *  each), dark with a lit upper edge. */
    fun debris(seed: Int): Bitmap {
        val rnd = Random(seed)
        val atlas = Bitmap.createBitmap(64 * 6, 64, Bitmap.Config.ARGB_8888)
        val into = Canvas(atlas)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f; strokeJoin = Paint.Join.ROUND }
        repeat(6) {
            val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val path = Path()
            val n = 4 + rnd.nextInt(3)
            for (i in 0 until n) {
                val ang = (i + rnd.nextFloat() * .6f) / n * 2 * PI
                val r = 14f + rnd.nextFloat() * 16f
                val x = 32f + cos(ang).toFloat() * r
                val y = 32f + sin(ang).toFloat() * r * (.45f + rnd.nextFloat() * .4f)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            val g = 26 + rnd.nextInt(18)
            fill.shader = LinearGradient(0f, 10f, 0f, 54f, Color.rgb(g + 22, g + 34, g + 18), Color.rgb(g - 14, g - 8, g - 16), Shader.TileMode.CLAMP)
            c.drawPath(path, fill)
            edge.color = Color.argb(150, 190, 230, 170)
            c.save(); c.clipRect(0f, 0f, 64f, 30f); c.drawPath(path, edge); c.restore()
            into.drawBitmap(bmp, it * 64f, 0f, null)
            bmp.recycle()
        }
        return atlas
    }

    /** A round glow, white in the middle, for tinting. */
    fun sprite(): Bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).also {
        Canvas(it).drawCircle(32f, 32f, 32f, Paint().apply { shader = RadialGradient(32f, 32f, 32f, intArrayOf(Color.WHITE, Color.argb(120, 255, 255, 255), Color.TRANSPARENT), floatArrayOf(0f, .3f, 1f), Shader.TileMode.CLAMP) })
    }
}

/** A value that flickers like a flame: it eases towards a new random level every so often. */
private class Flicker(private val rnd: Random, private val lo: Float, private val hi: Float, private val every: IntRange) {
    private var v = (lo + hi) / 2
    private var to = v
    private var next = 0L
    fun at(now: Long): Float {
        if (now >= next) { to = lo + rnd.nextFloat() * (hi - lo); next = now + every.first + rnd.nextInt(every.last - every.first) }
        v += (to - v) * .22f
        return v
    }
}

/** Tinted copies of one white sprite, one colour filter per colour (made once, not per frame:
 *  hundreds a frame kept the TV's memory cleaner busy and the motion stuttering). */
private class Tints {
    private val filters = HashMap<Int, PorterDuffColorFilter>()
    fun of(color: Int) = filters.getOrPut(color) { PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY) }
}

/** Many small textured quads (sparks, dust, glows, debris) in one draw call: drawn one by one,
 *  the TV's render thread spent ~19 ms a frame issuing them (9 Nov, measured). Each quad is
 *  tinted by its vertex colour; [blend] is how the lot lands on what's under it. */
private class Sprites(texture: Bitmap, private val max: Int, blend: PorterDuff.Mode) {
    private val verts = FloatArray(max * 12)
    private val tex = FloatArray(max * 12)
    private val cols = IntArray(max * 6)
    private var n = 0
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        shader = BitmapShader(texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        xfermode = PorterDuffXfermode(blend)
    }
    private val cell = texture.height.toFloat()

    /** A glow: [color] at [alpha], centred on x, y. */
    fun glow(color: Int, x: Float, y: Float, rx: Float, ry: Float, alpha: Float) =
        quad(color, alpha, 0, x, y, rx, ry, 1f, 0f)

    /** Cell [cell] of the texture (square cells in a row), turned by cos/sin, at [alpha]. */
    fun quad(color: Int, alpha: Float, cellAt: Int, x: Float, y: Float, rx: Float, ry: Float, c: Float, s: Float) {
        if (n >= max || alpha <= .004f) return
        val col = (color and 0xFFFFFF) or ((alpha.coerceAtMost(1f) * 255).toInt() shl 24)
        val u0 = cellAt * cell
        val u1 = u0 + cell
        // Corners: (-rx,-ry) (rx,-ry) (rx,ry) (-rx,ry), turned, as two triangles.
        val ax = -rx * c + ry * s; val ay = -rx * s - ry * c
        val bx = rx * c + ry * s; val by = rx * s - ry * c
        val o = n * 12
        put(o, x + ax, y + ay, u0, 0f); put(o + 2, x + bx, y + by, u1, 0f); put(o + 4, x - ax, y - ay, u1, cell)
        put(o + 6, x + ax, y + ay, u0, 0f); put(o + 8, x - ax, y - ay, u1, cell); put(o + 10, x - bx, y - by, u0, cell)
        cols.fill(col, n * 6, n * 6 + 6)
        n++
    }

    private fun put(i: Int, x: Float, y: Float, u: Float, v: Float) {
        verts[i] = x; verts[i + 1] = y; tex[i] = u; tex[i + 1] = v
    }

    fun draw(canvas: Canvas) {
        if (n > 0) canvas.drawVertices(Canvas.VertexMode.TRIANGLES, n * 12, verts, 0, tex, 0, cols, 0, null, 0, 0, paint)
        n = 0
    }
}

/**
 * The picture and what lives in it, under the words. The soft light (the picture's own glow,
 * the shafts, the mist, the torches' light on the walls, the floor circle) is drawn by
 * [DoomLight] at half size and added on top: the TV's graphics drew it at 28 frames a second
 * at full size (9 Nov, measured). The sharp things (flames, his hands, his eyes, sparks, dust,
 * debris) are drawn here at full size.
 */
class DoomStage(context: Context, scenes: List<DoomFx.Scene> = DoomFx.SCENES) : View(context) {
    companion object {
        /** How long OK is held to summon; then the floor ignites, and it settles. */
        const val HOLD_MS = 2000L
        const val IGNITE_MS = 900L
        const val SETTLE_MS = 4500L
    }

    // Each picture's places: the known four are measured; the Doomsday app's change as
    // its pictures are downloaded (a picture it doesn't know gets DoomFx.ANY).
    private val scenes = scenes.toMutableList()
    private val pictures = arrayOfNulls<Bitmap>(scenes.size)
    private val maps = arrayOfNulls<DoomFx.Maps>(scenes.size)
    var scene = 0
        private set
    private var previous = -1
    private var shownAt = 0L
    private var openedAt = 0L
    private var lastMove = System.currentTimeMillis()
    // Motion per frame is in 60ths of a second, times [step] (the frames are paced at 30).
    private var step = 1f
    private var lastT = 0L
    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(now: Long) {
            if (!isAttachedToWindow) return
            // A steady 30 a second: the TV's graphics can't hold 60 here, and an even 30
            // looks smoother than an uneven 40 (9 Nov, measured on a Chromecast with Google TV).
            if (FramePace.due(now)) invalidate() else Choreographer.getInstance().postFrameCallback(this)
        }
    }
    private var surgeAt = 0L
    private var nextPulse = 0L
    // The summoning (hold OK, D 10 Nov: "a reel worth theme"): his power gathers while it's
    // held, then the floor ignites ring by ring, a flash, the hall shakes, and it settles.
    private var chargeFrom = 0L
    private var chargeLeft = 0L
    private var chargeKept = 0f
    private var blastAt = 0L
    private val rnd = Random(16)
    var light: DoomLight? = null

    /** Calm (the Doomsday app's Effects setting, and its night hours): the light, mist and dust
     *  stay, the camera holds still and his power no longer rises by itself. */
    var calm = false

    // Where the picture sits: a slow drift closer to Doom (the same for everything drawn on it).
    val place = Matrix()

    // This frame, for the light drawn after it (DoomLight).
    internal var frameA = 0f
    internal var frameSurge = 0f
    internal var frameSecs = 0.0
    internal var frameT = 0L

    private val add = PorterDuffXfermode(PorterDuff.Mode.ADD)
    private val bmp = Paint(Paint.FILTER_BITMAP_FLAG)
    private val light2 = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = add }
    private val dot = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = add }
    private val tint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val bolt = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; xfermode = add }
    private val solid = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val tints = Tints()
    private val src = Rect()
    private val dst = RectF(0f, 0f, 1920f, 1080f)
    private val box = RectF()
    private val d = RectF()
    private val m = Matrix()
    private val path = Path()

    private val sprite = DoomFx.sprite()
    private val glows = Sprites(sprite, 420, PorterDuff.Mode.ADD)
    private val fire = DoomFx.noise(64, 21, 3, 2.2f)
    private val fireA = BitmapShader(fire, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    private val fireB = BitmapShader(fire, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    private val coreA = RadialGradient(0f, 0f, 1f, intArrayOf(Color.WHITE, Color.argb(150, 255, 255, 255), Color.TRANSPARENT), floatArrayOf(0f, .35f, 1f), Shader.TileMode.CLAMP)
    private val coreB = RadialGradient(0f, 0f, 1f, intArrayOf(Color.WHITE, Color.argb(150, 255, 255, 255), Color.TRANSPARENT), floatArrayOf(0f, .35f, 1f), Shader.TileMode.CLAMP)
    private val handA = ComposeShader(fireA, coreA, PorterDuff.Mode.DST_IN)
    private val handB = ComposeShader(fireB, coreB, PorterDuff.Mode.DST_IN)
    private val greenFire = PorterDuffColorFilter(Color.rgb(60, 255, 130), PorterDuff.Mode.SRC_IN)
    private val paleFire = PorterDuffColorFilter(Color.rgb(170, 255, 200), PorterDuff.Mode.SRC_IN)
    private val debrisArt = DoomFx.debris(7)
    private val pieces = Sprites(debrisArt, 80, PorterDuff.Mode.SRC_OVER)
    private val glints = Sprites(debrisArt, 40, PorterDuff.Mode.ADD)

    private val flames = List(4) { Flicker(rnd, .45f, 1f, 50..140) }
    private val sways = List(4) { Flicker(rnd, -1f, 1f, 90..260) }
    private val handFlick = List(2) { Flicker(rnd, .65f, 1f, 60..160) }

    // His power crackling round his hands: a few jagged arcs, redrawn every ~70 ms.
    private class Arc(val pts: FloatArray, var until: Long)
    private val arcs = List(6) { Arc(FloatArray(14), 0L) }
    private val arcLines = FloatArray(6 * 6 * 4)
    private var arcCount = 0

    private class Mote(var x: Float, var y: Float, val z: Float, val phase: Float)
    private val motes = List(90) { Mote(rnd.nextFloat() * 1920f, rnd.nextFloat() * 1080f, .3f + rnd.nextFloat() * .7f, rnd.nextFloat() * 6.28f) }

    private class Ember(var x: Float = 0f, var y: Float = 0f, var vx: Float = 0f, var vy: Float = 0f, var max: Float = 0f, var warm: Boolean = false, var seed: Float = 0f) { var life = 0f }
    private val embers = ArrayList<Ember>(160)
    private val spare = ArrayList<Ember>(160)

    private class Shard(var x: Float, var y: Float, val z: Float, var rot: Float, val spin: Float, var tumble: Float, val roll: Float, val kind: Int)
    private val shards = List(34) {
        val z = .2f + rnd.nextFloat() * .8f
        Shard(rnd.nextFloat() * 1920f, rnd.nextFloat() * 1080f, z * z, rnd.nextFloat() * 6f, (rnd.nextFloat() - .5f) * .05f, rnd.nextFloat() * 6f, .02f + rnd.nextFloat() * .06f, rnd.nextInt(6))
    }

    init {
        isFocusable = false
        isClickable = false
    }

    /** Picture [i] arrived: its light is lifted out (off the main thread), then it shows. */
    fun picture(i: Int, bitmap: Bitmap, places: DoomFx.Scene? = null) {
        if (places != null) scenes[i] = places
        pictures[i] = bitmap
        maps[i] = null
        val sc = scenes[i]
        Thread {
            val made = runCatching { DoomFx.maps(bitmap, sc) }.getOrNull()
            post { if (pictures[i] === bitmap) { maps[i] = made; invalidate() } }
        }.apply { priority = Thread.MIN_PRIORITY }.start()
        if (i == scene) { shownAt = System.currentTimeMillis(); invalidate() }
    }

    fun has(i: Int) = pictures[i] != null

    /** Picture [i] taken away (the Doomsday app's Remove). */
    fun forget(i: Int) {
        pictures[i] = null
        maps[i] = null
        if (i == scene) invalidate()
    }

    internal fun mapsNow(): DoomFx.Maps? = maps[scene]

    fun show(i: Int) {
        if (i == scene && shownAt > 0) return
        if (pictures[scene] != null && i != scene) previous = scene
        scene = i
        shownAt = if (pictures[i] != null) System.currentTimeMillis() else 0L
        invalidate()
    }

    /** The screen opened: in from black, nothing else. */
    fun open() {
        openedAt = System.currentTimeMillis()
        lastMove = openedAt
        nextPulse = openedAt + 9_000
        invalidate()
    }

    /** The remote moved: his fire flares, his eyes brighten, a wave runs round the floor. */
    fun surge() {
        surgeAt = System.currentTimeMillis()
        lastMove = surgeAt
        nextPulse = surgeAt + 20_000 + rnd.nextInt(10_000)
        invalidate()
    }

    /** OK held ([on]) or let go: his power gathers, or ebbs away if it wasn't enough. */
    fun charge(on: Boolean) {
        val t = System.currentTimeMillis()
        if (on && chargeFrom == 0L) { chargeFrom = t; lastMove = t; invalidate() }
        if (!on && chargeFrom != 0L) { chargeKept = chargeLevel(t); chargeLeft = t; chargeFrom = 0L }
    }

    /** Held long enough: the summoning goes off. */
    fun blast() {
        val t = System.currentTimeMillis()
        chargeFrom = 0L; chargeKept = 0f
        blastAt = t; lastMove = t
        nextPulse = t + 25_000
        invalidate()
    }

    /** How full the charge is, 0 to 1 over [HOLD_MS] (a short press shows nothing). */
    fun chargeLevel(t: Long = System.currentTimeMillis()): Float = when {
        chargeFrom != 0L -> ((t - chargeFrom - 250f) / (HOLD_MS - 250f)).coerceIn(0f, 1f)
        chargeKept > 0f -> (chargeKept * (1f - (t - chargeLeft) / 500f)).coerceAtLeast(0f)
        else -> 0f
    }

    /** Where the fire leaves Doom, in this view's 1920 x 1080 places. */
    fun source(): FloatArray {
        val p = scenes[scene].source.copyOf()
        place.mapPoints(p)
        return p
    }

    fun release() {
        pictures.fill(null)
        maps.fill(null)
        embers.clear()
        shownAt = 0L
        previous = -1
    }

    private fun surgeLevel(t: Long): Float {
        val a = (t - surgeAt) / 1000f
        return if (surgeAt == 0L || a > 1.6f) 0f else if (a < .15f) a / .15f else 1f - (a - .15f) / 1.45f
    }

    override fun onDraw(canvas: Canvas) {
        val t = System.currentTimeMillis()
        val k = width / 1920f
        canvas.drawColor(Color.BLACK)
        val pic = pictures[scene] ?: return
        val sc = scenes[scene]
        val secs = (t % 3_600_000L) / 1000.0
        step = if (lastT == 0L) 1f else ((t - lastT) / 16.67f).coerceIn(0f, 4f)
        lastT = t
        // In from black when the screen opens; a slow cross-fade between pictures.
        val open = if (Ui.still) 1f else DoomFx.smooth(0f, 1f, (t - openedAt) / 1400f)
        val fade = if (Ui.still || shownAt == 0L) 1f else DoomFx.smooth(0f, 1f, (t - shownAt) / 2000f)
        // The camera: a slow push in towards Doom and out again, drifting a little as it goes.
        val charge = chargeLevel(t)
        val bt = if (blastAt == 0L) -1L else t - blastAt
        if (bt > SETTLE_MS) blastAt = 0L
        val push = if (calm) 0f else sc.push * (.5f - .5f * cos(((t - shownAt) / 40_000.0) * PI).toFloat())
        val zoom = if (Ui.still) 1f else 1f + push + .03f * charge * charge
        place.setScale(zoom, zoom, sc.focus[0], sc.focus[1])
        if (!Ui.still && !calm) place.postTranslate(9f * sin(secs * .071).toFloat(), 5f * sin(secs * .053 + 1).toFloat())
        // The shockwave: the hall shakes as it goes off, and steadies.
        if (bt > IGNITE_MS) {
            val shake = 16f * exp(-(bt - IGNITE_MS) / 260f)
            place.postTranslate((rnd.nextFloat() - .5f) * 2 * shake, (rnd.nextFloat() - .5f) * 2 * shake)
        }
        // Now and then his power rises by itself (as on a move of the remote, without the bolt).
        if (!Ui.still && !calm && nextPulse in 1..t && openedAt > 0 && t - openedAt > 3000) {
            surgeAt = t
            nextPulse = t + 20_000 + rnd.nextInt(10_000)
        }

        canvas.save()
        canvas.scale(k, k)
        canvas.concat(place)
        val prev = pictures.getOrNull(previous)
        if (prev != null && fade < 1f) {
            bmp.alpha = (open * 255).toInt()
            canvas.drawBitmap(prev, null, dst, bmp)
        }
        bmp.alpha = (open * fade * 255).toInt()
        canvas.drawBitmap(pic, null, dst, bmp)
        if (fade >= 1f) previous = -1

        val a = open * fade
        // The hall darkens as he gathers it, until it goes off.
        val dim = if (bt in 0..IGNITE_MS) .45f else if (bt < 0) .45f * charge else 0f
        if (dim > 0f) { solid.color = Color.argb((dim * 255 * a).toInt(), 0, 0, 0); canvas.drawRect(dst, solid) }
        val blastPower = when { bt < 0 -> 0f; bt <= IGNITE_MS -> 1f; else -> (1f - (bt - IGNITE_MS).toFloat() / (SETTLE_MS - IGNITE_MS)).coerceAtLeast(0f) }
        val surge = max(surgeLevel(t), max(charge * .9f, blastPower))
        frameA = a; frameSurge = surge; frameSecs = secs; frameT = t
        val maps = maps[scene]
        if (maps != null && a > 0f) living(canvas, sc, maps, a, surge, secs, t)
        if (sc.motes) motes(canvas, a, secs)
        if (sc.debris) shards(canvas, a, secs)
        if (maps != null && a > 0f && (charge > 0f || bt >= 0)) summoning(canvas, sc, maps, a, charge, bt)
        embers(canvas, a)
        pieces.draw(canvas)
        glints.draw(canvas)
        glows.draw(canvas)
        canvas.restore()
        light?.invalidate()

        if (isAttachedToWindow && !Ui.still && t - lastMove < 30 * 60_000L) Choreographer.getInstance().postFrameCallback(frame)
    }

    /** The soft light, for [DoomLight]: drawn in its own (half size) canvas, added on top. */
    internal fun drawLight(canvas: Canvas, k: Float) {
        val maps = maps[scene] ?: return
        val a = frameA
        if (a <= 0f || pictures[scene] == null) return
        val sc = scenes[scene]
        val secs = frameSecs
        val surge = frameSurge
        canvas.save()
        canvas.scale(k, k)
        canvas.concat(place)
        lights.draw(canvas, sc, maps, a, surge, secs, frameT, scene)
        canvas.restore()
    }

    /** The summoning: sparks rising off the floor while it gathers, the floor's rings lighting
     *  one after another from the centre, then the flash, the windows blazing, embers flying. */
    private fun summoning(canvas: Canvas, sc: DoomFx.Scene, maps: DoomFx.Maps, a: Float, charge: Float, bt: Long) {
        val c = sc.circle ?: floatArrayOf(sc.focus[0], 1000f, 300f, 60f)
        val rx = c[2] * 1.75f
        val ry = c[3] * 1.9f
        if (charge > 0f && !Ui.still) {
            var n = charge * 2.5f * step
            while (n > 0f && (n >= 1f || rnd.nextFloat() < n)) {
                val ang = rnd.nextFloat() * 2 * PI.toFloat()
                val r = sqrt(rnd.nextFloat())
                ember(c[0] + cos(ang) * rx * r, c[1] + sin(ang) * ry * r, (rnd.nextFloat() - .5f) * .6f, -1.4f - rnd.nextFloat() * 2.4f * charge, 45f + rnd.nextFloat() * 50f, false)
                n -= 1f
            }
            // His eyes and hands lead it: a glow gathering round him.
            glowAt(canvas, Color.rgb(90, 255, 150), sc.source[0], sc.source[1], 60f + 160f * charge, a * .35f * charge)
        }
        if (bt < 0) return
        // Ignite: three rings run out across the floor, one after another.
        for (k in 0 until 3) {
            val age = (bt - k * 220L) / 700f
            if (age < 0f || age > 1f) continue
            val r = .08f + age * 1.07f
            val fadeR = (1f - age) * (if (age < .12f) age / .12f else 1f)
            val pts = 44
            for (j in 0 until pts) {
                val ang = j * 2 * PI.toFloat() / pts
                glowBox(canvas, Color.rgb(90, 255, 150), c[0] + cos(ang) * rx * r, c[1] + sin(ang) * ry * r, 34f + 30f * r, 12f + 10f * r, a * .75f * fadeR)
            }
        }
        if (bt < IGNITE_MS) return
        // The flash, and the windows blazing with it.
        val ft = (bt - IGNITE_MS).toFloat()
        val flash = if (ft < 90f) ft / 90f else exp(-(ft - 90f) / 520f)
        // The windows blaze in his green, not white.
        light2.alpha = (a * flash * .55f * 255).toInt()
        light2.colorFilter = summonTint
        canvas.drawBitmap(maps.glow, null, dst, light2)
        light2.colorFilter = null
        glowAt(canvas, Color.rgb(90, 255, 150), sc.focus[0], sc.focus[1], 1500f, a * .8f * flash)
        glowAt(canvas, Color.rgb(235, 255, 240), sc.source[0], sc.source[1], 420f, a * flash)
        if (ft < 40f && !Ui.still) repeat(70) {
            val ang = rnd.nextFloat() * 2 * PI.toFloat()
            val sp = 3f + rnd.nextFloat() * 9f
            ember(sc.source[0], sc.source[1], cos(ang) * sp, sin(ang) * sp * .6f - 2f, 40f + rnd.nextFloat() * 60f, rnd.nextFloat() < .3f)
        }
    }

    private val summonTint = android.graphics.LightingColorFilter(Color.rgb(120, 255, 160), 0)

    private val flameNow = FloatArray(4)
    private val lights = DoomLights(tints, sprite, flameNow)

    private fun living(canvas: Canvas, sc: DoomFx.Scene, maps: DoomFx.Maps, a: Float, surge: Float, secs: Double, t: Long) {
        // Torches: the flame itself stretched and swayed so it burns (its light: DoomLights).
        for (n in sc.torches.indices) {
            val tp = sc.torches[n]
            val f = flames[n].at(t)
            flameNow[n] = f
            val sway = sways[n].at(t)
            val x = tp[0]; val base = tp[1]; val h = tp[2]
            canvas.save()
            canvas.translate(x, base)
            canvas.skew(sway * .18f, 0f)
            canvas.scale(1f + (f - .7f) * .25f, .85f + f * .45f)
            canvas.translate(-x, -base)
            val flame = maps.flames.getOrNull(n)
            if (flame != null) {
                val x0 = ((x - h) / 2).toInt().toFloat() * 2f
                val y0 = ((base - h * 2.2f) / 2).toInt().toFloat() * 2f
                box.set(x0, y0, x0 + flame.width * 2f, y0 + flame.height * 2f)
                light2.alpha = (a * (.55f + .45f * f) * 255).toInt()
                canvas.drawBitmap(flame, null, box, light2)
            }
            canvas.restore()
            glowAt(canvas, Color.rgb(255, 200, 120), x, base - h * .45f, h * .9f, a * .35f * f)
            if (!Ui.still && rnd.nextFloat() < .22f * step) ember(x + (rnd.nextFloat() - .5f) * h * .4f, base - h * .8f, (rnd.nextFloat() - .5f) * .5f, -1.1f - rnd.nextFloat() * 1.3f, 40f + rnd.nextFloat() * 60f, true)
        }

        // Green fire turning in his hands, his power crackling round them.
        for (n in sc.hands.indices) {
            val hp = sc.hands[n]
            val fl = handFlick[n % 2].at(t)
            val r = hp[2] * (1f + surge * .9f) * (.9f + .15f * fl)
            for (layer in 0..1) {
                val fireShader = if (layer == 0) fireA else fireB
                val core = if (layer == 0) coreA else coreB
                m.setScale(r / 40f, r / 28f)
                m.postRotate(((secs * (if (layer == 0) 70 else -110) + n * 90) % 360).toFloat())
                m.postTranslate(hp[0], (hp[1] - (secs * (if (layer == 0) 55 else 90)) % 2000).toFloat())
                fireShader.setLocalMatrix(m)
                m.setScale(r * 1.6f, r * 1.6f)
                m.postTranslate(hp[0], hp[1] - r * .25f)
                core.setLocalMatrix(m)
                tint.shader = if (layer == 0) handA else handB
                tint.colorFilter = if (layer == 0) greenFire else paleFire
                tint.xfermode = add
                tint.alpha = (a * (if (layer == 0) .85f else .55f) * (.75f + .25f * fl + surge * .3f).coerceAtMost(1f) * 255).toInt()
                canvas.drawCircle(hp[0], hp[1] - r * .25f, r * 1.6f, tint)
            }
            tint.shader = null
            tint.colorFilter = null
            tint.xfermode = null
            glowAt(canvas, Color.rgb(220, 255, 230), hp[0], hp[1], r * .5f, a * (.45f + .3f * fl))
            crackle(canvas, n, hp[0], hp[1] - r * .2f, r * sc.crackle, a, surge, t)
            if (!Ui.still && rnd.nextFloat() < (.3f + surge) * step) ember(hp[0] + (rnd.nextFloat() - .5f) * r, hp[1] - r * .3f, (rnd.nextFloat() - .5f) * .9f, -.8f - rnd.nextFloat() * 1.6f, 25f + rnd.nextFloat() * 35f, false)
        }

        arcsDraw(canvas, a, sc.crackle)

        // His eyes: a low green light in the slots, rising now and then and on each move.
        val power = (t % 9000) / 9000f
        val rise = if (power > .82f) sin((power - .82f) / .18f * PI).toFloat() else 0f
        val e = (.45f + .12f * sin(secs * 1.7).toFloat() + .4f * rise + .5f * surge).coerceAtMost(1f)
        for (ep in sc.eyes) {
            val g = sc.eyeGlow
            glowBox(canvas, Color.rgb(80, 255, 140), ep[0], ep[1], ep[2] * 1.6f * g, ep[3] * 2.6f * g, (a * e * .55f * g).coerceAtMost(1f))
            glowBox(canvas, Color.rgb(200, 255, 220), ep[0], ep[1], ep[2] * .55f, ep[3] * .6f, a * e)
        }
    }

    /** Small jagged arcs leaping off his hand, new ones every ~70 ms; more while his power rises. */
    private fun crackle(canvas: Canvas, hand: Int, x: Float, y: Float, r: Float, a: Float, surge: Float, t: Long) {
        if (Ui.still) return
        for (i in 0 until 3) {
            val arc = arcs[hand * 3 + i]
            if (t >= arc.until) {
                // Some moments without: it crackles, it doesn't buzz.
                val live = rnd.nextFloat() < .55f + surge * .45f
                arc.until = t + 50 + rnd.nextInt(90)
                if (!live) { arc.pts[0] = Float.NaN; continue }
                val ang = rnd.nextFloat() * 2 * PI.toFloat()
                val len = r * (1.3f + rnd.nextFloat() * (1.6f + surge * 2f))
                for (j in 0 until 7) {
                    val q = j / 6f
                    val jit = if (j == 0) 0f else (rnd.nextFloat() - .5f) * r * .55f
                    arc.pts[j * 2] = x + cos(ang) * len * q - sin(ang) * jit
                    arc.pts[j * 2 + 1] = y + sin(ang) * len * q * .8f + cos(ang) * jit - q * q * r * .6f
                }
            }
            if (arc.pts[0].isNaN()) continue
            for (j in 0 until 6) {
                if (arcCount + 4 > arcLines.size) break
                arcLines[arcCount++] = arc.pts[j * 2]; arcLines[arcCount++] = arc.pts[j * 2 + 1]
                arcLines[arcCount++] = arc.pts[j * 2 + 2]; arcLines[arcCount++] = arc.pts[j * 2 + 3]
            }
        }
    }

    /** All the arcs of this frame in two line draws (a wide faint one, a thin bright one). */
    private fun arcsDraw(canvas: Canvas, a: Float, size: Float) {
        if (arcCount == 0) return
        bolt.strokeWidth = 6f * size; bolt.color = Color.argb((a * 60).toInt(), 60, 255, 130)
        canvas.drawLines(arcLines, 0, arcCount, bolt)
        bolt.strokeWidth = 1.6f * (.4f + .6f * size); bolt.color = Color.argb((a * 220).toInt(), 215, 255, 230)
        canvas.drawLines(arcLines, 0, arcCount, bolt)
        arcCount = 0
    }

    /** Dust hanging in the light, drifting. */
    private fun motes(canvas: Canvas, a: Float, secs: Double) {
        for (mo in motes) {
            if (!Ui.still) {
                mo.x += ((.12f + .25f * mo.z) * sin(secs * .2 + mo.phase).toFloat() + .05f) * step
                mo.y += (-.05f * mo.z + .12f * sin(secs * .37 + mo.phase * 2).toFloat()) * step
                if (mo.x > 1940f) mo.x = -20f
                if (mo.x < -20f) mo.x = 1940f
                if (mo.y < -20f) mo.y = 1100f
                if (mo.y > 1100f) mo.y = -20f
            }
            val tw = .5f + .5f * sin(secs * (1 + mo.z) + mo.phase).toFloat()
            glowAt(canvas, Color.rgb(255, 236, 190), mo.x, mo.y, 1.5f + 3.5f * mo.z, a * (.12f + .38f * tw) * mo.z)
        }
    }

    /** The mask picture: broken pieces blown past and tumbling, near ones bigger, faster and
     *  softer (out of focus), catching the green light as they turn. */
    private fun shards(canvas: Canvas, a: Float, secs: Double) {
        for (s in shards) {
            if (!Ui.still) {
                s.x += (.4f + 2.6f * s.z) * step
                s.y -= (.2f + .8f * s.z + .25f * sin(secs + s.kind).toFloat()) * step
                s.rot += s.spin * step
                s.tumble += s.roll * step
                if (s.x > 2000f) { s.x = -80f; s.y = 200f + rnd.nextFloat() * 1000f }
                if (s.y < -80f) { s.y = 1160f; s.x = rnd.nextFloat() * 1920f }
            }
            val face = cos(s.tumble)
            val glint = max(0f, sin(s.tumble * 2f + 1f)).let { it * it * it * it }
            val size = (10f + 46f * s.z) / 2
            val c = cos(s.rot); val sn = sin(s.rot)
            val rx = size * (.25f + .75f * kotlin.math.abs(face))
            // Out of focus up close: a second, slightly bigger and fainter copy round it.
            if (s.z > .6f) pieces.quad(Color.WHITE, a * .45f, s.kind, s.x, s.y, rx * 1.12f, size * 1.12f, c, sn)
            pieces.quad(Color.WHITE, a * (if (s.z > .6f) .6f else .92f), s.kind, s.x, s.y, rx, size, c, sn)
            if (glint > .05f) glints.quad(Color.rgb(170, 255, 190), a * glint * .8f, s.kind, s.x, s.y, rx, size, c, sn)
        }
    }

    private fun ember(x: Float, y: Float, vx: Float, vy: Float, max: Float, warm: Boolean) {
        if (embers.size >= 150) return
        val e = if (spare.isEmpty()) Ember() else spare.removeAt(spare.size - 1)
        e.x = x; e.y = y; e.vx = vx; e.vy = vy; e.max = max; e.warm = warm; e.seed = rnd.nextFloat() * 50f; e.life = 0f
        embers.add(e)
    }

    private fun embers(canvas: Canvas, a: Float) {
        var i = 0
        while (i < embers.size) {
            val e = embers[i]
            e.life += step
            if (e.life > e.max) { embers.removeAt(i); if (spare.size < 160) spare.add(e); continue }
            val k = e.life / e.max
            e.x += (e.vx + sin(e.seed + e.life * .21f) * .5f) * step
            e.y += e.vy * step
            e.vy *= 1f - .005f * step
            val col = if (e.warm) Color.rgb(255, 190, 110) else Color.rgb(140, 255, 180)
            glowAt(canvas, col, e.x, e.y, (if (e.warm) 3.2f else 4f) * (1f - k * .5f), a * (1f - k) * .9f)
            i++
        }
    }

    private fun glowAt(canvas: Canvas, color: Int, x: Float, y: Float, r: Float, alpha: Float) = glowBox(canvas, color, x, y, r, r, alpha)

    @Suppress("UNUSED_PARAMETER")
    private fun glowBox(canvas: Canvas, color: Int, x: Float, y: Float, rx: Float, ry: Float, alpha: Float) =
        glows.glow(color, x, y, rx, ry, alpha)
}

/** The soft light of the Doomsday picture (its glow, shafts, mist, torchlight, floor circle),
 *  drawn into [DoomLight]'s half-size layer. Shaders made once and moved, not made per frame. */
private class DoomLights(private val tints: Tints, sprite: Bitmap, private val flames: FloatArray) {
    private val glows = Sprites(sprite, 16, PorterDuff.Mode.ADD)
    private val add = PorterDuffXfermode(PorterDuff.Mode.ADD)
    private val light = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = add }
    private val tint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply { xfermode = add }
    private val dot = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = add }
    private val dst = RectF(0f, 0f, 1920f, 1080f)
    private val d = RectF()
    private val m = Matrix()

    // A soft shaft of light, 64 wide and 256 long: bright down its middle, fading at its far end.
    private val beam = Bitmap.createBitmap(64, 256, Bitmap.Config.ARGB_8888).also {
        val px = IntArray(64 * 256)
        for (y in 0 until 256) for (x in 0 until 64) {
            val across = exp(-((x - 31.5f) / 13f).let { q -> q * q })
            val along = DoomFx.smooth(0f, .12f, y / 255f) * (1f - DoomFx.smooth(.35f, 1f, y / 255f))
            px[y * 64 + x] = Color.argb((across * along * 255).toInt(), 255, 255, 255)
        }
        it.setPixels(px, 0, 64, 0, 0, 64, 256)
    }
    private val warmBeam = PorterDuffColorFilter(Color.rgb(255, 214, 140), PorterDuff.Mode.SRC_IN)
    private val greenBeam = PorterDuffColorFilter(Color.rgb(200, 255, 170), PorterDuff.Mode.SRC_IN)
    private val greenMist = PorterDuffColorFilter(Color.rgb(150, 210, 140), PorterDuff.Mode.SRC_IN)
    private val warmMist = PorterDuffColorFilter(Color.rgb(210, 180, 130), PorterDuff.Mode.SRC_IN)

    private val mist = listOf(DoomFx.noise(128, 3, 4, 1.6f), DoomFx.noise(128, 8, 4, 1.8f), DoomFx.noise(128, 13, 3, 1.4f))
        .map { BitmapShader(it, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) }
    private var mistTop = Float.NaN
    private var mistLayers: List<ComposeShader> = emptyList()

    private var floorOf: Bitmap? = null
    private var floorShader: BitmapShader? = null
    private var floorSweep: ComposeShader? = null
    private val sweep = SweepGradient(0f, 0f, intArrayOf(Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT, Color.TRANSPARENT, Color.WHITE), floatArrayOf(0f, .025f, .28f, .5f, .525f, .78f, 1f))

    fun draw(canvas: Canvas, sc: DoomFx.Scene, maps: DoomFx.Maps, a: Float, surge: Float, secs: Double, t: Long, scene: Int) {
        // The picture's own light breathing (the windows, the haze, the beam), up as his power rises.
        light.alpha = (a * sc.glow * (.17f + .08f * sin(secs * .7).toFloat() + .05f * sin(secs * 1.9).toFloat() + .22f * surge) * 255).toInt()
        canvas.drawBitmap(maps.glow, null, dst, light)

        // Shafts of light from above, slowly brightening and fading, with a little sway.
        for (n in sc.beams.indices) {
            val b = sc.beams[n]
            val breath = .5f + .5f * sin(secs * (.23 + n * .07) + n * 2.1).toFloat()
            tint.colorFilter = if (b[5] > .7f) warmBeam else greenBeam
            tint.alpha = (a * (.06f + .11f * breath) * 255).toInt()
            m.reset()
            m.setScale(b[3] / 64f, b[4] / 256f)
            m.postTranslate(-b[3] / 2, 0f)
            m.postRotate(b[2] + sin(secs * .11 + n).toFloat() * 1.5f)
            m.postTranslate(b[0], b[1])
            canvas.drawBitmap(beam, m, tint)
        }
        tint.colorFilter = null

        // Mist rolling across: three layers at their own pace, the nearest lowest and quickest.
        if (sc.mist > 0f) {
            val top = sc.mistTop
            if (top != mistTop) {
                mistTop = top
                val fadeIn = LinearGradient(0f, top, 0f, top + 360f, Color.TRANSPARENT, Color.WHITE, Shader.TileMode.CLAMP)
                mistLayers = mist.map { ComposeShader(it, fadeIn, PorterDuff.Mode.DST_IN) }
            }
            for (n in mist.indices) {
                val scale = when (n) { 0 -> 7.5f; 1 -> 4.5f; else -> 10f }
                val speed = when (n) { 0 -> 16f; 1 -> -26f; else -> 38f }
                m.setScale(scale, scale * .5f)
                m.postTranslate((secs * speed).toFloat() % (128 * scale), (secs * (if (n == 1) 2 else -3)).toFloat() % (128 * scale * .5f) + (if (n == 2) 260f else 0f))
                mist[n].setLocalMatrix(m)
                tint.shader = mistLayers[n]
                tint.colorFilter = if (scene == 2) warmMist else greenMist
                tint.alpha = (a * sc.mist * (when (n) { 0 -> .22f; 1 -> .15f; else -> .18f }) * 255).toInt()
                canvas.drawRect(-60f, top, 1980f, 1140f, tint)
            }
            tint.shader = null
            tint.colorFilter = null
        }

        // The torches' light on the walls round them, flickering with the flame.
        for (n in sc.torches.indices) {
            val tp = sc.torches[n]
            val f = flames[n]
            glow(canvas, Color.rgb(255, 150, 60), tp[0], tp[1] - tp[2] * .5f, tp[2] * 8f, a * (.07f + .13f * f))
        }

        // The hall floor: green energy running through the carved circle, a wave on each surge.
        val c = sc.circle
        val floor = maps.floor
        if (c != null && floor != null) {
            if (floor !== floorOf) {
                floorOf = floor
                floorShader = BitmapShader(floor, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(Matrix().apply { setScale(2f, 2f) }) }
                floorSweep = ComposeShader(floorShader!!, sweep, PorterDuff.Mode.DST_IN)
            }
            light.alpha = (a * (.5f + .15f * sin(secs * 1.3).toFloat() + .4f * surge).coerceAtMost(1f) * 255).toInt()
            canvas.drawBitmap(floor, null, dst, light)
            m.setRotate((secs * 52 % 360).toFloat())
            m.postScale(1f, c[3] / c[2])
            m.postTranslate(c[0], c[1])
            sweep.setLocalMatrix(m)
            tint.shader = floorSweep
            tint.alpha = (a * .95f * 255).toInt()
            canvas.drawRect(c[0] - c[2] * 1.8f, c[1] - c[3] * 2f, c[0] + c[2] * 1.8f, 1080f, tint)
            if (surge > 0f) {
                val age = 1f - surge
                val r0 = .05f + age * .95f
                val ring = RadialGradient(0f, 0f, 1f, intArrayOf(Color.TRANSPARENT, Color.WHITE, Color.TRANSPARENT), floatArrayOf((r0 - .16f).coerceAtLeast(0f), r0, (r0 + .04f).coerceAtMost(1f)), Shader.TileMode.CLAMP)
                m.setScale(c[2] * 1.8f, c[3] * 1.9f)
                m.postTranslate(c[0], c[1])
                ring.setLocalMatrix(m)
                tint.shader = ComposeShader(floorShader!!, ring, PorterDuff.Mode.DST_IN)
                tint.alpha = (a * surge * 255).toInt()
                canvas.drawRect(c[0] - c[2] * 1.8f, c[1] - c[3] * 2f, c[0] + c[2] * 1.8f, 1080f, tint)
            }
            tint.shader = null
        }

        // His hands lighting what's round them, more as his power rises.
        for (hp in sc.hands) glow(canvas, Color.rgb(90, 255, 150), hp[0], hp[1], hp[2] * (4f + 3f * surge), a * (.14f + .3f * surge))
        glows.draw(canvas)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun glow(canvas: Canvas, color: Int, x: Float, y: Float, r: Float, alpha: Float) = glows.glow(color, x, y, r, r, alpha)
}

/** [DoomStage]'s soft light at half size, scaled up and added over the picture: a quarter of
 *  the drawing for light that has no sharp edges anyway. */
class DoomLight(context: Context, private val stage: DoomStage) : View(context) {
    init {
        isFocusable = false
        isClickable = false
        pivotX = 0f
        pivotY = 0f
        scaleX = SCALE.toFloat()
        scaleY = SCALE.toFloat()
        setLayerType(LAYER_TYPE_HARDWARE, Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD) })
        stage.light = this
    }

    override fun onDraw(canvas: Canvas) {
        if (width > 0) stage.drawLight(canvas, width / 1920f)
    }

    companion object {
        const val SCALE = 4 // drawn at a quarter of the width and height (480 x 270)
    }
}


/** Over the words: when the remote moves, green fire leaps from Doom's hand to what's selected,
 *  crackling on the way, and that tile glows for a moment. */
class DoomBolt(context: Context, private val stage: DoomStage) : View(context) {
    /** What the fire goes round, not through (the countdown), in 1920 x 1080 places. */
    var avoid: RectF? = null
    private var from = FloatArray(2)
    private var to = FloatArray(2)
    private var bend = FloatArray(2)
    private val box = RectF()
    private var at = 0L
    private val rnd = Random(4)
    private val add = PorterDuffXfermode(PorterDuff.Mode.ADD)
    private val dot = Paint(Paint.FILTER_BITMAP_FLAG).apply { xfermode = add }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; xfermode = add }
    private val path = Path()
    private val d = RectF()
    private val p = FloatArray(2)
    private val sprite = DoomFx.sprite()
    private val tints = Tints()

    init {
        isFocusable = false
        isClickable = false
    }

    fun fire(v: View) {
        if (Ui.still || width == 0) return
        val a = IntArray(2)
        val b = IntArray(2)
        getLocationOnScreen(a)
        v.getLocationOnScreen(b)
        val k = width / 1920f
        val w = v.width * v.scaleX
        val h = v.height * v.scaleY
        // Into this view's 1920 x 1080 places.
        box.set((b[0] - a[0]) / k, (b[1] - a[1]) / k, (b[0] - a[0] + w) / k, (b[1] - a[1] + h) / k)
        to = floatArrayOf(box.centerX(), box.centerY())
        from = stage.source()
        // Arc up and over, a little different each time.
        val mx = (from[0] + to[0]) / 2
        val my = (from[1] + to[1]) / 2
        bend = floatArrayOf(mx + (rnd.nextFloat() - .5f) * 160f, min(from[1], to[1]) - 80f - rnd.nextFloat() * 120f)
        if (my < bend[1]) bend[1] = my
        // Through the countdown: swing low under it instead.
        if (crosses()) bend = floatArrayOf(mx + (rnd.nextFloat() - .5f) * 80f, max(from[1], to[1]) + 150f + rnd.nextFloat() * 60f)
        at = System.currentTimeMillis()
        invalidate()
    }

    private fun crosses(): Boolean {
        val r = avoid ?: return false
        for (i in 1..23) {
            on(i / 24f, p)
            if (r.contains(p[0], p[1])) return true
        }
        return false
    }

    private fun on(t: Float, out: FloatArray) {
        val u = 1 - t
        out[0] = u * u * from[0] + 2 * u * t * bend[0] + t * t * to[0]
        out[1] = u * u * from[1] + 2 * u * t * bend[1] + t * t * to[1]
    }

    override fun onDraw(canvas: Canvas) {
        if (at == 0L) return
        val age = System.currentTimeMillis() - at
        val travel = 480f
        if (age > 1500) { at = 0L; return }
        val k = width / 1920f
        canvas.save()
        canvas.scale(k, k)
        val head = min(1f, age / travel).let { 1 - (1 - it) * (1 - it) }
        if (age < travel + 260) {
            // The crackling filament: drawn afresh each frame with new jitter.
            val tail = max(0f, head - .55f)
            val fadeOut = if (age > travel) 1f - (age - travel) / 260f else 1f
            path.reset()
            val steps = 26
            for (i in 0..steps) {
                on(tail + (head - tail) * i / steps, p)
                val j = if (i == 0 || i == steps) 0f else 14f
                val x = p[0] + (rnd.nextFloat() - .5f) * j
                val y = p[1] + (rnd.nextFloat() - .5f) * j
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            line.strokeWidth = 10f; line.color = Color.argb((70 * fadeOut).toInt(), 60, 255, 130)
            canvas.drawPath(path, line)
            line.strokeWidth = 2.6f; line.color = Color.argb((235 * fadeOut).toInt(), 210, 255, 225)
            canvas.drawPath(path, line)
            // A glowing trail along it, brightest at the head.
            for (i in 0 until 18) {
                val tt = head - i * .03f
                if (tt < tail) break
                on(tt, p)
                glow(canvas, Color.rgb(80, 255, 150), p[0], p[1], 26f - i, (.55f - i * .028f) * fadeOut)
            }
            on(head, p)
            glow(canvas, Color.rgb(230, 255, 240), p[0], p[1], 18f, .9f * fadeOut)
        }
        if (age > travel * .85f) {
            // It lands: a flash, then the tile glows and fades.
            val s = (age - travel * .85f) / (1500 - travel * .85f)
            val flash = max(0f, 1f - s * 3f)
            glow(canvas, Color.rgb(200, 255, 220), to[0], to[1], 70f + 90f * s, flash * .7f)
            dot.colorFilter = tints.of(Color.rgb(70, 255, 140))
            dot.alpha = ((1f - s) * .45f * 255).toInt()
            d.set(box.left - 60f, box.top - 46f, box.right + 60f, box.bottom + 46f)
            canvas.drawBitmap(sprite, null, d, dot)
            dot.colorFilter = null
        }
        canvas.restore()
        postInvalidateOnAnimation()
    }

    private fun glow(canvas: Canvas, color: Int, x: Float, y: Float, r: Float, alpha: Float) {
        dot.colorFilter = tints.of(color)
        dot.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
        d.set(x - r, y - r, x + r, y + r)
        canvas.drawBitmap(sprite, null, d, dot)
        dot.colorFilter = null
    }
}
