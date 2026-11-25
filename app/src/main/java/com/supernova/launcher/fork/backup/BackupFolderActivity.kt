package com.supernova.launcher.fork.backup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.supernova.launcher.R
import com.supernova.launcher.appGraph
import com.supernova.launcher.ui.LauncherViewModel
import kotlinx.coroutines.launch

// Nothing of its own on screen: asks the system for a folder, keeps the grant, makes the first copy there and goes.
class BackupFolderActivity : AppCompatActivity() {

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        if (tree == null) finish() else keep(tree)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            runCatching { picker.launch(null) }.onFailure {
                Toast.makeText(this, R.string.fork_backup_failed, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun keep(tree: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val state = BackupState(this)
        val kept = runCatching { contentResolver.takePersistableUriPermission(tree, flags) }
        if (kept.isFailure) {
            Toast.makeText(this, R.string.fork_backup_failed, Toast.LENGTH_LONG).show()
            finish()
            return
        }
        state.folder = tree.toString()
        state.lastInFolder = 0
        state.folderFailed = false
        val viewModel = ViewModelProvider(this, LauncherViewModel.Factory(appGraph))[LauncherViewModel::class.java]
        lifecycleScope.launch {
            WeeklyBackup.run(this@BackupFolderActivity, viewModel)
            val done = if (state.lastInFolder > 0) R.string.fork_backup_done else R.string.fork_backup_failed
            Toast.makeText(this@BackupFolderActivity, done, Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
