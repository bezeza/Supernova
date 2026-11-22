package com.supernova.launcher.fork.categories

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.supernova.launcher.R
import com.supernova.launcher.data.AppCatalog
import com.supernova.launcher.data.IconThread
import com.supernova.launcher.data.LaunchableApp

// Puts an app's icon in a view the way the drawer's own tiles do: a cached
// icon at once, a missing one rendered off the main thread and set when it
// lands. A bind never renders, so a tile of seven icons can't stall a scroll.
internal class CategoryIcons(
    private val catalog: AppCatalog,
    private val decorate: (Drawable?, LaunchableApp) -> Drawable?
) {

    // [badge] false leaves the notification badge off, for icons too small to carry one.
    fun bind(view: ImageView, app: LaunchableApp, badge: Boolean = true) {
        val key = appKey(app)
        val cached = catalog.cachedIcon(app)
        if (cached != null) {
            view.setTag(R.id.tag_icon_app, key)
            view.setImageDrawable(shown(cached, app, badge))
            return
        }
        if (view.getTag(R.id.tag_icon_app) != key) view.setImageDrawable(null)
        view.setTag(R.id.tag_icon_app, key)
        IconThread.render(catalog, app) { icon ->
            if (view.getTag(R.id.tag_icon_app) == key) {
                view.setImageDrawable(shown(catalog.cachedIcon(app) ?: icon, app, badge))
            }
        }
    }

    fun clear(view: ImageView) {
        view.setTag(R.id.tag_icon_app, null)
        view.setImageDrawable(null)
    }

    private fun shown(icon: Drawable?, app: LaunchableApp, badge: Boolean): Drawable? =
        if (badge) decorate(icon, app) else icon
}
