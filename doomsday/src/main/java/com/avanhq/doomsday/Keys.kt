package com.avanhq.doomsday

/**
 * The Pictures screen's own keyboard, as keys and what they do. The arrows move round the keys,
 * wrapping at the edges; OK presses one. Plain, so it's tested on the JVM (KeysTest); drawn by
 * [KeyboardView].
 */
class Keys(text: String = "") {
    companion object {
        const val DELETE = "⌫"
        const val CLEAR = "Clear"
        const val TINY = "tinyurl.com/"
        const val DONE = "Done"

        /** The rows; each key's width in units (a letter is one). Every row is ten units wide. */
        val ROWS: List<List<Pair<String, Int>>> = listOf(
            "1234567890".map { it.toString() to 1 },
            "qwertyuiop".map { it.toString() to 1 },
            "asdfghjkl-".map { it.toString() to 1 },
            "zxcvbnm./_".map { it.toString() to 1 },
            listOf(":" to 1, DELETE to 2, CLEAR to 2, TINY to 3, DONE to 2),
        )
        const val UNITS = 10
    }

    var text = text
        private set
    var row = 0
        private set
    var col = 0
        private set

    val key: String get() = ROWS[row][col].first

    fun left() { col = (col - 1 + ROWS[row].size) % ROWS[row].size }
    fun right() { col = (col + 1) % ROWS[row].size }
    fun up() = toRow(row - 1)
    fun down() = toRow(row + 1)

    /** Onto another row, to the key under the middle of this one. */
    private fun toRow(r: Int) {
        val middle = start(row, col) + ROWS[row][col].second / 2f
        row = (r + ROWS.size) % ROWS.size
        col = ROWS[row].indices.first { start(row, it) + ROWS[row][it].second > middle }
    }

    /** Where key [c] of row [r] starts, in units. */
    fun start(r: Int, c: Int): Int = ROWS[r].take(c).sumOf { it.second }

    /** Press the key the arrows are on; true when it was Done. */
    fun press(): Boolean {
        when (val k = key) {
            DELETE -> text = text.dropLast(1)
            CLEAR -> text = ""
            TINY -> text = Links.TINY
            DONE -> return true
            else -> text += k
        }
        return false
    }

    /** A long press on ⌫: everything goes. */
    fun clear() { text = "" }
}
