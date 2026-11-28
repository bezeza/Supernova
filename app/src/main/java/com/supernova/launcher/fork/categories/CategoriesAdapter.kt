package com.supernova.launcher.fork.categories

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.supernova.launcher.R
import com.supernova.launcher.data.LaunchableApp

// The rows of the Categories drawer. A row's key says which row it is, its
// content whether it changed, so a new list only rebinds what moved.
internal sealed class CategoryRow(val key: String, val content: String) {
    class Recent(val apps: List<LaunchableApp>, val slots: Int, val size: CategorySize = RecentLayout.DEFAULT) :
        CategoryRow("recent", "$slots:$size:" + apps.joinToString(",") { appKey(it) })

    class Tile(val entry: CategoryEntry, val options: CategoryOptions = CategoryOptions()) : CategoryRow(
        "tile/${entry.group.id}",
        entry.group.name + "|" + options.size + options.large + "@" + options.col + "," + options.row + "|" +
            entry.note + "|" + entry.apps.joinToString(",") { appKey(it) }
    ) {
        val slots = TileSlots.of(options)
        val layout = CategoryLayout.tile(entry.apps, slots)
    }

    class Header(val text: String) : CategoryRow("header", text)

    class Loose(val app: LaunchableApp) : CategoryRow("app/${appKey(app)}", appKey(app))

    // The way back for categories taken off the tiles.
    class Hidden(val categories: List<HiddenCategory>) :
        CategoryRow("hidden", categories.joinToString(",") { it.id })
}

