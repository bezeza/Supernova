package com.supernova.launcher.ui

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R

/**
 * The interface language, chosen inside the launcher. It is a per-app locale
 * (AppCompatDelegate), so it survives restarts and shows up in Android 13+
 * "App languages" too. Language names are written in their own language.
 */
object AppLanguage {
    // BCP-47 tag to the name shown in the list; an empty tag follows the system.
    private val choices = listOf(
        "en" to "English",
        "uk" to "Українська",
        "ru" to "Русский",
        "ru-Cyrl-UA-odesa" to "Одесский"
    )

    private fun currentTag(): String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags()

    fun currentLabel(context: Context): String {
        val tag = currentTag()
        return choices.firstOrNull { it.first.equals(tag, ignoreCase = true) }?.second
            ?: context.getString(R.string.language_system)
    }

    fun showPicker(activity: AppCompatActivity) {
        val labels = (listOf(activity.getString(R.string.language_system)) + choices.map { it.second })
            .toTypedArray()
        val tag = currentTag()
        val checked = choices.indexOfFirst { it.first.equals(tag, ignoreCase = true) } + 1
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.language)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                dialog.dismiss()
                val picked = if (which == 0) "" else choices[which - 1].first
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(picked))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
