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

// One category's own settings. Anything not stored is the default, so a
// backup from before these existed reads as it always did.
data class CategoryOptions(
    val sort: CategorySort = CategorySort.DRAWER,
    // Off the tiles; the apps stay in search.
    val hidden: Boolean = false
) {
    val isDefault: Boolean get() = sort == CategorySort.DRAWER && !hidden
}

@Serializable
internal class StoredOptions(val sort: String = "", val hidden: Boolean = false)

// The options of every category in one preference, a JSON object by the
// folder's id: `{"Ігри":{"sort":"ALPHA","hidden":true}}`. DrawerGroup, and so
// the stored groups, are not touched. A value that cannot be read is no options.
object CategoryOptionsStore {

    private val json = Json { ignoreUnknownKeys = true }

    fun decode(raw: String): Map<String, CategoryOptions> {
        val stored = if (raw.isBlank()) null else parse(raw)
        return stored?.mapValues { (_, v) -> CategoryOptions(CategorySort.fromName(v.sort), v.hidden) } ?: emptyMap()
    }

    private fun parse(raw: String): Map<String, StoredOptions>? =
        runCatching { json.decodeFromString<Map<String, StoredOptions>>(raw) }.getOrNull()

    // Defaults are not written, so a category set back to them leaves no trace.
    fun encode(options: Map<String, CategoryOptions>): String {
        val stored = options.filterValues { !it.isDefault }
            .mapValues { (_, value) -> StoredOptions(value.sort.name, value.hidden) }
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
