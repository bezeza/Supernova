package com.supernova.launcher.fork.backup

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// When a weekly copy is due, how the copies are named and which old ones go. Plain data, so none of it needs a phone.
internal object WeeklyPlan {

    const val KEEP = 4
    private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

    // Only files named exactly like this are ever taken for the launcher's own copies in a folder the user picked.
    private val FOLDER_NAME = Regex("""supernova-weekly-\d{4}-\d{2}-\d{2}-\d{6}\.json""")

    // The copies inside the app are named as its other automatic backups are, so they are listed and restored
    // with them; they live in a folder of their own, so neither kind pushes the other out.
    private val LOCAL_NAME = Regex("""layout-\d+\.json""")

    // Never made, a week old, or dated more than a week ahead (the clock was set wrong then).
    fun isDue(last: Long, now: Long): Boolean = last == 0L || abs(now - last) >= WEEK_MS

    fun localName(now: Long): String = "layout-$now.json"

    fun folderName(now: Long): String =
        "supernova-weekly-" + SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US).format(Date(now)) + ".json"

    // The names among [names] to delete after [written]: the launcher's own beyond the newest [KEEP], never the one
    // just written and never anything it did not name.
    fun surplusInFolder(names: List<String>, written: String): List<String> = surplus(names, written, FOLDER_NAME)

    fun surplusLocal(names: List<String>, written: String): List<String> = surplus(names, written, LOCAL_NAME)

    private fun surplus(names: List<String>, written: String, own: Regex): List<String> =
        names.filter { it != written && own.matches(it) }.sortedDescending().drop(KEEP - 1)
}
