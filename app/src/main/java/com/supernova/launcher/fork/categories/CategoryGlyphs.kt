package com.supernova.launcher.fork.categories

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.supernova.launcher.R

// The icon of each approved category (Material Symbols Outlined, Apache 2.0),
// found by the category's name. A folder or tab named otherwise has none, and
// the drawer falls back to what it did before. The drawables are white: the
// caller tints them from the drawer's own colours.
object CategoryGlyphs {

    private val BY_NAME: Map<String, Int> = mapOf(
        "Соцмережі" to R.drawable.ic_fork_cat_groups,
        "Месенджери" to R.drawable.ic_fork_cat_chat,
        "Банки й фінанси" to R.drawable.ic_fork_cat_account_balance,
        "Держава і зв’язок" to R.drawable.ic_fork_cat_badge,
        "Покупки" to R.drawable.ic_fork_cat_shopping_bag,
        "Транспорт" to R.drawable.ic_fork_cat_directions_car,
        "Кіно і відео" to R.drawable.ic_fork_cat_movie,
        "Музика і аудіо" to R.drawable.ic_fork_cat_music_note,
        "Фото і монтаж" to R.drawable.ic_fork_cat_photo_library,
        "Ігри" to R.drawable.ic_fork_cat_sports_esports,
        "Робота і пошта" to R.drawable.ic_fork_cat_work,
        "ШІ" to R.drawable.ic_fork_cat_smart_toy,
        "Медицина" to R.drawable.ic_fork_cat_medical_services,
        "Годинники" to R.drawable.ic_fork_cat_watch,
        "Теми і віджети" to R.drawable.ic_fork_cat_palette,
        "Мережа і VPN" to R.drawable.ic_fork_cat_vpn_lock,
        "Навчання" to R.drawable.ic_fork_cat_school,
        "Інструменти" to R.drawable.ic_fork_cat_build,
        "Системні" to R.drawable.ic_fork_cat_settings,
        "Інше" to R.drawable.ic_fork_cat_apps
    )

    @DrawableRes
    fun resFor(name: String): Int? = BY_NAME[name.trim()]

    // A fresh drawable for the category's name, or null for another name.
    fun forName(context: Context, name: String): Drawable? =
        resFor(name)?.let { AppCompatResources.getDrawable(context, it)?.mutate() }
}