// Four columns: a tile takes two, a full-width row all four, a loose app one. An adapter is many
// small overrides, so the count of functions is not a measure of it.
@Suppress("TooManyFunctions")
internal class CategoriesAdapter(
    private val icons: CategoryIcons,
    private val actions: CategoriesActions,
    // A tile tapped while arranging: its menu, for the tile with this folder id.
    private val onEditTap: (View, String) -> Unit,
    // The row of the newest installs held: its menu, for the row's own view.
    private val onRecentHold: (View) -> Unit,
    private val onShowHidden: (Context, List<HiddenCategory>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), MosaicSource {

    private var look = TileLook(textColor = 0)
    private val textColor: Int get() = look.textColor

    // The rows as the controller gave them; [items] are those on screen.
    private var base: List<CategoryRow> = emptyList()
    private var items: List<CategoryRow> = emptyList()

    // True while the tiles are being arranged: they shake, and a tap on one opens its menu.
    var editing = false
        private set

    private var columns = CellGrid.COLUMNS

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = items.size

    private fun getItem(position: Int): CategoryRow = items[position]

    // A new list: only the rows that changed are rebound.
    fun submitList(list: List<CategoryRow>) {
        base = list
        show(mosaic(list))
    }

    // How many columns the mosaic has: four, more on a wide screen. The tiles are placed anew when it changes.
    fun setColumns(count: Int) {
        if (columns == count || count < 1) return
        columns = count
        show(mosaic(base))
    }

    // Where each tile stands in the mosaic, by folder id.
    private var cells: Map<String, Placed> = emptyMap()

    // The tiles as they stand, each in its cell, and the cells shown instead while a tile is dragged (null: as
    // they stand).
    fun placed(): List<Placed> = cells.values.toList()

    fun showCells(shown: List<Placed>?) {
        if (shown == null) {
            show(mosaic(base))
        } else {
            cells = shown.associateBy { it.id }
            notifyItemRangeChanged(0, itemCount, PAYLOAD_RELAYOUT)
        }
    }

    // The tile with this folder id, as the controller gave it.
    fun tile(id: String): CategoryRow.Tile? =
        base.firstOrNull { it is CategoryRow.Tile && it.entry.group.id == id } as? CategoryRow.Tile

    fun tileIdAt(position: Int): String? = (items.getOrNull(position) as? CategoryRow.Tile)?.entry?.group?.id

    // The tiles in the cells they were left in, a new one in the first free cell, and in their reading order in
    // the list, so what the list holds in order is what the eye reads. The other rows stay where they are.
    private fun mosaic(list: List<CategoryRow>): List<CategoryRow> {
        val tiles = list.filterIsInstance<CategoryRow.Tile>()
        val placed = CellGrid.arrange(
            tiles.map { Entry(it.entry.group.id, it.options.size, it.options.col, it.options.row) }, columns
        )
        cells = placed.associateBy { it.id }
        val rank = CellGrid.order(placed).withIndex().associate { it.value to it.index }
        val ordered = tiles.sortedBy { rank[it.entry.group.id] ?: Int.MAX_VALUE }
        var next = 0
        return list.map { if (it is CategoryRow.Tile) ordered[next++] else it }
    }

    override fun cellAt(position: Int): Placed? =
        (items.getOrNull(position) as? CategoryRow.Tile)?.let { cells[it.entry.group.id] }

    // Rows of one kind are as high as each other, measured once: the row of the newest installs is a kind for each
    // of its sizes, as a strip, a half-width card and a full-width one are not as high.
    override fun kindAt(position: Int): Int {
        val type = getItemViewType(position)
        val size = (items.getOrNull(position) as? CategoryRow.Recent)?.size ?: return type
        return type + (size.ordinal + 1) * KIND_STEP
    }

    // The name under a tile, on one line (a longer name is cut short: the cell has no room for a second), and the
    // gap before it. Measured as the name is, with the font's own padding: lineHeight alone is a little less.
    private fun titleBlock(context: Context): Int {
        val probe = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TILE_LABEL_SP)
            maxLines = 1
            text = NAME_PROBE
            measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
        }
        return dp(context, TITLE_GAP_DP) + probe.measuredHeight
    }

    // How high the tile is on its own at [boxWidth]: the card the card's own geometry gives for the width the margins
    // leave, the margins, and the name under it (a small tile has its name inside its card).
    override fun naturalHeight(context: Context, position: Int, boxWidth: Int): Int {
        val row = items.getOrNull(position) as? CategoryRow.Tile ?: return 0
        val margin = dp(context, TILE_MARGIN_DP)
        val card = CardGeometry({ row.slots }, { margin }).height(boxWidth - 2 * margin)
        val name = if (row.options.size == CategorySize.SMALL) 0 else titleBlock(context)
        return card + 2 * margin + name
    }

    private fun show(new: List<CategoryRow>) {
        val old = items
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size

            override fun getNewListSize() = new.size

            override fun areItemsTheSame(oldPosition: Int, newPosition: Int) =
                Diff.areItemsTheSame(old[oldPosition], new[newPosition])

            override fun areContentsTheSame(oldPosition: Int, newPosition: Int) =
                Diff.areContentsTheSame(old[oldPosition], new[newPosition])
        })
        items = new
        diff.dispatchUpdatesTo(this)
    }

    fun setEditing(on: Boolean) {
        if (editing == on) return
        editing = on
        notifyItemRangeChanged(0, itemCount, PAYLOAD_EDIT)
    }

    // The colours or a switch moved: the rows redraw.
    fun setLook(look: TileLook) {
        if (look == this.look) return
        this.look = look
        notifyItemRangeChanged(0, itemCount)
    }

    override fun getItemId(position: Int): Long = getItem(position).key.hashCode().toLong()

    override fun getItemViewType(position: Int): Int = when (val row = getItem(position)) {
        is CategoryRow.Recent -> TYPE_RECENT
        is CategoryRow.Tile -> TYPE_TILE
        is CategoryRow.Header -> TYPE_HEADER
        is CategoryRow.Loose -> TYPE_LOOSE
        is CategoryRow.Hidden -> TYPE_HIDDEN
    }

    // A tile is two spans wide (a small one, one; a double one, four, or the row if that is narrower), a loose
    // app one, anything else the whole row.
    override fun spanAt(position: Int, spanCount: Int): Int = when (val row = getItem(position)) {
        is CategoryRow.Tile -> when (row.options.size) {
            CategorySize.NORMAL, CategorySize.TALL, CategorySize.HALF -> SPAN_TILE
            CategorySize.SMALL -> 1
            CategorySize.WIDE, CategorySize.LARGE, CategorySize.STRIP -> minOf(SPAN_DOUBLE, spanCount)
        }
        is CategoryRow.Recent -> RecentLayout.span(row.size, spanCount)
        is CategoryRow.Loose -> 1
        else -> spanCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        return when (viewType) {
            TYPE_RECENT -> RecentHolder(context)
            TYPE_TILE -> TileHolder(TileCell(context))
            TYPE_HEADER -> HeaderHolder(context)
            TYPE_HIDDEN -> HiddenHolder(context)
            else -> LooseHolder(context)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is CategoryRow.Recent -> (holder as RecentHolder).bind(row)
            is CategoryRow.Tile -> (holder as TileHolder).bind(row)
            is CategoryRow.Header -> (holder as HeaderHolder).bind(row)
            is CategoryRow.Loose -> (holder as LooseHolder).bind(row)
            is CategoryRow.Hidden -> (holder as HiddenHolder).bind(row)
        }
    }

    // Only the arranging started or stopped: the shake and the card's touches, not a whole bind.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_EDIT) && holder is Arrangeable) {
            holder.applyEditing(editing)
        } else if (payloads.contains(PAYLOAD_RELAYOUT)) {
            // Only a fresh pass of the layout: the rows are as they were bound.
            holder.itemView.requestLayout()
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        (holder as? Arrangeable)?.applyEditing(editing)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        (holder as? Arrangeable)?.applyEditing(false)
    }

    // A tile that shakes and takes drags while the tiles are arranged.
    private interface Arrangeable {
        fun applyEditing(on: Boolean)
    }

    private fun cardColor(): Int = CategoryTint.cardColor(textColor, look.backdrop)

    private fun label(context: Context, sizeSp: Float, centred: Boolean, lines: Int = 1): TextView =
        TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            maxLines = lines
            ellipsize = android.text.TextUtils.TruncateAt.END
            if (centred) gravity = Gravity.CENTER_HORIZONTAL
        }

    private fun dp(context: Context, value: Float): Int =
        (value * context.resources.displayMetrics.density).toInt()

    // The category's own icon before its name, in the drawer's text colour.
    private fun showGlyph(title: TextView, name: String) {
        val context = title.context
        val glyph = if (look.showIcons) CategoryGlyphs.forName(context, name) else null
        if (glyph == null) {
            title.setCompoundDrawablesRelative(null, null, null, null)
            return
        }
        val size = dp(context, GLYPH_DP)
        glyph.setTint(textColor)
        glyph.setBounds(0, 0, size, size)
        title.setCompoundDrawablesRelative(glyph, null, null, null)
        title.compoundDrawablePadding = dp(context, GLYPH_GAP_DP)
    }

    // A tile's card with its name under it.
    private inner class TileCell(context: Context) {
        val view = LinearLayout(context)
        private val card = CategoryTileCard(context)
        private val title = label(context, TILE_LABEL_SP, centred = true, lines = 1)

        init {
            view.orientation = LinearLayout.VERTICAL
            val margin = dp(context, TILE_MARGIN_DP)
            card.gapPx = margin
            view.setPadding(margin, margin, margin, margin)
            view.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            view.addView(title, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, TITLE_GAP_DP) })
        }

        fun bind(row: CategoryRow.Tile) {
            val entry = row.entry
            card.setColors(cardColor(), textColor)
            title.text = entry.group.name
            title.setTextColor(textColor)
            showGlyph(title, entry.group.name)
            view.contentDescription = view.context.getString(R.string.folder_desc, entry.group.name)
            // While arranging, a tap on the tile is its menu's, not the category's.
            val open = { target: View -> if (!editing) actions.onCategory(target, entry.group, entry.apps) }
            val openLongPress = { target: View -> actions.onCategoryLongPress(target, entry.group, entry.apps) }
            card.onEditTap = { onEditTap(view, entry.group.id) }
            card.launchFromIcons = look.launchFromIcons
            card.setSlots(row.slots)
            // A small tile's name is inside its card, so a pair of them is as high as a normal tile's card.
            val small = row.options.size == CategorySize.SMALL
            title.visibility = if (small) View.GONE else View.VISIBLE
            card.setName(entry.group.name)
            card.setEmptyText(entry.note)
            card.bind(row.layout, icons, actions, open = open, openLongPress = openLongPress)
            // The name under the tile opens it too.
            view.setOnClickListener(open)
            // While arranging a hold is the mosaic's: it drags the tile.
            view.setOnLongClickListener { if (editing) false else { openLongPress(it); true } }
            setEditing(editing)
        }

        fun setEditing(on: Boolean) {
            card.editing = on
        }
    }

    private inner class TileHolder(private val cell: TileCell) : RecyclerView.ViewHolder(cell.view), Arrangeable {
        private val wobble = Wobble(itemView)

        init {
            itemView.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        fun bind(row: CategoryRow.Tile) {
            cell.bind(row)
            applyEditing(editing)
        }

        override fun applyEditing(on: Boolean) {
            cell.setEditing(on)
            if (on) wobble.start() else wobble.stop()
        }
    }

    private inner class RecentHolder(context: Context) : RecyclerView.ViewHolder(LinearLayout(context)) {
        private val shape = GradientDrawable()
        private val title = label(context, TILE_LABEL_SP, centred = false, lines = TITLE_LINES)
        private val lines = List(2) { LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL } }
        private val slots = List(CategoryLayout.RECENT_MAX) { ImageView(context) }
        private val iconSize = dp(context, RECENT_ICON_DP)

        init {
            val column = itemView as LinearLayout
            column.orientation = LinearLayout.VERTICAL
            val margin = dp(context, TILE_MARGIN_DP)
            val inner = dp(context, RECENT_PADDING_DP)
            column.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(margin, margin, margin, margin) }
            shape.cornerRadius = context.resources.displayMetrics.density * RECENT_CORNER_DP
            column.background = shape
            column.setPadding(inner, inner, inner, inner)
            title.text = context.getString(R.string.fork_recent_installs_title)
            column.addView(title)
            lines.forEachIndexed { index, line ->
                val top = if (index == 0) dp(context, TITLE_GAP_DP) else 0
                column.addView(line, LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = top })
            }
            // A hold on the card itself (the icons have their own) is its menu.
            column.setOnLongClickListener { onRecentHold(it); true }
        }

        fun bind(item: CategoryRow.Recent) {
            shape.setColor(cardColor())
            title.setTextColor(textColor)
            val across = RecentLayout.columns(item.size, item.slots)
            val rowsOfCard = RecentLayout.rows(item.size)
            // The slots go into the rows of the size: [across] to a row, each as wide as a column.
            lines.forEach { it.removeAllViews() }
            slots.forEachIndexed { index, slot ->
                val line = index / across
                if (line < rowsOfCard) lines[line].addView(slot, LinearLayout.LayoutParams(0, iconSize, 1f))
                val app = item.apps.getOrNull(index)
                // Slots past the size's room are not in a row at all.
                slot.visibility = when {
                    line >= rowsOfCard || index >= RecentLayout.capacity(item.size, item.slots) -> View.GONE
                    app == null -> View.INVISIBLE
                    else -> View.VISIBLE
                }
                if (app == null) {
                    icons.clear(slot)
                    slot.setOnClickListener(null)
                    slot.setOnLongClickListener(null)
                    slot.isClickable = false
                } else {
                    icons.bind(slot, app)
                    slot.contentDescription = app.label
                    slot.setOnClickListener { actions.onApp(it, app) }
                    slot.setOnLongClickListener { actions.onAppLongPress(it, app); true }
                }
            }
            lines[1].visibility = if (rowsOfCard > 1) View.VISIBLE else View.GONE
        }
    }

    private inner class HeaderHolder(context: Context) : RecyclerView.ViewHolder(TextView(context)) {
        private val text = itemView as TextView

        init {
            text.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_SP)
            val side = dp(context, HEADER_PADDING_DP)
            text.setPadding(side, side, side, 0)
        }

        fun bind(row: CategoryRow.Header) {
            text.text = row.text
            text.setTextColor(textColor)
        }
    }

    private inner class LooseHolder(context: Context) : RecyclerView.ViewHolder(LinearLayout(context)) {
        private val icon = ImageView(context)
        private val name = label(context, LOOSE_LABEL_SP, centred = true)

        init {
            val column = itemView as LinearLayout
            column.orientation = LinearLayout.VERTICAL
            column.gravity = Gravity.CENTER_HORIZONTAL
            column.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val pad = dp(context, LOOSE_PADDING_DP)
            column.setPadding(pad, pad, pad, pad)
            val size = dp(context, LOOSE_ICON_DP)
            column.addView(icon, LinearLayout.LayoutParams(size, size))
            column.addView(name, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        fun bind(row: CategoryRow.Loose) {
            val app = row.app
            name.text = app.label
            name.setTextColor(textColor)
            name.visibility = if (app.hideLabel) View.GONE else View.VISIBLE
            itemView.contentDescription = app.label
            icons.bind(icon, app)
            itemView.setOnClickListener { actions.onApp(it, app) }
            itemView.setOnLongClickListener { actions.onAppLongPress(it, app); true }
        }
    }

    private inner class HiddenHolder(context: Context) : RecyclerView.ViewHolder(TextView(context)) {
        private val text = itemView as TextView

        init {
            text.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, HEADER_SP)
            text.gravity = Gravity.CENTER
            val pad = dp(context, HEADER_PADDING_DP)
            text.setPadding(pad, pad, pad, pad)
        }

        fun bind(row: CategoryRow.Hidden) {
            text.text = text.context.getString(R.string.fork_hidden_categories, row.categories.size)
            text.setTextColor(textColor)
            text.setOnClickListener { onShowHidden(it.context, row.categories) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<CategoryRow>() {
        override fun areItemsTheSame(old: CategoryRow, new: CategoryRow) = old.key == new.key

        override fun areContentsTheSame(old: CategoryRow, new: CategoryRow) = old.content == new.content
    }

    private companion object {
        // The text a name under a tile is measured with: only its height is wanted.
        const val NAME_PROBE = "Ag"
        const val TYPE_RECENT = 0

        // View types are small: a size of the recent row adds a multiple of this to its own.
        private const val KIND_STEP = 100
        const val TYPE_TILE = 1
        const val TYPE_HEADER = 2
        const val TYPE_LOOSE = 3
        const val TYPE_HIDDEN = 4
        const val PAYLOAD_EDIT = "edit"
        const val PAYLOAD_RELAYOUT = "relayout"
        const val SPAN_TILE = 2
        const val SPAN_DOUBLE = 4
        const val TILE_LABEL_SP = 12f
        const val TITLE_LINES = 2
        const val GLYPH_DP = 16f
        const val GLYPH_GAP_DP = 4f
        const val LOOSE_LABEL_SP = 11f
        const val HEADER_SP = 13f
        const val TILE_MARGIN_DP = 6f
        const val TITLE_GAP_DP = 6f
        const val RECENT_PADDING_DP = 12f
        const val RECENT_CORNER_DP = 24f
        const val RECENT_ICON_DP = 52f
        const val HEADER_PADDING_DP = 14f
        const val LOOSE_PADDING_DP = 6f
        const val LOOSE_ICON_DP = 48f
    }
}
