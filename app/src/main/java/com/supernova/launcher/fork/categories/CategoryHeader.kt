package com.supernova.launcher.fork.categories

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.supernova.launcher.R

// The top of a category's screen: a back arrow, the category's name (with its
// icon when that is switched on) and a button that starts choosing the large
// icons. While choosing, the name line shows what to do instead.
internal class CategoryHeader(context: Context) : LinearLayout(context) {

    var onBack: () -> Unit = {}
    var onPick: () -> Unit = {}
    var onAdd: () -> Unit = {}

    private val density = context.resources.displayMetrics.density
    private var name = ""
    private var textColor = 0
    private var showIcon = false
    private var hint: String? = null

    private val backButton = button(R.drawable.ic_back, context.getString(R.string.back)) { onBack() }
    private val pickButton =
        button(R.drawable.ic_fork_pick_large, context.getString(R.string.fork_pick_large)) { onPick() }

    private val addButton = button(R.drawable.ic_fork_add, context.getString(R.string.fork_add_apps)) { onAdd() }

    private val title = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_SP)
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val side = dp(SIDE_DP)
        setPadding(side, dp(VERTICAL_DP), side, dp(VERTICAL_DP))
        val size = dp(BUTTON_DP)
        addView(backButton, LayoutParams(size, size))
        addView(title, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(TITLE_GAP_DP)
        })
        addView(addButton, LayoutParams(size, size))
        addView(pickButton, LayoutParams(size, size))
    }

    // [choosable]: there is something to choose between.
    fun show(name: String, choosable: Boolean) {
        this.name = name
        hint = null
        pickButton.visibility = if (choosable) VISIBLE else GONE
        render()
    }

    fun setChoosable(choosable: Boolean) {
        if (hint == null) pickButton.visibility = if (choosable) VISIBLE else GONE
    }

    // While choosing: [text] in place of the name, and the button away.
    fun setHint(text: String?, choosable: Boolean) {
        hint = text
        pickButton.visibility = if (text != null) INVISIBLE else if (choosable) VISIBLE else GONE
        addButton.visibility = if (text != null) INVISIBLE else VISIBLE
        render()
    }

    fun setLook(color: Int, showIcon: Boolean) {
        if (color == textColor && showIcon == this.showIcon) return
        textColor = color
        this.showIcon = showIcon
        title.setTextColor(color)
        backButton.imageTintList = ColorStateList.valueOf(color)
        pickButton.imageTintList = ColorStateList.valueOf(color)
        addButton.imageTintList = ColorStateList.valueOf(color)
        render()
    }

    private fun render() {
        title.text = hint ?: name
        val glyph = if (showIcon && hint == null) CategoryGlyphs.forName(context, name) else null
        if (glyph == null) {
            title.setCompoundDrawablesRelative(null, null, null, null)
            return
        }
        val size = dp(GLYPH_DP)
        glyph.setTint(textColor)
        glyph.setBounds(0, 0, size, size)
        title.setCompoundDrawablesRelative(glyph, null, null, null)
        title.compoundDrawablePadding = dp(GLYPH_GAP_DP)
    }

    private fun button(icon: Int, description: String, onClick: () -> Unit) = ImageButton(context).apply {
        setImageResource(icon)
        background = ripple()
        contentDescription = description
        setOnClickListener { onClick() }
    }

    private fun ripple() = TypedValue().let { value ->
        context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true)
        AppCompatResources.getDrawable(context, value.resourceId)
    }

    private fun dp(value: Float): Int = (value * density).toInt()

    private companion object {
        const val TITLE_SP = 20f
        const val SIDE_DP = 8f
        const val VERTICAL_DP = 4f
        const val BUTTON_DP = 48f
        const val TITLE_GAP_DP = 4f
        const val GLYPH_DP = 22f
        const val GLYPH_GAP_DP = 8f
    }
}
