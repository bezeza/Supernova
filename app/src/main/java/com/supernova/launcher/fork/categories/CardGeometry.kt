package com.supernova.launcher.fork.categories

// Where a tile card's parts lie, for the layout its slots ask: a square tile, a
// double one (two tiles wide, one high) or a tall one (one wide, two high).
// Plain arithmetic on the card's size, so the card only has to place what this
// says.
internal class CardGeometry(private val slots: () -> List<TileSlots.Slot>, private val gap: () -> Int) {

    // How many heights of the card the icons run across: 1, 2 for a double tile, a half for a tall one.
    private fun content(): Float = TileSlots.contentWidth(slots())

    fun height(width: Int): Int = when {
        content() > 1f -> TileGeometry.doubleHeight(width, gap())
        content() < 1f -> TileGeometry.tallHeight(width, gap())
        else -> width
    }

    // A share of the shorter side, so a tall tile pads as a normal one does.
    fun padding(width: Int, height: Int): Int = (minOf(width, height) * PADDING_SHARE).toInt()

    // The length the slots' fractions are of: what the card's height leaves, or less if the icons would not
    // fit across.
    fun reference(width: Int, height: Int): Int {
        val pad = padding(width, height)
        return minOf(height - 2 * pad, ((width - 2 * pad) / content()).toInt())
    }

    // Where the icons start across and down: their run is centred in the card.
    fun left(width: Int, height: Int): Int = (width - (content() * reference(width, height)).toInt()) / 2

    fun top(width: Int, height: Int): Int = (height - reference(width, height)) / 2

    // The corner of small icons has a target of its own where three or more large icons stand beside it
    // (the usual tile, the double, the tall and the big one); in the others a tap off an icon is the card's.
    fun cornerTarget(width: Int, height: Int): TileGeometry.Rect? {
        val corner = slots().filter { !it.large }
        if (TileSlots.largeCount(slots()) < CategoryLayout.LARGE_SLOTS || corner.isEmpty()) return null
        val ref = reference(width, height)
        val left = left(width, height) + (corner.minOf { it.x } * ref).toInt()
        val top = top(width, height) + (corner.minOf { it.y } * ref).toInt()
        val cell = (slots().first { it.large }.size * ref).toInt()
        return TileGeometry.cornerTarget(left, top, width, height, cell)
    }

    private companion object {
        const val PADDING_SHARE = 0.07f
    }
}
