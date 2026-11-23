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
    class Recent(val apps: List<LaunchableApp>, val slots: Int) :
        CategoryRow("recent", slots.toString() + ":" + apps.joinToString(",") { appKey(it) })

    class Tile(val entry: CategoryEntry, val options: CategoryOptions = CategoryOptions()) : CategoryRow(
        "tile/${entry.group.id}",
        entry.group.name + "|" + options.size + options.large + "|" + entry.apps.joinToString(",") { appKey(it) }
    ) {
        val slots = if (options.size == CategorySize.SMALL) TileSlots.small() else TileSlots.normal(options.large)
        val layout = CategoryLayout.tile(entry.apps, slots)
    }

    class Header(val text: String) : CategoryRow("header", text)

    class Loose(val app: LaunchableApp) : CategoryRow("app/${appKey(app)}", appKey(app))

    // The bar on top while the tiles are arranged: how, and the button that ends it.
    object Done : CategoryRow("done", "done")

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
    private val editHandlers: EditHandlers,
    private val onShowHidden: (Context, List<HiddenCategory>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var look = TileLook(textColor = 0)
    private val textColor: Int get() = look.textColor

    // The rows as the controller gave them; [items] are those on screen: with the bar on top while arranging.
    private var base: List<CategoryRow> = emptyList()
    private var items: List<CategoryRow> = emptyList()

    // True while the tiles are being arranged: they shake, take drags and open their settings on a tap.
    var editing = false
        private set

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int = items.size

    private fun getItem(position: Int): CategoryRow = items[position]

    // A new list: only the rows that changed are rebound.
    fun submitList(list: List<CategoryRow>) {
        base = list
        show(withBar(list))
    }

    private fun withBar(list: List<CategoryRow>): List<CategoryRow> =
        if (editing) listOf(CategoryRow.Done) + list else list

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
        show(withBar(base))
        notifyItemRangeChanged(0, itemCount, PAYLOAD_EDIT)
    }

    // A drag moved the tile at [from] to [to]: only over another tile.
    fun move(from: Int, to: Int): Boolean {
        if (items.getOrNull(from) !is CategoryRow.Tile || items.getOrNull(to) !is CategoryRow.Tile) return false
        items = CategoryReorder.moved(items, from, to)
        base = items.filter { it !is CategoryRow.Done }
        notifyItemMoved(from, to)
        return true
    }

    fun isTile(position: Int): Boolean = items.getOrNull(position) is CategoryRow.Tile

    // A corner dragged to another size: the tile takes it at once, without waiting for the saved setting to
    // come back round, and the setting is saved. What comes back the same changes nothing.
    private fun resize(row: CategoryRow.Tile, dx: Float, step: Float) {
        val size = TileResize.resize(row.options.size, dx, step)
        if (size == row.options.size) return
        val id = row.entry.group.id
        val at = items.indexOfFirst { it is CategoryRow.Tile && it.entry.group.id == id }
        if (at < 0) return
        val sized = CategoryRow.Tile(row.entry, row.options.copy(size = size))
        items = items.toMutableList().also { it[at] = sized }
        base = items.filter { it !is CategoryRow.Done }
        notifyItemChanged(at)
        editHandlers.onResize(id, size)
    }

    // The folders' ids in the order the tiles are in now.
    fun tileIds(): List<String> = items.filterIsInstance<CategoryRow.Tile>().map { it.entry.group.id }

    // The colours or a switch moved: the rows redraw.
    fun setLook(look: TileLook) {
        if (look == this.look) return
        this.look = look
        notifyItemRangeChanged(0, itemCount)
    }

    override fun getItemId(position: Int): Long = getItem(position).key.hashCode().toLong()

    override fun getItemViewType(position: Int): Int = when (val row = getItem(position)) {
        is CategoryRow.Recent -> TYPE_RECENT
        is CategoryRow.Tile -> if (row.options.size == CategorySize.WIDE) TYPE_WIDE else TYPE_TILE
        is CategoryRow.Header -> TYPE_HEADER
        is CategoryRow.Loose -> TYPE_LOOSE
        is CategoryRow.Hidden -> TYPE_HIDDEN
        CategoryRow.Done -> TYPE_DONE
    }

    // A tile is two spans wide (a small one, one; a wide one, the row), a loose app one, anything else the whole row.
    fun spanAt(position: Int, spanCount: Int): Int = when (val row = getItem(position)) {
        is CategoryRow.Tile -> when (row.options.size) {
            CategorySize.NORMAL -> SPAN_TILE
            CategorySize.SMALL -> 1
            CategorySize.WIDE -> spanCount
        }
        is CategoryRow.Loose -> 1
        else -> spanCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val context = parent.context
        return when (viewType) {
            TYPE_RECENT -> RecentHolder(context)
            TYPE_TILE -> TileHolder(context)
            TYPE_WIDE -> WideHolder(context)
            TYPE_HEADER -> HeaderHolder(context)
            TYPE_HIDDEN -> HiddenHolder(context)
            TYPE_DONE -> DoneHolder(context)
            else -> LooseHolder(context)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is CategoryRow.Recent -> (holder as RecentHolder).bind(row)
            is CategoryRow.Tile -> if (holder is WideHolder) holder.bind(row) else (holder as TileHolder).bind(row)
            is CategoryRow.Header -> (holder as HeaderHolder).bind(row)
            is CategoryRow.Loose -> (holder as LooseHolder).bind(row)
            is CategoryRow.Hidden -> (holder as HiddenHolder).bind(row)
            CategoryRow.Done -> (holder as DoneHolder).bind()
        }
    }

    // Only the arranging started or stopped: the shake and the card's touches, not a whole bind.
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_EDIT) && holder is Arrangeable) {
            holder.applyEditing(editing)
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

    private inner class TileHolder(context: Context) : RecyclerView.ViewHolder(LinearLayout(context)), Arrangeable {
        private val card = CategoryTileCard(context)
        private val wobble = Wobble(itemView)
        private val title = label(context, TILE_LABEL_SP, centred = true, lines = TITLE_LINES)

        init {
            val column = itemView as LinearLayout
            column.orientation = LinearLayout.VERTICAL
            val margin = dp(context, TILE_MARGIN_DP)
            column.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            column.setPadding(margin, margin, margin, margin)
            column.addView(card, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            column.addView(title, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, TITLE_GAP_DP) })
        }

        fun bind(row: CategoryRow.Tile) {
            val entry = row.entry
            card.setColors(cardColor(), textColor)
            title.text = entry.group.name
            title.setTextColor(textColor)
            showGlyph(title, entry.group.name)
            itemView.contentDescription = itemView.context.getString(R.string.folder_desc, entry.group.name)
            // While arranging, a tap is for the tile's settings, not for opening it.
            val open = { view: View ->
                if (editing) editHandlers.onTap(view, entry) else actions.onCategory(view, entry.group, entry.apps)
            }
            val openLongPress = { view: View -> actions.onCategoryLongPress(view, entry.group, entry.apps) }
            card.edit.onResize = { dx -> resize(row, dx, card.edit.stepPx(itemView.parent as? View)) }
            card.launchFromIcons = look.launchFromIcons
            card.setSlots(row.slots)
            // A small tile's name is smaller too, to fit its narrower column.
            val labelSp = if (row.options.size == CategorySize.SMALL) SMALL_LABEL_SP else TILE_LABEL_SP
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSp)
            card.bind(row.layout, icons, actions, open = open, openLongPress = openLongPress)
            // The name under the tile opens it too.
            itemView.setOnClickListener(open)
            // While arranging a hold is the list's: it drags the tile.
            itemView.setOnLongClickListener { if (editing) false else { openLongPress(it); true } }
            applyEditing(editing)
        }

        override fun applyEditing(on: Boolean) {
            card.edit.editing = on
            if (on) wobble.start() else wobble.stop()
        }
    }

    private inner class WideHolder(context: Context) : RecyclerView.ViewHolder(CategoryWideCard(context)), Arrangeable {
        private val card = itemView as CategoryWideCard
        private val wobble = Wobble(itemView)

        init {
            val margin = dp(context, TILE_MARGIN_DP)
            card.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(margin, margin, margin, margin) }
        }

        fun bind(row: CategoryRow.Tile) {
            val entry = row.entry
            card.setColors(cardColor(), textColor)
            card.nameView.text = entry.group.name
            showGlyph(card.nameView, entry.group.name)
            card.contentDescription = card.context.getString(R.string.folder_desc, entry.group.name)
            card.launchFromIcons = look.launchFromIcons
            card.edit.onResize = { dx -> resize(row, dx, card.edit.stepPx(itemView.parent as? View)) }
            card.bind(
                entry.apps, icons, actions,
                open = { view ->
                    if (editing) editHandlers.onTap(view, entry) else actions.onCategory(view, entry.group, entry.apps)
                },
                openLongPress = { view -> actions.onCategoryLongPress(view, entry.group, entry.apps) }
            )
            applyEditing(editing)
        }

        override fun applyEditing(on: Boolean) {
            card.edit.editing = on
            if (on) wobble.start() else wobble.stop()
        }
    }

    // The bar on top while arranging: how, and the button that ends it.
    private inner class DoneHolder(context: Context) : RecyclerView.ViewHolder(LinearLayout(context)) {
        private val hint = label(context, HINT_SP, centred = false, lines = HINT_LINES)
        private val done = label(context, DONE_SP, centred = true)
        private val shape = GradientDrawable()

        init {
            val bar = itemView as LinearLayout
            bar.orientation = LinearLayout.HORIZONTAL
            bar.gravity = Gravity.CENTER_VERTICAL
            val pad = dp(context, DONE_PADDING_DP)
            bar.setPadding(pad, pad / 2, pad, pad / 2)
            bar.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            hint.setText(R.string.fork_edit_hint)
            bar.addView(hint, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            done.setText(R.string.fork_edit_done)
            shape.cornerRadius = dp(context, DONE_CORNER_DP).toFloat()
            done.background = shape
            done.setPadding(pad, pad / 2, pad, pad / 2)
            bar.addView(done, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = pad })
            done.setOnClickListener { editHandlers.onDone() }
        }

        fun bind() {
            hint.setTextColor(textColor)
            done.setTextColor(textColor)
            shape.setColor(cardColor())
        }
    }

    private inner class RecentHolder(context: Context) : RecyclerView.ViewHolder(LinearLayout(context)) {
        private val shape = GradientDrawable()
        private val title = label(context, TILE_LABEL_SP, centred = false, lines = TITLE_LINES)
        private val row = LinearLayout(context)
        private val slots = List(CategoryLayout.RECENT_MAX) { ImageView(context) }

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
            row.orientation = LinearLayout.HORIZONTAL
            val iconSize = dp(context, RECENT_ICON_DP)
            slots.forEach { slot ->
                row.addView(slot, LinearLayout.LayoutParams(0, iconSize, 1f))
            }
            column.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, TITLE_GAP_DP) })
        }

        fun bind(item: CategoryRow.Recent) {
            shape.setColor(cardColor())
            title.setTextColor(textColor)
            slots.forEachIndexed { index, slot ->
                val app = item.apps.getOrNull(index)
                // Slots past the chosen count are not in the row at all.
                slot.visibility = when {
                    index >= item.slots -> View.GONE
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
        const val TYPE_RECENT = 0
        const val TYPE_TILE = 1
        const val TYPE_HEADER = 2
        const val TYPE_LOOSE = 3
        const val TYPE_HIDDEN = 4
        const val TYPE_WIDE = 5
        const val TYPE_DONE = 6
        const val PAYLOAD_EDIT = "edit"
        const val HINT_SP = 11f
        const val HINT_LINES = 3
        const val DONE_SP = 14f
        const val DONE_PADDING_DP = 12f
        const val DONE_CORNER_DP = 18f
        const val SPAN_TILE = 2
        const val TILE_LABEL_SP = 12f
        const val SMALL_LABEL_SP = 10f
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
