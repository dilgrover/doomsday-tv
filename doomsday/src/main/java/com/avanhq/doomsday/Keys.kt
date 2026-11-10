package com.avanhq.doomsday

/**
 * The Pictures screen's own keyboard, as keys and what they do. The arrows move round the keys,
 * wrapping at the edges; OK presses one. Plain, so it's tested on the JVM (KeysTest); drawn by
 * [KeyboardView].
 */
class Keys(text: String = "", val rows: List<List<Pair<String, Int>>> = ROWS) {
    companion object {
        const val DELETE = "⌫"
        const val CLEAR = "Clear"
        const val TINY = "tinyurl.com/"
        const val DONE = "Done"
        const val SPACE = "Space"

        /** The rows; each key's width in units (a letter is one). Every row is ten units wide. */
        val ROWS: List<List<Pair<String, Int>>> = listOf(
            "1234567890".map { it.toString() to 1 },
            "qwertyuiop".map { it.toString() to 1 },
            "asdfghjkl-".map { it.toString() to 1 },
            "zxcvbnm./_".map { it.toString() to 1 },
            listOf(":" to 1, DELETE to 2, CLEAR to 2, TINY to 3, DONE to 2),
        )

        /** For a city's name (Settings → City): letters and a space instead of a link's marks. */
        val CITY_ROWS: List<List<Pair<String, Int>>> = listOf(
            "qwertyuiop".map { it.toString() to 1 },
            "asdfghjkl'".map { it.toString() to 1 },
            "zxcvbnm,.-".map { it.toString() to 1 },
            listOf(SPACE to 4, DELETE to 2, CLEAR to 2, DONE to 2),
        )
        const val UNITS = 10
    }

    var text = text
        private set
    var row = 0
        private set
    var col = 0
        private set

    val key: String get() = rows[row][col].first

    fun left() { col = (col - 1 + rows[row].size) % rows[row].size }
    fun right() { col = (col + 1) % rows[row].size }
    fun up() = toRow(row - 1)
    fun down() = toRow(row + 1)

    /** Onto another row, to the key under the middle of this one. */
    private fun toRow(r: Int) {
        val middle = start(row, col) + rows[row][col].second / 2f
        row = (r + rows.size) % rows.size
        col = rows[row].indices.first { start(row, it) + rows[row][it].second > middle }
    }

    /** Where key [c] of row [r] starts, in units. */
    fun start(r: Int, c: Int): Int = rows[r].take(c).sumOf { it.second }

    /** Press the key the arrows are on; true when it was Done. */
    fun press(): Boolean {
        when (val k = key) {
            DELETE -> text = text.dropLast(1)
            CLEAR -> text = ""
            TINY -> text = Links.TINY
            SPACE -> text += " "
            DONE -> return true
            else -> text += k
        }
        return false
    }

    /** A long press on ⌫: everything goes. */
    fun clear() { text = "" }
}
