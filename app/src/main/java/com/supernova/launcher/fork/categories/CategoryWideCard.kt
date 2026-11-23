package com.supernova.launcher.fork.categories

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.supernova.launcher.R
import com.supernova.launcher.data.LaunchableApp
import kotlin.math.max
import kotlin.math.min

// A category's tile as a row across the screen: the name on the left, up to
// six icons of a middle size on the right, and "+N" when there are more apps.
// Laid out by hand, like the square tile.
internal class CategoryWideCard(context: Context) : EditableCard(context) {

    private val density = context.resources.displayMetrics.density
    private val shape = GradientDrawable()
    private val icons = List(WideRow.SLOTS) { ImageView(context) }
    private var used = 0

    val nameView = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, NAME_SP)
        maxLines = NAME_LINES
        ellipsize = TextUtils.TruncateAt.END
        gravity = Gravity.CENTER_VERTICAL
    }

    // "+N" in the slot after the last icon.
    private val more = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, MORE_SP)
        gravity = Gravity.CENTER
        maxLines = 1
    }

    // True: a tap on an icon launches the app. False: every tap opens the category.
    var launchFromIcons = true


    init {
        addView(nameView)
        icons.forEach { view ->
            view.scaleType = ImageView.ScaleType.FIT_CENTER
            addView(view)
        }
        addView(more)
        shape.cornerRadius = density * CORNER_DP
        background = shape
    }

    fun setColors(card: Int, text: Int) {
        shape.setColor(card)
        nameView.setTextColor(text)
        more.setTextColor(text)
        edit.setColor(text)
    }


    fun bind(
        apps: List<LaunchableApp>,
        catalog: CategoryIcons,
        actions: CategoriesActions,
        open: (View) -> Unit,
        openLongPress: (View) -> Unit
    ) {
        setOnClickListener(open)
        setOnLongClickListener { openLongPress(it); true }
        val plan = WideRow.plan(apps.size)
        used = plan.shown + if (plan.more > 0) 1 else 0
        icons.forEachIndexed { index, view ->
            val app = apps.getOrNull(index).takeIf { index < plan.shown }
            if (app == null) {
                view.visibility = GONE
                catalog.clear(view)
                view.isClickable = false
                return@forEachIndexed
            }
            view.visibility = VISIBLE
            catalog.bind(view, app)
            view.contentDescription = app.label
            if (launchFromIcons) {
                view.setOnClickListener { actions.onApp(it, app) }
                view.setOnLongClickListener { actions.onAppLongPress(it, app); true }
            } else {
                view.setOnClickListener(null)
                view.setOnLongClickListener(null)
                view.isClickable = false
            }
        }
        more.visibility = if (plan.more > 0) VISIBLE else GONE
        more.text = more.context.getString(R.string.fork_more_apps, plan.more)
        requestLayout()
    }

    private fun gap(): Int = (GAP_DP * density).toInt()

    private fun iconSize(inner: Int): Int {
        val room = (inner * ROW_SHARE).toInt() - (WideRow.SLOTS - 1) * gap()
        return min((MAX_ICON_DP * density).toInt(), max((MIN_ICON_DP * density).toInt(), room / WideRow.SLOTS))
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, (HEIGHT_DP * density).toInt())
        val inner = width - 2 * (PAD_DP * density).toInt()
        val size = iconSize(inner)
        val exact = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        icons.forEach { it.measure(exact, exact) }
        more.measure(exact, exact)
        val nameWidth = (inner - slotsWidth(size) - gap() * 2).coerceAtLeast(0)
        nameView.measure(
            MeasureSpec.makeMeasureSpec(nameWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight, MeasureSpec.AT_MOST)
        )
    }

    private fun slotsWidth(size: Int): Int = WideRow.SLOTS * size + (WideRow.SLOTS - 1) * gap()

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val pad = (PAD_DP * density).toInt()
        val height = b - t
        val size = icons[0].measuredWidth
        val nameTop = (height - nameView.measuredHeight) / 2
        nameView.layout(pad, nameTop, pad + nameView.measuredWidth, nameTop + nameView.measuredHeight)
        // The used slots sit against the right edge.
        var x = (r - l) - pad - (used * size + (used - 1).coerceAtLeast(0) * gap())
        val top = (height - size) / 2
        val items = icons.filter { it.isVisible } + listOfNotNull(more.takeIf { it.isVisible })
        icons.forEach { it.layout(0, 0, 0, 0) }
        more.layout(0, 0, 0, 0)
        items.forEach { view ->
            view.layout(x, top, x + size, top + size)
            x += size + gap()
        }
    }

    private companion object {
        const val HEIGHT_DP = TileGeometry.WIDE_HEIGHT_DP
        const val PAD_DP = 14f
        const val GAP_DP = 4f
        const val MIN_ICON_DP = 28f
        const val MAX_ICON_DP = 40f
        const val ROW_SHARE = 0.62f
        const val CORNER_DP = 24f
        const val NAME_SP = 15f
        const val MORE_SP = 12f
        const val NAME_LINES = 2
    }
}
