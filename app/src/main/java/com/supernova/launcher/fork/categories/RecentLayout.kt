package com.supernova.launcher.fork.categories

// How the row of the newest installs lies, by the size the owner gave it. The row has three sizes, the footprints
// of tiles of the same name: a strip of one row across the whole width (4 by 1, as it always was, with as many
// icons as the setting says), a half-width card of two rows of two icons (2 by 2), a full-width card of two rows of
// four (4 by 2). Plain arithmetic, so it is testable without a view.
internal object RecentLayout {

    val SIZES = listOf(CategorySize.STRIP, CategorySize.NORMAL, CategorySize.WIDE)

    private const val HALF_COLUMNS = 2
    private const val FULL_COLUMNS = 4
    private const val ROWS_OF_CARD = 2

    // The size the row has when the owner never chose one, and for a size it cannot have.
    val DEFAULT = CategorySize.STRIP

    fun sizeOf(options: CategoryOptions?): CategorySize = options?.size?.takeIf { it in SIZES } ?: DEFAULT

    // How many icons stand across in one row, given the setting's [slots] for the strip.
    fun columns(size: CategorySize, slots: Int): Int = when (size) {
        CategorySize.NORMAL -> HALF_COLUMNS
        CategorySize.WIDE -> FULL_COLUMNS
        else -> slots
    }

    fun rows(size: CategorySize): Int = if (size == CategorySize.STRIP) 1 else ROWS_OF_CARD

    // How many of the newest installs the row shows.
    fun capacity(size: CategorySize, slots: Int): Int = columns(size, slots) * rows(size)

    // How many spans of the list's four the row takes: the half-width card two, the others all of them.
    fun span(size: CategorySize, spanCount: Int): Int =
        if (size == CategorySize.NORMAL) minOf(HALF_COLUMNS, spanCount) else spanCount
}
