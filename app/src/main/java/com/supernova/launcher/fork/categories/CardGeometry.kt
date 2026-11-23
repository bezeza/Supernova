package com.supernova.launcher.fork.categories

// Where a tile card's parts lie, for the layout its slots ask: a square tile
// or a double one, two tiles wide and one high. Plain arithmetic on the card's
// size, so the card only has to place what this says.
internal class CardGeometry(private val slots: () -> List<TileSlots.Slot>, private val gap: () -> Int) {

    private fun content(): Float = TileSlots.contentWidth(slots())

    fun height(width: Int): Int = if (content() > 1f) TileGeometry.doubleHeight(width, gap()) else width

    fun padding(height: Int): Int = (height * PADDING_SHARE).toInt()

    // The length the slots' fractions are of: the card's height, less its padding.
    fun reference(height: Int): Int = height - 2 * padding(height)

    // Where the icons start across: a double tile's run of icons is centred in its width.
    fun left(width: Int, height: Int): Int {
        val pad = padding(height)
        return pad + ((width - 2 * pad) - (content() * reference(height)).toInt()) / 2
    }

    // The corner of small icons has a target of its own where three or more large icons stand beside it
    // (the usual tile and the double one); in the others a tap off an icon is the card's.
    fun cornerTarget(width: Int, height: Int): TileGeometry.Rect? {
        val corner = slots().filter { !it.large }
        if (TileSlots.largeCount(slots()) < CategoryLayout.LARGE_SLOTS || corner.isEmpty()) return null
        val ref = reference(height)
        val left = left(width, height) + (corner.minOf { it.x } * ref).toInt()
        val top = padding(height) + (corner.minOf { it.y } * ref).toInt()
        // A cell of the large icons is half the reference.
        return TileGeometry.cornerTarget(left, top, width, height, ref / 2)
    }

    private companion object {
        const val PADDING_SHARE = 0.07f
    }
}
