package com.supernova.launcher.fork.categories

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.withStyledAttributes

// One of the sizes a tile can have, as an icon in the row on top of its menu.
internal class SizeChoice(val size: CategorySize, @DrawableRes val icon: Int, val label: String)

// One of the ways a tile can lay out its icons (how many are large), as an icon in the second row of the menu.
internal class LayoutChoice(val count: Int, @DrawableRes val icon: Int, val label: String)

// The layouts a tile offers, the one it has and what picking one does. Empty for a size that has one layout.
internal class LayoutRow(val choices: List<LayoutChoice>, val current: Int, val onPick: (Int) -> Unit) {
    companion object {
        val NONE = LayoutRow(emptyList(), 0) {}
    }
}

// A row of the menu under the sizes.
internal class MenuRow(val label: String, @DrawableRes val icon: Int, val onClick: () -> Unit)

// The menu of a tile: the sizes it can have as a row of icons, the one it has marked in the accent colour, and under
// them the things to do with it. It is a window over the list that does not take the finger: the touch that opened it
// goes on, and a move of it takes the tile off to be dragged ([MosaicDrag]), which closes the menu.
internal class TileMenu(private val context: Context, private val look: TileLook) {

    private val density = context.resources.displayMetrics.density
    private var popup: PopupWindow? = null

    val isShowing: Boolean get() = popup?.isShowing == true

    private fun dp(value: Float): Int = (value * density).toInt()

    @Suppress("LongParameterList") // What one menu is made of, each part used once.
    fun show(
        anchor: View,
        touchX: Float,
        touchY: Float,
        current: CategorySize,
        sizes: List<SizeChoice>,
        layouts: LayoutRow,
        rows: List<MenuRow>,
        onSize: (CategorySize) -> Unit
    ) {
        dismiss()
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(look.menuSurface)
                cornerRadius = dp(CORNER_DP).toFloat()
            }
            setPadding(dp(PAD_DP), dp(PAD_DP), dp(PAD_DP), dp(PAD_DP))
            minimumWidth = dp(MIN_WIDTH_DP)
        }
        if (sizes.isNotEmpty()) {
            content.addView(sizeRow(sizes, current) { size ->
                dismiss()
                onSize(size)
            })
            content.addView(divider())
        }
        if (layouts.choices.isNotEmpty()) {
            content.addView(layoutRow(layouts))
            content.addView(divider())
        }
        rows.forEach { row -> content.addView(rowView(row)) }
        val window = PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.isFocusable = true
        window.isOutsideTouchable = true
        window.elevation = dp(ELEVATION_DP).toFloat()
        popup = window
        content.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val screen = context.resources.displayMetrics
        val margin = dp(SCREEN_MARGIN_DP)
        val rightLimit = screen.widthPixels - content.measuredWidth - margin
        val x = (touchX - content.measuredWidth / 2).toInt().coerceIn(margin, rightLimit)
        val below = touchY.toInt() + dp(FINGER_GAP_DP)
        val y = if (below + content.measuredHeight > screen.heightPixels - margin) {
            (touchY - dp(FINGER_GAP_DP) - content.measuredHeight).toInt().coerceAtLeast(margin)
        } else {
            below
        }
        window.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
    }

    fun dismiss() {
        popup?.dismiss()
        popup = null
    }

    private fun sizeRow(sizes: List<SizeChoice>, current: CategorySize, onSize: (CategorySize) -> Unit): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        sizes.forEach { choice -> row.addView(sizeButton(choice, choice.size == current, onSize)) }
        return row
    }

    private fun layoutRow(layouts: LayoutRow): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        layouts.choices.forEach { choice ->
            row.addView(iconButton(choice.icon, choice.label, choice.count == layouts.current) {
                dismiss()
                layouts.onPick(choice.count)
            })
        }
        return row
    }

    private fun sizeButton(choice: SizeChoice, active: Boolean, onSize: (CategorySize) -> Unit): View =
        iconButton(choice.icon, choice.label, active) { onSize(choice.size) }

    private fun iconButton(@DrawableRes icon: Int, label: String, active: Boolean, onClick: () -> Unit): View =
        ImageView(context).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(if (active) look.accent else look.menuText)
            val pad = dp(SIZE_ICON_PAD_DP)
            setPadding(pad, pad, pad, pad)
            if (active) background = activeBackground()
            contentDescription = label
            TooltipCompat.setTooltipText(this, label)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(dp(SIZE_ICON_DP), dp(SIZE_ICON_DP))
        }

    // The round wash of the accent behind the size the tile has.
    private fun activeBackground() = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor((look.accent and COLOR_MASK) or (ACTIVE_ALPHA shl ALPHA_SHIFT))
    }

    private fun divider(): View = View(context).apply {
        setBackgroundColor((DIVIDER_ALPHA shl ALPHA_SHIFT) or (look.menuText and COLOR_MASK))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1f)).apply {
            topMargin = dp(PAD_DP)
            bottomMargin = dp(PAD_DP)
        }
    }

    private fun rowView(row: MenuRow): View {
        val view = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ROW_SIDE_DP), 0, dp(ROW_SIDE_DP), 0)
            minimumHeight = dp(ROW_HEIGHT_DP)
            context.withStyledAttributes(attrs = intArrayOf(android.R.attr.selectableItemBackground)) {
                foreground = getDrawable(0)
            }
            setOnClickListener {
                dismiss()
                row.onClick()
            }
        }
        view.addView(ImageView(context).apply {
            setImageResource(row.icon)
            imageTintList = ColorStateList.valueOf(look.menuText)
            layoutParams = LinearLayout.LayoutParams(dp(ROW_ICON_DP), dp(ROW_ICON_DP)).apply {
                marginEnd = dp(ROW_GAP_DP)
            }
        })
        view.addView(TextView(context).apply {
            text = row.label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, ROW_TEXT_SP)
            setTextColor(look.menuText)
        })
        return view
    }

    private companion object {
        const val CORNER_DP = 20f
        const val PAD_DP = 8f
        const val MIN_WIDTH_DP = 236f
        const val ELEVATION_DP = 8f
        const val SCREEN_MARGIN_DP = 8f
        const val FINGER_GAP_DP = 28f
        const val SIZE_ICON_DP = 44f
        const val SIZE_ICON_PAD_DP = 10f
        const val ROW_HEIGHT_DP = 48f
        const val ROW_SIDE_DP = 12f
        const val ROW_ICON_DP = 22f
        const val ROW_GAP_DP = 16f
        const val ROW_TEXT_SP = 16f
        const val ACTIVE_ALPHA = 0x33
        const val DIVIDER_ALPHA = 0x33
        const val ALPHA_SHIFT = 24
        const val COLOR_MASK = 0xFFFFFF
    }
}
