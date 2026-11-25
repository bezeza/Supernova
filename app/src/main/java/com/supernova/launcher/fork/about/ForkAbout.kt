package com.supernova.launcher.fork.about

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R
import com.supernova.launcher.ui.settings.AboutLinks
import com.supernova.launcher.ui.settings.SettingItem

// What Settings > About says about the fork: who made it, from what, and what it changed. The rows go after the
// original's "What's new"; "Source code" and "Report an issue" point at the fork through about_source_url.
object ForkAbout {

    const val REPOSITORY = "https://github.com/bezeza/Supernova"
    const val AUTHOR_GITHUB = "https://github.com/bezeza"
    const val AUTHOR_TELEGRAM = "https://t.me/Bezeza"
    const val ORIGINAL = "https://github.com/tanujnotes/Supernova"

    // About's rows with the fork's own among them.
    fun addTo(context: Context, items: List<SettingItem>): List<SettingItem> {
        val at = items.indexOfFirst { it.title == R.string.about_whats_new }
        return if (at < 0) items + rows(context) else items.take(at + 1) + rows(context) + items.drop(at + 1)
    }

    private fun rows(context: Context): List<SettingItem> = listOf(
        SettingItem.Row(
            R.string.fork_about,
            subtitle = { context.getString(R.string.fork_about_summary) },
            onClick = { showAuthor(context) }
        ).describedBy(R.string.fork_about_summary),
        SettingItem.Row(
            R.string.fork_whats_new,
            subtitle = { context.getString(R.string.fork_whats_new_summary) },
            onClick = { showChanges(context) }
        ).describedBy(R.string.fork_whats_new_summary)
    )

    private fun showAuthor(context: Context) {
        val links = listOf(
            R.string.fork_about_github to AUTHOR_GITHUB,
            R.string.fork_about_telegram to AUTHOR_TELEGRAM,
            R.string.fork_about_original to ORIGINAL
        )
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fork_about_author)
            .setItems(links.map { context.getString(it.first) }.toTypedArray()) { _, which ->
                AboutLinks.open(context, links[which].second)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showChanges(context: Context) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.fork_whats_new)
            .setMessage(R.string.fork_changes)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
