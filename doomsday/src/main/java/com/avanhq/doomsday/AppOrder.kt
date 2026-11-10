package com.avanhq.doomsday

/**
 * Which apps the row shows and in what order: your own order first, then the TV's usual favourites, then
 * by name; apps you hide stay out of the row but are still listed in Settings → Apps. Plain
 * functions on package names, tested on the JVM (AppOrderTest).
 */
object AppOrder {
    /** The usual order on the row ; anything else follows by name. */
    val FAVOURITES = listOf(
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom",
        "com.amazon.avod",
        "com.disney.disneyplus",
        "com.apple.atve.androidtv.appletv",
        "uk.co.bbc.iplayer",
        "bbc.iplayer.android",
        "air.ITVMobilePlayer",
        "com.channel4.ondemand",
        "com.nowtv.androidtv",
        "com.bskyb.sky",
        "com.spotify.tv.android",
        "com.google.android.youtube.tvmusic",
        "in.startv.hotstar",
    )

    /** Never in the row or the list: the TV's own tools and leftovers. */
    val ALWAYS_HIDDEN = setOf(
        "com.google.android.play.games",
        "com.google.android.videos",
        "com.android.vending",
        "com.android.tv.settings",
        "com.google.android.tv.settings",
        "com.amazon.tv.settings.v2",
    )

    /** One app: its package and its name (for sorting by name). */
    class App(val pkg: String, val label: String)

    /** [apps] in their order: [order] first, then the favourites, then by name. [hidden] are
     *  left out unless [all] (Settings lists every app, hidden ones too). */
    fun arrange(apps: List<App>, order: List<String>, hidden: Set<String>, self: String, all: Boolean = false): List<App> =
        apps.asSequence()
            .filter { it.pkg != self && it.pkg !in ALWAYS_HIDDEN }
            .filter { all || it.pkg !in hidden }
            .distinctBy { it.pkg }
            .sortedWith(compareBy<App>({ rank(it.pkg, order) }, { it.label.lowercase() }))
            .toList()

    private fun rank(pkg: String, order: List<String>): Int {
        val mine = order.indexOf(pkg)
        if (mine >= 0) return mine
        val usual = FAVOURITES.indexOf(pkg)
        return if (usual >= 0) 10_000 + usual else Int.MAX_VALUE
    }

    /** The new saved order after moving [pkg] [by] places in [shown] (the order as listed). */
    fun move(shown: List<String>, pkg: String, by: Int): List<String> {
        val i = shown.indexOf(pkg)
        if (i < 0) return shown
        val to = (i + by).coerceIn(0, shown.size - 1)
        if (to == i) return shown
        return shown.toMutableList().apply { removeAt(i); add(to, pkg) }
    }

    /** [pkg] moved to the front of [shown]. */
    fun front(shown: List<String>, pkg: String): List<String> =
        if (pkg !in shown) shown else listOf(pkg) + shown.filter { it != pkg }

    /** [hidden] with [pkg] switched between shown and hidden. */
    fun toggle(hidden: Set<String>, pkg: String): Set<String> = if (pkg in hidden) hidden - pkg else hidden + pkg

    /** A saved list ("a,b,c") back into packages. */
    fun decode(s: String?): List<String> = s.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun encode(pkgs: Collection<String>): String = pkgs.joinToString(",")
}
