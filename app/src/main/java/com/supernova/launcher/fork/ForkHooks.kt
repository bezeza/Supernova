package com.supernova.launcher.fork

import android.app.Activity
import android.content.Intent
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
import com.supernova.launcher.fork.welcome.ForkLanguageActivity
import com.supernova.launcher.fork.welcome.Welcome
import com.supernova.launcher.fork.welcome.WelcomeFlags

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
     * Starts the original's first run, [wizard]; until a language has been asked for once, the fork's language
     * screen instead, which goes on to the wizard. Called where [LauncherActivity] would start the wizard itself.
     */
    fun startOnboarding(activity: Activity, wizard: Intent) {
        val flags = WelcomeFlags(activity)
        // A first run that begins with the fork in it ends with the fork's tour.
        flags.firstRun = true
        val first = if (flags.languageAsked) {
            wizard
        } else {
            Intent(activity, ForkLanguageActivity::class.java)
        }
        activity.startActivity(first)
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
        override fun onResume(owner: LifecycleOwner) {
            if (owner is LauncherActivity) Welcome.onLauncherResumed(owner)
            plugIn(owner, Phase.RESUME)
        }

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
