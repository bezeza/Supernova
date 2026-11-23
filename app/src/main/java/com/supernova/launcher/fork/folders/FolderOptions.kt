package com.supernova.launcher.fork.folders

import com.supernova.launcher.ui.FolderPreviewStyle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// How a folder's icon is drawn, apart from the settings' own choice: DEFAULT
// follows them, GRID is 2 by 2, GRID_3X3 is 3 by 3, SINGLE the first app alone.
enum class FolderLook {
    DEFAULT,
    GRID,
    GRID_3X3,
    SINGLE;

    companion object {
        fun fromName(name: String): FolderLook =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
    }
}

// One folder's own settings: the window's columns (0 = as the settings say), how
// opaque the window is (null = as the style says), the name in the window, and
// the icon. Anything not stored is the default, so older backups read as ever.
data class FolderOptions(
    val columns: Int = 0,
    val tint: Int? = null,
    val hideTitle: Boolean = false,
    val look: FolderLook = FolderLook.DEFAULT
) {
    val isDefault: Boolean
        get() = columns == 0 && tint == null && !hideTitle && look == FolderLook.DEFAULT

    companion object {
        const val COLUMNS_MAX = 6
        const val TINT_MAX = 100
        val TINT_CHOICES = listOf(TINT_MAX, 80, 60, 40, 20)

        fun columnsOrDefault(value: Int): Int = if (value in 1..COLUMNS_MAX) value else 0
        fun tintOrDefault(value: Int?): Int? = value?.takeIf { it in 0..TINT_MAX }
    }
}

@Serializable
internal class StoredFolderOptions(
    val columns: Int = 0,
    val tint: Int? = null,
    val hideTitle: Boolean = false,
    val look: String = ""
)

// Every folder's options in one preference, a JSON object by the folder's id (a
// placed folder's item id, a drawer folder's group id). A value that cannot be
// read is no options; defaults are not written.
object FolderOptionsStore {

    private val json = Json { ignoreUnknownKeys = true }

    private fun parse(raw: String): Map<String, StoredFolderOptions>? =
        runCatching { json.decodeFromString<Map<String, StoredFolderOptions>>(raw) }.getOrNull()

    fun decode(raw: String): Map<String, FolderOptions> {
        val stored = if (raw.isBlank()) null else parse(raw)
        return stored?.mapValues { (_, v) ->
            FolderOptions(
                FolderOptions.columnsOrDefault(v.columns), FolderOptions.tintOrDefault(v.tint), v.hideTitle,
                FolderLook.fromName(v.look)
            )
        } ?: emptyMap()
    }

    fun encode(options: Map<String, FolderOptions>): String {
        val stored = options.filterValues { !it.isDefault }
            .mapValues { (_, v) -> StoredFolderOptions(v.columns, v.tint, v.hideTitle, v.look.name) }
        return if (stored.isEmpty()) "" else json.encodeToString(stored)
    }

    fun update(raw: String, key: String, change: (FolderOptions) -> FolderOptions): String {
        val all = decode(raw).toMutableMap()
        all[key] = change(all[key] ?: FolderOptions())
        return encode(all)
    }
}

// What an icon look asks of the preview: the style, whether the first app is drawn large, and how many apps.
class PreviewSpec(val style: FolderPreviewStyle, val prioritizeFirst: Boolean, val limit: Int?)

object FolderLooks {

    // The preview a folder's look asks for, or null for the settings' own.
    fun spec(look: FolderLook): PreviewSpec? = when (look) {
        FolderLook.DEFAULT -> null
        FolderLook.GRID -> PreviewSpec(FolderPreviewStyle.GRID, prioritizeFirst = false, limit = null)
        FolderLook.GRID_3X3 -> PreviewSpec(FolderPreviewStyle.GRID_3X3, prioritizeFirst = false, limit = null)
        FolderLook.SINGLE -> PreviewSpec(FolderPreviewStyle.GRID, prioritizeFirst = true, limit = 1)
    }
}

// A folder named just before its window opens, so the window can find its own options.
class PendingFolder(val options: FolderOptions?, val at: Long) {
    companion object {
        // A name that was not used within this is stale: another window's.
        const val VALID_MS = 1000L

        fun usable(pending: PendingFolder?, now: Long): FolderOptions? =
            pending?.takeIf { now - it.at in 0..VALID_MS }?.options
    }
}
