package com.supernova.launcher.fork.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.IOException

// The folder the user picked, through the system's document access. Throws where the folder is gone or refuses.
internal class BackupFolder(private val resolver: ContentResolver, private val tree: Uri) {

    private val root: Uri =
        DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    // The folder's own name, null where it cannot be read.
    fun name(): String? = runCatching {
        resolver.query(root, arrayOf(Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }.getOrNull()

    // Writes [text] as a new file [name], then clears the launcher's own surplus copies.
    fun add(name: String, text: String) {
        val file = DocumentsContract.createDocument(resolver, root, MIME, name)
            ?: throw IOException("the folder made no file")
        val stream = resolver.openOutputStream(file, "wt") ?: throw IOException("the file did not open")
        stream.use { it.write(text.toByteArray()) }
        val files = list()
        WeeklyPlan.surplusInFolder(files.keys.toList(), name).forEach { old ->
            // A copy that will not go is no reason to call the new one failed.
            runCatching { DocumentsContract.deleteDocument(resolver, files.getValue(old)) }
        }
    }

    private fun list(): Map<String, Uri> {
        val id = DocumentsContract.getTreeDocumentId(tree)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME)
        val found = mutableMapOf<String, Uri>()
        resolver.query(children, columns, null, null, null)?.use {
            while (it.moveToNext()) {
                found[it.getString(1)] = DocumentsContract.buildDocumentUriUsingTree(tree, it.getString(0))
            }
        }
        return found
    }

    private companion object {
        const val MIME = "application/json"
    }
}
