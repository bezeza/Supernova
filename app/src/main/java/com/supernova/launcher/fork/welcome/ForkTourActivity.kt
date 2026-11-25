package com.supernova.launcher.fork.welcome

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.supernova.launcher.R

// The fork's tour: three pages over the home screen, after the first run or from About. "Skip" leaves it from any
// page; Back goes a page back, and leaves from the first.
class ForkTourActivity : AppCompatActivity() {

    private class Page(val scene: TourDemo.Scene, val title: Int, val text: Int)

    private val pages = listOf(
        Page(TourDemo.Scene.MOSAIC, R.string.fork_tour_mosaic, R.string.fork_tour_mosaic_text),
        Page(TourDemo.Scene.ARRANGE, R.string.fork_tour_arrange, R.string.fork_tour_arrange_text),
        Page(TourDemo.Scene.FEED, R.string.fork_tour_feed, R.string.fork_tour_feed_text)
    )
    private lateinit var pager: ViewPager2
    private val ink by lazy { ContextCompat.getColor(this, R.color.onboardingText) }
    private val icons by lazy { appIcons() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        WelcomeFlags(this).apply {
            tourSeen = true
            newsSeen = WelcomePlan.NEWS
        }
        setContentView(content())
        onBackPressedDispatcher.addCallback(this) {
            if (pager.currentItem > 0) pager.currentItem -= 1 else finish()
        }
    }

    private fun content(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.onboardingBackground))
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(system.left, system.top, system.right, system.bottom + dp(SIDE_DP))
            insets
        }
        val skip = TextView(context, null, 0, R.style.OnboardingTextButton).apply {
            setText(R.string.fork_tour_skip)
            setOnClickListener { finish() }
        }
        addView(skip, LinearLayout.LayoutParams(WRAP_CONTENT, dp(BUTTON_HEIGHT_DP)).apply { gravity = Gravity.END })
        pager = ViewPager2(context).apply { adapter = Pages() }
        addView(pager, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        val dots = label(R.string.fork_tour_dots, DOTS_SP)
        addView(dots, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        val next = TextView(context, null, 0, R.style.OnboardingPrimaryButton).apply {
            setOnClickListener {
                if (pager.currentItem < pages.lastIndex) pager.currentItem += 1 else finish()
            }
        }
        addView(next, LinearLayout.LayoutParams(MATCH_PARENT, dp(BUTTON_HEIGHT_DP)).apply {
            setMargins(dp(SIDE_DP), dp(SIDE_DP) / 2, dp(SIDE_DP), 0)
        })
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.text = pages.indices.joinToString(" ") { if (it == position) "●" else "○" }
                next.setText(if (position == pages.lastIndex) R.string.fork_tour_done else R.string.fork_tour_next)
            }
        })
    }

    private inner class Pages : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun getItemCount() = pages.size

        override fun getItemViewType(position: Int) = position

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view = page(pages[viewType])
            view.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            return object : RecyclerView.ViewHolder(view) {}
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) = Unit
    }

    private fun page(page: Page): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(SIDE_DP), 0, dp(SIDE_DP), 0)
        val feed = listOf(
            R.string.fork_tour_feed_search, R.string.fork_tour_feed_music, R.string.feed_calendar_title,
            R.string.feed_contacts_title
        ).map { getString(it) }
        addView(TourDemo(context, page.scene, icons, ink, feed), LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        addView(label(page.title, TITLE_SP))
        addView(label(page.text, TEXT_SP).apply { minLines = TEXT_LINES })
    }

    private fun label(res: Int, size: Float) = TextView(this).apply {
        setText(res)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(ink)
        gravity = Gravity.CENTER
        setPadding(0, dp(GAP_DP), 0, 0)
    }

    // A few of the phone's own app icons, for the pictures.
    private fun appIcons(): List<Drawable> {
        val launchable = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launchable, 0)
            .filter { it.activityInfo.packageName != packageName }
            .take(ICONS)
            .map { it.loadIcon(packageManager) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val ICONS = 8
        const val SIDE_DP = 24
        const val GAP_DP = 12
        const val BUTTON_HEIGHT_DP = 52
        const val TITLE_SP = 24f
        const val TEXT_SP = 16f
        const val TEXT_LINES = 3
        const val DOTS_SP = 14f
    }
}
