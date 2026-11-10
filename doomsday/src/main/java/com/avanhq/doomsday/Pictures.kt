package com.avanhq.doomsday

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import com.avanhq.tv.DoomFx
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * The four pictures (Hall, Mask, Tapestry, Throne): each slot keeps the link the user typed and the picture it gave, in the
 * app's own storage (files/pictures/<slot>). Nothing is fetched until Download is pressed;
 * nothing comes with the app (the film's pictures aren't ours to hand out). A picture that is
 * one of the four known pictures, recognised by its fingerprint, gets its measured places (DoomFx.KNOWN).
 */
class Pictures(private val context: Context) {
    class Slot(val key: String, val name: String)

    companion object {
        private const val MAX_PIXELS = 16_000_000L
        val SLOTS = listOf(Slot("hall", "Hall"), Slot("mask", "Mask"), Slot("tapestry", "Tapestry"), Slot("throne", "Throne"))
    }

    private val prefs = context.getSharedPreferences("pictures", Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun file(i: Int) = File(context.filesDir, "pictures/$i")

    fun has(i: Int) = file(i).length() > 0

    /** What the user typed for slot [i] ("" if nothing yet). */
    fun link(i: Int): String = prefs.getString("link$i", "") ?: ""

    /** Where the picture came from in the end ("example.com"), or "" for one copied in. */
    fun from(i: Int): String = prefs.getString("from$i", "") ?: ""

    fun setLink(i: Int, typed: String) = prefs.edit().putString("link$i", typed).apply()

    /** The picture for slot [i], fitted to the 1920 x 1080 screen, and its places. Off the main thread. */
    fun load(i: Int): Pair<Bitmap, DoomFx.Scene>? {
        val f = file(i)
        if (f.length() <= 0) return null
        val bytes = runCatching { f.readBytes() }.getOrNull() ?: return null
        val bmp = fit(bytes) ?: return null
        return bmp to DoomFx.placesFor(sha256(bytes))
    }

    fun remove(i: Int) {
        file(i).delete()
        prefs.edit().remove("from$i").apply()
    }

    /** Fetch what [typed] links to into slot [i]. [done] gets null, or the plain-words problem;
     *  a failed try keeps the picture that was there. */
    fun download(i: Int, typed: String, done: (String?) -> Unit) {
        setLink(i, typed)
        val link = Links.normalise(typed) ?: return done("Type a link first.")
        Links.problem(link)?.let { return done(it) }
        Thread {
            val problem = runCatching { fetch(i, link) }.getOrElse { "Couldn't reach it (${it.javaClass.simpleName})." }
            main.post { done(problem) }
        }.start()
    }

    private fun fetch(i: Int, link: String): String? {
        // A browser's name: some picture sites send nothing to an app that doesn't say one.
        val request = Request.Builder().url(link).header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Doomsday TV").build()
        client.newCall(request).execute().use { r ->
            val final = r.request.url.toString()
            Links.problem(final)?.let { return it }
            if (!r.isSuccessful) return Links.refused(r.code)
            val body = r.body ?: return "The site sent nothing."
            if (body.contentLength() > Links.MAX_BYTES) return "That picture is too big."
            val bytes = body.byteStream().use { input ->
                val buf = java.io.ByteArrayOutputStream()
                val chunk = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(chunk)
                    if (n < 0) break
                    buf.write(chunk, 0, n)
                    if (buf.size() > Links.MAX_BYTES) return "That picture is too big."
                }
                buf.toByteArray()
            }
            if (Links.kind(bytes.copyOf(minOf(16, bytes.size))) == null) return "That link isn't a picture (it's a web page or something else)."
            if (fit(bytes) == null) return "The TV couldn't open that picture."
            val f = file(i)
            f.parentFile?.mkdirs()
            val part = File(f.parentFile, "$i.part")
            part.writeBytes(bytes)
            if (!part.renameTo(f)) return "Couldn't save it on the TV."
            prefs.edit().putString("from$i", Links.host(final)).apply()
            return null
        }
    }

    /** The picture filling the screen (cut to 16:9 in the middle), decoded no bigger than needed. */
    private fun fit(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 1920 && bounds.outHeight / (sample * 2) >= 1080) sample *= 2
        // A very large or oddly shaped picture: shrink until it fits in ~16 million pixels, so a
        // huge download can't run the TV out of memory.
        while (bounds.outWidth.toLong() / sample * (bounds.outHeight.toLong() / sample) > MAX_PIXELS) sample *= 2
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        if (src.width == 1920 && src.height == 1080) return src
        val scale = maxOf(1920f / src.width, 1080f / src.height)
        val cw = (1920 / scale).toInt()
        val ch = (1080 / scale).toInt()
        val x = (src.width - cw) / 2
        val y = (src.height - ch) / 2
        val out = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, Rect(x, y, x + cw, y + ch), Rect(0, 0, 1920, 1080), Paint(Paint.FILTER_BITMAP_FLAG))
        src.recycle()
        return out
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
