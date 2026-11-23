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

// The size of a category's tile: NORMAL is a square of two columns, WIDE a row
// across the screen, SMALL one column.
enum class CategorySize {
    NORMAL,
    WIDE,
    SMALL;

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
    val large: Int = LARGE_DEFAULT
) {
    val isDefault: Boolean
        get() = sort == CategorySort.DRAWER && !hidden && size == CategorySize.NORMAL && large == LARGE_DEFAULT

    companion object {
        const val LARGE_DEFAULT = 3
        val LARGE_CHOICES = listOf(3, 1, 0)

        // Anything but 3, 1 or 0 is the default.
        fun largeOrDefault(value: Int): Int = if (value in LARGE_CHOICES) value else LARGE_DEFAULT
    }
}

@Serializable
internal class StoredOptions(
    val sort: String = "",
    val hidden: Boolean = false,
    val size: String = "",
    val large: Int = CategoryOptions.LARGE_DEFAULT
)

// The options of every category in one preference, a JSON object by the
// folder's id: `{"Ігри":{"sort":"ALPHA","hidden":true}}`. DrawerGroup, and so
// the stored groups, are not touched. A value that cannot be read is no options.
object CategoryOptionsStore {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String): Map<String, CategoryOptions> {
        val stored = if (raw.isBlank()) null else parse(raw)
        return stored?.mapValues { (_, v) ->
            val large = CategoryOptions.largeOrDefault(v.large)
            CategoryOptions(CategorySort.fromName(v.sort), v.hidden, CategorySize.fromName(v.size), large)
        } ?: emptyMap()
    }

    private fun parse(raw: String): Map<String, StoredOptions>? =
        runCatching { json.decodeFromString<Map<String, StoredOptions>>(raw) }.getOrNull()

    // Defaults are not written, so a category set back to them leaves no trace.
    fun encode(options: Map<String, CategoryOptions>): String {
        val stored = options.filterValues { !it.isDefault }
            .mapValues { (_, v) -> StoredOptions(v.sort.name, v.hidden, v.size.name, v.large) }
        return if (stored.isEmpty()) "" else json.encodeToString(stored)
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
        return encode(all.filterKeys { it == id || it in existing })
    }
}
