package com.supernova.launcher.fork.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R
import com.supernova.launcher.ui.LauncherViewModel
import com.supernova.launcher.ui.settings.SettingItem
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

// Settings > Backup: the folder for the weekly copy, and when the last copies were made (a tap makes them now).
object ForkBackupRows {

    fun addTo(context: Context, viewModel: LauncherViewModel, items: List<SettingItem>): List<SettingItem> =
        items + listOf(
            SettingItem.Row(
                R.string.fork_backup_folder,
                subtitle = { folderSummary(context) },
                onClick = { folderTapped(context) }
            ).describedBy(R.string.fork_backup_folder_about),
            SettingItem.Row(
                R.string.fork_backup_last,
                subtitle = { lastSummary(context) },
                onClick = { backUpNow(context, viewModel) }
            ).describedBy(R.string.fork_backup_last_about)
        )

    private fun folderSummary(context: Context): String {
        val state = BackupState(context)
        val folder = state.folder ?: return context.getString(R.string.fork_backup_folder_none)
        val name = BackupFolder(context.contentResolver, folder.toUri()).name()
        return if (name == null || state.folderFailed) context.getString(R.string.fork_backup_folder_lost) else name
    }

    private fun lastSummary(context: Context): String {
        val state = BackupState(context)
        val format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        val date = { time: Long ->
            if (time == 0L) context.getString(R.string.fork_backup_never) else format.format(Date(time))
        }
        val inApp = context.getString(R.string.fork_backup_in_app, date(state.lastLocal))
        if (state.folder == null) return inApp
        return inApp + "\n" + context.getString(R.string.fork_backup_in_folder, date(state.lastInFolder))
    }

    private fun folderTapped(context: Context) {
        val pick = { context.startActivity(Intent(context, BackupFolderActivity::class.java)) }
        if (BackupState(context).folder == null) {
            pick()
            return
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fork_backup_folder)
            .setMessage(R.string.fork_backup_folder_about)
            .setPositiveButton(R.string.fork_backup_other_folder) { _, _ -> pick() }
            // The files already there stay: turning it off only stops new ones.
            .setNegativeButton(R.string.fork_backup_turn_off) { _, _ ->
                BackupState(context).folder = null
                activityOf(context)?.recreate()
            }
            .setNeutralButton(R.string.cancel, null)
            .show()
    }

    private fun backUpNow(context: Context, viewModel: LauncherViewModel) {
        val activity = activityOf(context) ?: return
        activity.lifecycleScope.launch {
            WeeklyBackup.run(activity, viewModel, force = true)
            val state = BackupState(activity)
            val failed = state.folder != null && state.folderFailed
            val message = if (failed) R.string.fork_backup_failed else R.string.fork_backup_done
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            activity.recreate()
        }
    }

    private fun activityOf(context: Context): AppCompatActivity? =
        generateSequence(context) { (it as? ContextWrapper)?.baseContext }
            .filterIsInstance<AppCompatActivity>().firstOrNull()
}
