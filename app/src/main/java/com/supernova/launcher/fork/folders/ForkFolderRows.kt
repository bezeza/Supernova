package com.supernova.launcher.fork.folders

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R
import com.supernova.launcher.data.Pref
import com.supernova.launcher.ui.LauncherViewModel

// The rows a folder's Edit sheet gets from the fork: the window's columns, its
// opacity, the name in it, and the folder's icon. Each commits at once, as the
// sheet's swipe rows do, into the one preference of folder options.
object ForkFolderRows {

    // One row: what it chooses between, how to read and write it in the options, how to name a value.
    private class Setting<T>(
        val title: Int,
        val values: List<T>,
        val get: (FolderOptions) -> T,
        val set: (FolderOptions, T) -> FolderOptions,
        val label: (Context, T) -> String
    )

    private fun settings(): List<Setting<*>> = listOf(
        Setting(R.string.fork_folder_columns, (0..FolderOptions.COLUMNS_MAX).toList(), { it.columns },
            { o, v -> o.copy(columns = v) }) { c, v ->
            if (v == 0) c.getString(R.string.fork_folder_default) else v.toString()
        },
        Setting(R.string.fork_folder_tint, listOf<Int?>(null) + FolderOptions.TINT_CHOICES, { it.tint },
            { o, v -> o.copy(tint = v) }) { c, v ->
            if (v == null) c.getString(R.string.fork_folder_default) else c.getString(R.string.opacity_percent, v)
        },
        Setting(R.string.fork_folder_title, listOf(false, true), { it.hideTitle },
            { o, v -> o.copy(hideTitle = v) }) { c, v ->
            c.getString(if (v) R.string.fork_folder_title_hidden else R.string.fork_folder_title_shown)
        },
        Setting(R.string.fork_folder_look, FolderLook.entries, { it.look }, { o, v -> o.copy(look = v) }) { c, v ->
            c.getString(
                when (v) {
                    FolderLook.DEFAULT -> R.string.fork_folder_default
                    FolderLook.GRID -> R.string.fork_look_grid
                    FolderLook.GRID_3X3 -> R.string.fork_look_grid3
                    FolderLook.SINGLE -> R.string.fork_look_single
                }
            )
        }
    )

    // Adds the rows after [after], a row of the sheet, for the folder [key].
    fun add(after: android.view.View, viewModel: LauncherViewModel, key: String) {
        val parent = after.parent as? ViewGroup ?: return
        var index = parent.indexOfChild(after) + 1
        settings().forEach { parent.addView(row(after.context, viewModel, key, it), index++) }
    }

    private fun current(viewModel: LauncherViewModel, key: String): FolderOptions =
        ForkFolders.options(viewModel.state.value.prefs, key) ?: FolderOptions()

    private fun <T> row(
        context: Context,
        viewModel: LauncherViewModel,
        key: String,
        setting: Setting<T>
    ): LinearLayout {
        val value = TextView(context, null, 0, R.style.SheetRowValue)
        fun show(options: FolderOptions) {
            value.text = setting.label(context, setting.get(options))
        }
        show(current(viewModel, key))
        return LinearLayout(context, null, 0, R.style.SheetRow).apply {
            addView(TextView(context, null, 0, R.style.SheetRowTitle).apply { setText(setting.title) })
            addView(value)
            setOnClickListener {
                val labels = setting.values.map { setting.label(context, it) }.toTypedArray()
                val chosen = setting.values.indexOf(setting.get(current(viewModel, key)))
                MaterialAlertDialogBuilder(context)
                    .setTitle(setting.title)
                    .setSingleChoiceItems(labels, chosen) { dialog, which ->
                        dialog.dismiss()
                        val raw = viewModel.state.value.prefs.folderOptions
                        val next = FolderOptionsStore.update(raw, key) { setting.set(it, setting.values[which]) }
                        viewModel.set(Pref.folderOptions, next)
                        show(FolderOptionsStore.decode(next)[key] ?: FolderOptions())
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }
}
