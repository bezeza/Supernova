package com.supernova.launcher.fork.categories

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
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
import com.supernova.launcher.R

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

// The side in pixels of a button of the menu's rows of icons: [SIZE_ICON_DP], or less where [count] of them
// would not fit the screen's width.
private const val SIZE_ICON_DP = 48f
private const val SCREEN_MARGIN_DP = 8f
private const val MENU_PAD_DP = 8f

private fun buttonSide(context: Context, count: Int): Int {
    val density = context.resources.displayMetrics.density
    val room = context.resources.displayMetrics.widthPixels - 2 * ((SCREEN_MARGIN_DP + MENU_PAD_DP) * density).toInt()
    return minOf((SIZE_ICON_DP * density).toInt(), room / maxOf(count, 1))
}

// The menu of a tile: the sizes it can have as a row of icons, the one it has marked in the accent colour, and under
// them the things to do with it. It is a window over the list that does not take the finger: the touch that opened it
// goes on, and a move of it takes the tile off to be dragged ([MosaicDrag]), which closes the menu.
internal class TileMenu(private val context: Context, private val look: TileLook) {

    private val density = context.resources.displayMetrics.density
    private var popup: PopupWindow? = null

    val isShowing: Boolean get() = popup?.isShowing == true

    // Told when the menu goes, however it goes.
    var onDismiss: () -> Unit = {}

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
        keepClear: Rect?,
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
            sizes.firstOrNull { it.size == current }?.let { choice ->
                val footprint = CellFlow.footprint(choice.size)
                val text = context.getString(R.string.fork_menu_caption, choice.label, footprint.w, footprint.h)
                content.addView(caption(text))
            }
            if (layouts.choices.isNotEmpty() || rows.isNotEmpty()) content.addView(divider())
        }
        if (layouts.choices.isNotEmpty()) {
            content.addView(layoutRow(layouts))
            layouts.choices.firstOrNull { it.count == layouts.current }?.let { content.addView(caption(it.label)) }
            content.addView(divider())
        }
        rows.forEach { row -> content.addView(rowView(row)) }
        val window = PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // Not focusable: the touches outside it reach the list, where the corner arc of the tile is.
        window.isFocusable = false
        window.isOutsideTouchable = false
        window.setOnDismissListener { onDismiss() }
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
        val room = MenuSpot.Room(screen.widthPixels, screen.heightPixels, margin, dp(ARC_CLEARANCE_DP))
        val avoid = keepClear?.let { MenuSpot.Box(it.left, it.top, it.right, it.bottom) }
        val want = MenuSpot.Box(x, y, x + content.measuredWidth, y + content.measuredHeight)
        val spot = MenuSpot.place(want, avoid, room)
        window.showAtLocation(anchor, Gravity.NO_GRAVITY, spot.first, spot.second)
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
        val button = buttonSide(context, sizes.size)
        sizes.forEach { choice ->
            row.addView(iconButton(choice.icon, choice.label, choice.size == current, button) { onSize(choice.size) })
        }
        return row
    }

    private fun layoutRow(layouts: LayoutRow): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val button = buttonSide(context, layouts.choices.size)
        layouts.choices.forEach { choice ->
            row.addView(iconButton(choice.icon, choice.label, choice.count == layouts.current, button) {
                dismiss()
                layouts.onPick(choice.count)
            })
        }
        return row
    }

    // The name of what is marked, one line under the icons, as the icons alone are small to tell apart.
    private fun caption(text: String): View = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_SP, CAPTION_SP)
        setTextColor(look.menuText)
        setPadding(0, dp(CAPTION_TOP_DP), 0, 0)
    }

    private fun iconButton(
        @DrawableRes icon: Int,
        label: String,
        active: Boolean,
        button: Int,
        onClick: () -> Unit
    ): View =
        ImageView(context).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(if (active) look.accent else look.menuText)
            val pad = button / PAD_DIVISOR
            setPadding(pad, pad, pad, pad)
            if (active) background = activeBackground()
            contentDescription = label
            TooltipCompat.setTooltipText(this, label)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(button, button)
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
        const val ARC_CLEARANCE_DP = 12f
        const val FINGER_GAP_DP = 28f
        // A button is a sixth padding on each side, so its icon is two thirds of it: 32 dp in 48.
        const val PAD_DIVISOR = 6
        const val CAPTION_SP = 13f
        const val CAPTION_TOP_DP = 2f
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
