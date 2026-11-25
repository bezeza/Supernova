package com.supernova.launcher.fork.welcome

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R
import com.supernova.launcher.ui.LauncherViewModel
import kotlinx.coroutines.launch

// Shows what [WelcomePlan] says is due over the home screen.
internal object Welcome {

    fun onLauncherResumed(activity: AppCompatActivity) {
        val flags = WelcomeFlags(activity)
        // Nothing left either way: no need to ask the store about the first run.
        val settled = if (flags.firstRun) flags.tourSeen else flags.newsSeen >= WelcomePlan.NEWS
        if (settled) return
        val viewModel = ViewModelProvider(activity)[LauncherViewModel::class.java]
        activity.lifecycleScope.launch {
            val done = viewModel.onboardingDone()
            when (WelcomePlan.due(done, flags.firstRun, flags.tourSeen, flags.newsSeen)) {
                WelcomePlan.Step.TOUR -> openTour(activity)
                WelcomePlan.Step.NEWS_CARD -> showNews(activity, flags)
                null -> Unit
            }
        }
    }

    // The tour marks itself seen as it opens: Home pressed half way through is not a trap.
    fun openTour(context: Context) {
        context.startActivity(Intent(context, ForkTourActivity::class.java))
    }

    private fun showNews(activity: AppCompatActivity, flags: WelcomeFlags) {
        if (activity.isFinishing || activity.isDestroyed) return
        flags.newsSeen = WelcomePlan.NEWS
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.fork_whats_new)
            .setMessage(R.string.fork_changes)
            .setPositiveButton(R.string.fork_news_show_tour) { _, _ -> openTour(activity) }
            .setNegativeButton(R.string.fork_news_close, null)
            .show()
    }
}
