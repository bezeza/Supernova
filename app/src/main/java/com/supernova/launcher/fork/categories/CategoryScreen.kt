package com.supernova.launcher.fork.categories

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.supernova.launcher.data.DrawerGroup
import com.supernova.launcher.data.LaunchableApp
import com.supernova.launcher.ui.AppDrawerAdapter

// How the drawer's own grid is laid out, which the category's screen copies so
// its apps sit as they do in the drawer: the column count and the grid's padding.
class ScreenLook(
    val columns: Int,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    // The look for another number of apps, asked when the category gains or loses some while it is open (the
    // columns follow the count); null where the look does not depend on it.
    val relook: ((Int) -> ScreenLook)? = null
)

// One category on a screen of its own, over the tiles, like a folder in App
// Library or a category in HyperOS. The content is a [CategoryPage]; this puts
// it over the tiles' list in the drawer's container and brings it in and out:
// it fades and scales in, and goes away the same way. Back, the arrow and a
// swipe to the right all return to the tiles.
internal class CategoryScreen(
    container: FrameLayout,
    cells: AppDrawerAdapter,
    onBack: () -> Unit,
    // The apps chosen as large icons, in the order chosen.
    onPicked: (List<LaunchableApp>) -> Unit,
    // The header's "+": add apps to the category.
    onAdd: () -> Unit
) {

    private val page = CategoryPage(container.context, cells, onBack, onPicked, onAdd)
    private val root = page.view.apply { visibility = View.GONE }

    // The apps' grid; the controller hangs the drawer's tab swipe on it.
    val grid: RecyclerView get() = page.grid

    init {
        // Above the tiles' list, which sits at index 1.
        container.addView(
            root,
            container.childCount,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    val isShown: Boolean get() = root.isVisible

    fun show(group: DrawerGroup, apps: List<LaunchableApp>, look: ScreenLook, animate: Boolean) {
        page.bind(group, apps, look)
        root.animate().cancel()
        root.visibility = View.VISIBLE
        if (animate) {
            root.alpha = 0f
            root.scaleX = OPEN_SCALE
            root.scaleY = OPEN_SCALE
            root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(ANIMATION_MS).start()
        } else {
            root.alpha = 1f
            root.scaleX = 1f
            root.scaleY = 1f
        }
    }

    // New apps for the open category: the grid diffs against what it had.
    fun update(apps: List<LaunchableApp>) = page.update(apps)

    fun hide(animate: Boolean) {
        page.release()
        root.animate().cancel()
        if (!animate || !isShown) {
            root.visibility = View.GONE
            return
        }
        root.animate().alpha(0f).scaleX(OPEN_SCALE).scaleY(OPEN_SCALE).setDuration(ANIMATION_MS)
            .withEndAction { root.visibility = View.GONE }.start()
    }

    fun rebind() = page.rebind()

    fun setLook(textColor: Int, showIcon: Boolean, accent: Int) = page.setLook(textColor, showIcon, accent)

    // True while choosing, whatever the tap was: it is the choosing's, not a launch.
    fun handleTap(app: LaunchableApp): Boolean = page.handleTap(app)

    // Back while choosing stops the choosing and leaves the screen open.
    fun cancelPicking(): Boolean = page.cancelPicking()

    private companion object {
        const val ANIMATION_MS = 180L
        const val OPEN_SCALE = 0.96f
    }
}
