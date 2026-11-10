package com.avanhq.doomsday

import java.net.URI

/**
 * What a typed picture link means, and whether the TV may fetch it. Plain functions, so they're tested on the JVM (LinksTest).
 */
object Links {
    const val TINY = "https://tinyurl.com/"
    const val BITLY = "https://bit.ly/"

    /** The most a picture may weigh: a 4K JPEG is a few MB. */
    const val MAX_BYTES = 25L * 1024 * 1024

    private val CODE = Regex("^[A-Za-z0-9_-]{3,40}$")

    /** The link to fetch for what was typed, or null if there's nothing to fetch. */
    fun normalise(typed: String): String? {
        val t = typed.trim()
        if (t.isEmpty() || t == TINY || t == BITLY || t == "https://" || t == "http://") return null
        if (CODE.matches(t)) return TINY + t
        if (t.startsWith("https://") || t.startsWith("http://")) return t
        return "https://$t"
    }

    /** Why the TV won't fetch [link] (or the picture it ended up at), or null if it may:
     *  https anywhere, plain http only inside the home. */
    fun problem(link: String): String? {
        val uri = runCatching { URI(link) }.getOrNull() ?: return "That doesn't look like a link."
        val host = uri.host ?: return "That doesn't look like a link."
        return when (uri.scheme?.lowercase()) {
            "https" -> null
            "http" -> if (isHome(host)) null else "Only https links, except from a computer at home."
            else -> "Only https links."
        }
    }

    /** A computer at home: a private address (192.168.x, 10.x, 172.16-31.x) or a .local name. */
    fun isHome(host: String): Boolean {
        if (host.endsWith(".local")) return true
        val p = host.split('.').mapNotNull { it.toIntOrNull() }
        if (p.size != 4 || host.count { it == '.' } != 3) return false
        return p[0] == 10 || (p[0] == 192 && p[1] == 168) || (p[0] == 172 && p[1] in 16..31)
    }

    /** What kind of picture the first bytes say it is (jpeg, png, webp), or null if none. */
    fun kind(head: ByteArray): String? = when {
        head.size >= 3 && head[0] == 0xFF.toByte() && head[1] == 0xD8.toByte() && head[2] == 0xFF.toByte() -> "jpeg"
        head.size >= 8 && head[0] == 0x89.toByte() && String(head, 1, 3, Charsets.US_ASCII) == "PNG" -> "png"
        head.size >= 12 && String(head, 0, 4, Charsets.US_ASCII) == "RIFF" && String(head, 8, 4, Charsets.US_ASCII) == "WEBP" -> "webp"
        else -> null
    }

    /** The plain words for a server's refusal. */
    fun refused(code: Int): String = when (code) {
        401, 403 -> "The site didn't let the TV download it ($code)."
        404 -> "Nothing at that link (404)."
        else -> "The site answered $code."
    }

    /** The site's name for the screen: "example.com". */
    fun host(link: String): String = runCatching { URI(link).host }.getOrNull()?.removePrefix("www.") ?: link
}
