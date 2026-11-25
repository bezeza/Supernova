package com.supernova.launcher.fork.backup

import android.content.Context
import androidx.core.net.toUri
import com.supernova.launcher.ui.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

// The weekly copy of the layout and settings, in the same form as "Export backup": one inside the app, and one in
// the folder the user picked, if any. No service and no schedule: it is looked at when the launcher comes to the
// front.
internal object WeeklyBackup {

    // Where the copies inside the app are; LauncherRepository lists them with its own automatic backups.
    const val LOCAL_DIR = "weekly_backups"

    private val lock = Mutex()

    // Makes what is due. [force]: make both now, whatever the date.
    suspend fun run(context: Context, viewModel: LauncherViewModel, force: Boolean = false) = lock.withLock {
        val state = BackupState(context)
        val now = System.currentTimeMillis()
        val local = force || WeeklyPlan.isDue(state.lastLocal, now)
        val folder = state.folder?.takeIf { force || WeeklyPlan.isDue(state.lastInFolder, now) }
        if (!local && folder == null) return@withLock
        // Nothing to keep before the home screen is there.
        val loaded = viewModel.state.first { it.loaded }
        if (loaded.model.items.isEmpty()) return@withLock
        val text = viewModel.exportBackup()
        withContext(Dispatchers.IO) {
            if (local) runCatching { writeLocal(context, now, text) }.onSuccess { state.lastLocal = now }
            if (folder != null) {
                val written = runCatching {
                    BackupFolder(context.contentResolver, folder.toUri()).add(WeeklyPlan.folderName(now), text)
                }
                state.folderFailed = written.isFailure
                if (written.isSuccess) state.lastInFolder = now
            }
        }
    }

    // Whole or not at all: written aside, then renamed into place.
    private fun writeLocal(context: Context, now: Long, text: String) {
        val dir = File(context.filesDir, LOCAL_DIR).apply { mkdirs() }
        val name = WeeklyPlan.localName(now)
        val part = File(dir, "$name.part")
        part.writeText(text)
        check(part.renameTo(File(dir, name))) { "the copy did not move into place" }
        WeeklyPlan.surplusLocal(dir.list().orEmpty().toList(), name).forEach { File(dir, it).delete() }
    }
}
