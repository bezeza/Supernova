package com.supernova.launcher.fork.welcome

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.supernova.launcher.R
import com.supernova.launcher.onboarding.OnboardingActivity
import com.supernova.launcher.ui.AppLanguage

// The one screen before the original's first run: the language, so the wizard after it is read in it. It starts the
// wizard itself once left. With another language picked it first lets the system recreate it in that language, and
// starts the wizard from there: a wizard started before the change had landed stayed in the old one.
class ForkLanguageActivity : AppCompatActivity() {

    private val tags = AppLanguage.choices.map { it.first }
    private val rows = mutableMapOf<String, TextView>()
    private lateinit var preselected: String
    private lateinit var chosen: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Recreated in the language just picked: on to the wizard.
        if (savedInstanceState != null && WelcomeFlags(this).languageAsked) {
            openWizard()
            return
        }
        // Drawn under the system bars, as the wizard is, with light icons on its dark ground.
        val bars = SystemBarStyle.dark(Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        preselected = LanguageChoice.preselected(
            AppCompatDelegate.getApplicationLocales().toLanguageTags(),
            Resources.getSystem().configuration.locales[0].language,
            tags
        )
        chosen = savedInstanceState?.getString(KEY_CHOSEN) ?: preselected
        setContentView(content())
        mark()
        // Back is "Next" with nothing changed.
        onBackPressedDispatcher.addCallback(this) { done(null) }
    }

    private fun openWizard() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_CHOSEN, chosen)
    }

    private fun content(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(color(R.color.onboardingBackground))
        val side = dp(SIDE_DP)
        setPadding(side, side, side, side)
        ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(side, bars.top + side, side, bars.bottom + side)
            insets
        }
        addView(text(R.string.fork_language_title, TITLE_SP), row(bottom = GAP_DP * 2))
        AppLanguage.choices.forEach { (tag, name) ->
            val choice = TextView(context).apply {
                this.text = name
                setTextSize(TypedValue.COMPLEX_UNIT_SP, CHOICE_SP)
                setTextColor(color(R.color.onboardingText))
                gravity = Gravity.CENTER
                minHeight = dp(CHOICE_HEIGHT_DP)
                isFocusable = true
                setOnClickListener {
                    chosen = tag
                    mark()
                }
            }
            rows[tag] = choice
            addView(choice, row(bottom = GAP_DP))
        }
        val next = TextView(context, null, 0, R.style.OnboardingPrimaryButton).apply {
            setText(R.string.fork_language_next)
            setOnClickListener { done(LanguageChoice.toApply(preselected, chosen)) }
        }
        addView(next, LinearLayout.LayoutParams(MATCH_PARENT, dp(BUTTON_HEIGHT_DP)).apply { topMargin = dp(SIDE_DP) })
    }

    private fun text(res: Int, size: Float) = TextView(this).apply {
        setText(res)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setTextColor(color(R.color.onboardingText))
        gravity = Gravity.CENTER
    }

    private fun row(bottom: Int) =
        LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = dp(bottom) }

    // The chosen language has a frame and says so to a screen reader.
    private fun mark() {
        rows.forEach { (tag, view) ->
            val on = tag == chosen
            view.isSelected = on
            view.background = GradientDrawable().apply {
                cornerRadius = dp(CORNER_DP).toFloat()
                val stroke = if (on) STROKE_ON_DP else STROKE_OFF_DP
                val line = color(R.color.onboardingText)
                setStroke(dp(stroke), if (on) line else (line and COLOR_MASK) or (FAINT_ALPHA shl ALPHA_SHIFT))
            }
        }
    }

    private fun done(tag: String?) {
        WelcomeFlags(this).languageAsked = true
        if (tag == null) {
            openWizard()
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        }
    }

    private fun color(res: Int) = ContextCompat.getColor(this, res)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val KEY_CHOSEN = "chosen"
        const val TITLE_SP = 26f
        const val CHOICE_SP = 18f
        const val CHOICE_HEIGHT_DP = 56
        const val BUTTON_HEIGHT_DP = 52
        const val SIDE_DP = 24
        const val GAP_DP = 12
        const val CORNER_DP = 16
        const val STROKE_ON_DP = 3
        const val STROKE_OFF_DP = 1
        const val FAINT_ALPHA = 0x44
        const val ALPHA_SHIFT = 24
        const val COLOR_MASK = 0xFFFFFF
    }
}
