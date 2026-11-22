package com.supernova.launcher.fork.categories

// How strong a tile's fill is, agreed with the drawer's own background. The
// fill is the text colour at a low alpha, so it reads on any backdrop. Over a
// solid drawer it is the light tint the tiles had; the clearer the drawer is,
// the more of the wallpaper shows through, and the more body the tile needs to
// still stand out from it, up to GLASS at a fully clear drawer.
internal object CategoryTint {
    const val SOLID = 0x26
    const val GLASS = 0x40

    // [backdrop]: the drawer background's opacity, 0 (clear) to 1 (solid).
    fun cardAlpha(backdrop: Float): Int {
        val solid = backdrop.coerceIn(0f, 1f)
        return Math.round(GLASS + (SOLID - GLASS) * solid)
    }

    fun cardColor(textColor: Int, backdrop: Float): Int =
        (textColor and RGB_MASK) or (cardAlpha(backdrop) shl ALPHA_SHIFT)

    private const val RGB_MASK = 0xFFFFFF
    private const val ALPHA_SHIFT = 24
}
