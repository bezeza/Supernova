package com.supernova.launcher.fork.categories

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.supernova.launcher.R

// The plate over the bottom of the tiles, the first time they are seen: a tile held opens its menu.
internal class TilesHint(context: Context, private val onChanged: () -> Unit) {

    private val flags = HintFlags.of(context)
    private val bar: PickBar = PickBar(context, R.string.fork_hint_ok) { seen() }.apply {
        visibility = View.GONE
        setMessage(context.getString(R.string.fork_hint_tile_menu))
    }

    val view: View get() = bar

    // The height it takes at the bottom of the list, 0 while it is not there.
    val height: Int get() = if (bar.isVisible) bar.height else 0

    fun setLook(accent: Int) = bar.setLook(accent)

    fun update(tilesShown: Boolean, hasTiles: Boolean, arranging: Boolean) {
        val due = flags.tileMenuDue(tilesShown, hasTiles, arranging)
        if (bar.isVisible == due) return
        bar.isVisible = due
        onChanged()
    }

    // "Got it", or a tile's menu opened: either way it is not told again.
    fun seen() {
        flags.markSeen(Hint.TILE_MENU)
        if (!bar.isVisible) return
        bar.isVisible = false
        onChanged()
    }
}
