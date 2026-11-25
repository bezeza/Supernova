package com.supernova.launcher.fork.categories

import android.content.Context
import android.view.View
import com.supernova.launcher.R

// The plate on a category's screen: the hint about the columns the first time, and for a moment the news that the
// large icons are chosen.
internal class PageNotes(context: Context) {

    private val flags = HintFlags.of(context)
    private var announcing = false
    private val bar: PickBar = PickBar(context, R.string.fork_hint_ok) {
        flags.markSeen(Hint.COLUMNS)
        hide()
    }.apply { visibility = View.GONE }

    val view: View get() = bar

    private val done = Runnable {
        announcing = false
        showHint(picking = false)
    }

    fun setLook(accent: Int) = bar.setLook(accent)

    // The hint, where it is due; the news of a choice stays its moment out.
    fun showHint(picking: Boolean) {
        if (announcing && !picking) return
        hide()
        if (!flags.columnsDue(picking)) return
        bar.setMessage(bar.context.getString(R.string.fork_hint_columns))
        bar.setButtonShown(true)
        bar.visibility = View.VISIBLE
    }

    fun announceChosen() {
        hide()
        announcing = true
        bar.setMessage(bar.context.getString(R.string.fork_pick_large_done))
        bar.setButtonShown(false)
        bar.visibility = View.VISIBLE
        bar.postDelayed(done, ANNOUNCE_MS)
    }

    fun hide() {
        bar.removeCallbacks(done)
        announcing = false
        bar.visibility = View.GONE
    }

    private companion object {
        const val ANNOUNCE_MS = 2500L
    }
}
