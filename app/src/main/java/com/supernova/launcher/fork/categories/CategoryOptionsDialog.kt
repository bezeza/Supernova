package com.supernova.launcher.fork.categories

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R

// A category that is off the tiles, to bring back.
class HiddenCategory(val id: String, val name: String)

// What the options sheet does when a row is picked.
internal class OptionsHandlers(
    val onSort: (CategorySort) -> Unit,
    val onSize: (CategorySize) -> Unit,
    val onLarge: (Int) -> Unit,
    val onHide: () -> Unit,
    val onFolderMenu: () -> Unit
)

// The sheet a long press on a tile opens: the category's own settings, and
// the folder's usual menu one row away.
internal object CategoryOptionsDialog {

    private enum class Row { SORT, SIZE, LARGE, HIDE, FOLDER }

    fun show(context: Context, name: String, options: CategoryOptions, handlers: OptionsHandlers) {
        // The number of large icons is a normal tile's: a wide one is a row, a small one all small icons.
        val rows = Row.entries.filter { it != Row.LARGE || options.size == CategorySize.NORMAL }
        val labels = rows.map { label(context, it, options) }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(name)
            .setItems(labels) { _, which ->
                when (rows[which]) {
                    Row.SORT -> {
                        val choice = Choice(CategorySort.entries, options.sort, ::sortLabel)
                        pick(context, name, choice, handlers.onSort)
                    }
                    Row.SIZE -> {
                        val choice = Choice(CategorySize.entries, options.size, ::sizeLabel)
                        pick(context, name, choice, handlers.onSize)
                    }
                    Row.LARGE -> {
                        val choice = Choice(CategoryOptions.LARGE_CHOICES, options.large, ::largeLabel)
                        pick(context, name, choice, handlers.onLarge)
                    }
                    Row.HIDE -> handlers.onHide()
                    Row.FOLDER -> handlers.onFolderMenu()
                }
            }
            .show()
    }

    private fun label(context: Context, row: Row, options: CategoryOptions): String = when (row) {
        Row.SORT -> context.getString(R.string.fork_sort_inside, sortLabel(context, options.sort))
        Row.SIZE -> context.getString(R.string.fork_tile_size, sizeLabel(context, options.size))
        Row.LARGE -> context.getString(R.string.fork_large_icons, largeLabel(context, options.large))
        Row.HIDE -> context.getString(R.string.fork_hide_category)
        Row.FOLDER -> context.getString(R.string.fork_folder_menu)
    }

    fun showHidden(context: Context, hidden: List<HiddenCategory>, onShow: (String) -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fork_show_again)
            .setItems(hidden.map { it.name }.toTypedArray()) { _, which -> onShow(hidden[which].id) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // What a single-choice dialog offers: the values, the one now chosen and their labels.
    private class Choice<T>(val values: List<T>, val current: T, val labelOf: (Context, T) -> String)

    private fun <T> pick(context: Context, name: String, choice: Choice<T>, onPick: (T) -> Unit) {
        val labels = choice.values.map { choice.labelOf(context, it) }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(name)
            .setSingleChoiceItems(labels, choice.values.indexOf(choice.current)) { dialog, which ->
                dialog.dismiss()
                onPick(choice.values[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sizeLabel(context: Context, size: CategorySize): String = context.getString(
        when (size) {
            CategorySize.NORMAL -> R.string.fork_size_normal
            CategorySize.WIDE -> R.string.fork_size_wide
            CategorySize.SMALL -> R.string.fork_size_small
        }
    )

    private fun largeLabel(context: Context, count: Int): String =
        if (count == 0) context.getString(R.string.fork_large_none) else count.toString()

    private fun sortLabel(context: Context, sort: CategorySort): String = context.getString(
        when (sort) {
            CategorySort.DRAWER -> R.string.fork_sort_drawer
            CategorySort.MANUAL -> R.string.fork_sort_manual
            CategorySort.ALPHA -> R.string.fork_sort_alpha
            CategorySort.FREQUENCY -> R.string.fork_sort_frequency
        }
    )
}
