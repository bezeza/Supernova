package com.supernova.launcher.fork.categories

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// How a category orders its apps, apart from the drawer's own sort: DRAWER
// follows the drawer (and a folder's manual order, as before), MANUAL the
// folder's members as stored, ALPHA by name, FREQUENCY by launches.
enum class CategorySort {
    DRAWER,
    MANUAL,
    ALPHA,
    FREQUENCY;

    companion object {
        fun fromName(name: String): CategorySort =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DRAWER
    }
}

// The size of a category's tile: NORMAL is a square of two columns, WIDE two such
// tiles side by side, TALL two one over the other, LARGE four of them (two by two),
// SMALL one column, STRIP the whole width and half a normal tile high, HALF a normal tile's width and half its
// height (two columns by one row). New sizes come last, so a backup from
// before them still reads (and an older build reads an unknown one as NORMAL).
enum class CategorySize {
    NORMAL,
    WIDE,
    SMALL,
    LARGE,
    TALL,
    STRIP,
    HALF;

    companion object {
        fun fromName(name: String): CategorySize =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NORMAL
    }
}

// One category's own settings. Anything not stored is the default, so a
// backup from before these existed reads as it always did.
data class CategoryOptions(
    val sort: CategorySort = CategorySort.DRAWER,
    // Off the tiles; the apps stay in search.
    val hidden: Boolean = false,
    val size: CategorySize = CategorySize.NORMAL,
    // How many large icons a normal tile has: 3, 1 or 0 (all small).
    val large: Int = LARGE_DEFAULT,
    // The cell the tile was put in, the top left one of its footprint; null while it was never put anywhere, and
    // the tile then stands where the order of the folders puts it.
    val col: Int? = null,
    val row: Int? = null
) {
    val isDefault: Boolean
        get() = sort == CategorySort.DRAWER && !hidden && size == CategorySize.NORMAL && large == LARGE_DEFAULT &&
            col == null && row == null

    companion object {
        const val LARGE_DEFAULT = 3

        // What a normal tile has: 3, 2, 1 or no large icons. A strip has 4 or 5 (STRIP_CHOICES), kept in the same
        // field: a normal tile reads 4 and 5 as 3, a strip reads anything under 5 as 4.
        val LARGE_CHOICES = listOf(3, 2, 1, 0)
        val STRIP_CHOICES = listOf(4, 5)
        private const val MOST = 5

        // Anything but 0 to 5 is the default.
        fun largeOrDefault(value: Int): Int = if (value in 0..MOST) value else LARGE_DEFAULT

        // The large icons a strip has: five if five were chosen, else four.
        fun stripCount(value: Int): Int = if (value >= MOST) MOST else STRIP_CHOICES.first()
    }
}

// A tile put in a cell, and given another size if [size] is not null.
class TileChange(val size: CategorySize?, val col: Int, val row: Int)

@Serializable
internal class StoredOptions(
    val sort: String = "",
    val hidden: Boolean = false,
    val size: String = "",
    val large: Int = CategoryOptions.LARGE_DEFAULT,
    // -1: no cell. Written only for a tile that has one; an older build leaves these keys alone.
    val col: Int = NO_CELL,
    val row: Int = NO_CELL
)

private const val NO_CELL = -1

// The options of every category in one preference, a JSON object by the
// folder's id: `{"Ігри":{"sort":"ALPHA","hidden":true}}`. DrawerGroup, and so
// the stored groups, are not touched. A value that cannot be read is no options.
object CategoryOptionsStore {

    // The strip of the newest installs is no folder, but it has a size and a cell of its own.
    const val RECENT_KEY = "fork:recent"
    private val KEPT_KEYS = setOf(RECENT_KEY)

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String): Map<String, CategoryOptions> {
        val stored = if (raw.isBlank()) null else parse(raw)
        return stored?.mapValues { (_, v) ->
            val large = CategoryOptions.largeOrDefault(v.large)
            val placed = v.col >= 0 && v.row >= 0
            CategoryOptions(
                CategorySort.fromName(v.sort), v.hidden, CategorySize.fromName(v.size), large,
                v.col.takeIf { placed }, v.row.takeIf { placed }
            )
        } ?: emptyMap()
    }

    private fun parse(raw: String): Map<String, StoredOptions>? =
        runCatching { json.decodeFromString<Map<String, StoredOptions>>(raw) }.getOrNull()

    // Defaults are not written, so a category set back to them leaves no trace.
    fun encode(options: Map<String, CategoryOptions>): String {
        val stored = options.filterValues { !it.isDefault }
            .mapValues { (_, v) ->
                StoredOptions(v.sort.name, v.hidden, v.size.name, v.large, v.col ?: NO_CELL, v.row ?: NO_CELL)
            }
        return if (stored.isEmpty()) "" else json.encodeToString(stored)
    }

    // [raw] with every tile in [changes] given its new cell (and size, where one is given) in one go, and the
    // categories that are gone (not in [existing]) dropped.
    fun updateTiles(raw: String, changes: Map<String, TileChange>, existing: Collection<String>): String {
        val all = decode(raw).toMutableMap()
        changes.forEach { (id, change) ->
            val now = all[id] ?: CategoryOptions()
            all[id] = now.copy(size = change.size ?: now.size, col = change.col, row = change.row)
        }
        return encode(all.filterKeys { it in changes || it in existing || it in KEPT_KEYS })
    }

    // [raw] with category [id] changed by [change], and the categories that are
    // gone (not in [existing]) dropped.
    fun update(
        raw: String,
        id: String,
        existing: Collection<String>,
        change: (CategoryOptions) -> CategoryOptions
    ): String {
        val all = decode(raw).toMutableMap()
        all[id] = change(all[id] ?: CategoryOptions())
        return encode(all.filterKeys { it == id || it in existing || it in KEPT_KEYS })
    }
}
