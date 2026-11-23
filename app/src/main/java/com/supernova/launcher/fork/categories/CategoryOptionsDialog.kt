package com.supernova.launcher.fork.categories

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R

// A category that is off the tiles, to bring back.
class HiddenCategory(val id: String, val name: String)

// What the options sheet does when a row is picked.
internal class OptionsHandlers(
    val onSort: (CategorySort) -> Unit,
    val onHide: () -> Unit,
    val onFolderMenu: () -> Unit
)

// The sheet a long press on a tile opens: the category's own settings, and
// the folder's usual menu one row away.
internal object CategoryOptionsDialog {

    fun show(context: Context, name: String, options: CategoryOptions, handlers: OptionsHandlers) {
        val rows = arrayOf(
            context.getString(R.string.fork_sort_inside, sortLabel(context, options.sort)),
            context.getString(R.string.fork_hide_category),
            context.getString(R.string.fork_folder_menu)
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(name)
            .setItems(rows) { _, which ->
                when (which) {
                    0 -> pickSort(context, name, options.sort, handlers.onSort)
                    1 -> handlers.onHide()
                    else -> handlers.onFolderMenu()
                }
            }
            .show()
    }

    fun showHidden(context: Context, hidden: List<HiddenCategory>, onShow: (String) -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fork_show_again)
            .setItems(hidden.map { it.name }.toTypedArray()) { _, which -> onShow(hidden[which].id) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pickSort(context: Context, name: String, current: CategorySort, onSort: (CategorySort) -> Unit) {
        val sorts = CategorySort.entries
        val labels = sorts.map { sortLabel(context, it) }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle(name)
            .setSingleChoiceItems(labels, sorts.indexOf(current)) { dialog, which ->
                dialog.dismiss()
                onSort(sorts[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sortLabel(context: Context, sort: CategorySort): String = context.getString(
        when (sort) {
            CategorySort.DRAWER -> R.string.fork_sort_drawer
            CategorySort.MANUAL -> R.string.fork_sort_manual
            CategorySort.ALPHA -> R.string.fork_sort_alpha
            CategorySort.FREQUENCY -> R.string.fork_sort_frequency
        }
    )
}
