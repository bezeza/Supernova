package com.supernova.launcher.fork.categories

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.supernova.launcher.R

// The Categories drawer as the Drawer settings page previews it: two tiles
// drawn from the sample icons, in the drawer's colours. Inert.
object CategoriesPreview {

    private const val CARD_ALPHA = 0x26
    private const val COLOR_MASK = 0xFFFFFF
    private const val ALPHA_SHIFT = 24
    private const val MARGIN_DP = 6f
    private const val LABEL_SP = 12f
    private const val TILES = 2

    fun row(context: Context, icons: List<Drawable>, textColor: Int): View {
        val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        val margin = (MARGIN_DP * context.resources.displayMetrics.density).toInt()
        val perTile = CategoryLayout.LARGE_SLOTS + CategoryLayout.MINI_SLOTS
        repeat(TILES) { index ->
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(margin, margin, margin, margin)
            }
            val card = CategoryTileCard(context)
            card.setCardColor((textColor and COLOR_MASK) or (CARD_ALPHA shl ALPHA_SHIFT))
            // The icons repeat when the sample has fewer than a tile holds.
            val picked = List(perTile) { icons.getOrNull((index * perTile + it) % icons.size.coerceAtLeast(1)) }
            card.showPreview(
                picked.take(CategoryLayout.LARGE_SLOTS).filterNotNull(),
                picked.drop(CategoryLayout.LARGE_SLOTS).filterNotNull()
            )
            column.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            column.addView(TextView(context).apply {
                text = context.getString(R.string.style_categories)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SP)
                setTextColor(textColor)
                gravity = Gravity.CENTER_HORIZONTAL
            })
            row.addView(column, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        return row
    }
}
