package com.supernova.launcher.fork

import android.graphics.drawable.Drawable
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.supernova.launcher.LauncherActivity
import com.supernova.launcher.data.AppCatalog
import com.supernova.launcher.data.LaunchableApp
import com.supernova.launcher.ui.AppDrawerAdapter
import com.supernova.launcher.fork.categories.CategoriesActions
import com.supernova.launcher.fork.categories.CategoriesController

/**
 * The fork's only entry point inside [LauncherActivity].
 *
 * [LauncherActivity.onCreate] calls [attach] once, after upstream setup.
 * Fork features belong in this package. They plug into the lifecycle methods
 * on [observer]: [DefaultLifecycleObserver.onStart] when the launcher becomes
 * visible, [DefaultLifecycleObserver.onResume] when it is the screen in front,
 * [DefaultLifecycleObserver.onDestroy] when this instance is going away.
 * Those methods do nothing until a feature is added there.
 *
 * [attach] adds one shared observer. Lifecycle ignores a second add of that
 * same observer, so calling [attach] again on one activity does not stack hooks.
 */
object ForkHooks {

    fun attach(activity: LauncherActivity) {
        activity.lifecycle.addObserver(observer)
    }

    /**
     * The Categories drawer's list, added to the drawer's list [container].
     * DrawerController builds it once and feeds it; see fork/categories.
     */
    fun categories(
        container: FrameLayout,
        catalog: AppCatalog,
        decorateIcon: (Drawable?, LaunchableApp) -> Drawable?,
        actions: CategoriesActions,
        screenAdapter: AppDrawerAdapter
    ): CategoriesController = CategoriesController(container, catalog, decorateIcon, actions, screenAdapter)

    private val observer = object : DefaultLifecycleObserver {
        /** Fork work that should run each time the launcher becomes visible plugs in here. */
        override fun onStart(owner: LifecycleOwner) = plugIn(owner, Phase.START)

        /** Fork work that should run each time the launcher is in front plugs in here. */
        override fun onResume(owner: LifecycleOwner) = plugIn(owner, Phase.RESUME)

        /** Fork work that should run when this launcher instance is going away plugs in here. */
        override fun onDestroy(owner: LifecycleOwner) = plugIn(owner, Phase.DESTROY)
    }

    /**
     * No-op stand-in. A fork feature replaces this body for its [phase]
     * and uses [owner] (the [LauncherActivity] passed to [attach]).
     */
    @Suppress("UnusedParameter")
    private fun plugIn(owner: LifecycleOwner, phase: Phase) = Unit

    private enum class Phase { START, RESUME, DESTROY }
}
