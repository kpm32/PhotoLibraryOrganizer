package com.anvar.photolibraryorganizer.domain.repository

/**
 * Moves one already-reviewed file to the operating system Trash.
 *
 * This contract is intentionally narrow: it is used for a selected file action
 * after the UI has already decided that deleting the source preview is safe.
 * Bulk quarantine cleanup uses its own repository contracts but shares the same
 * platform Trash implementation in the data layer.
 */
fun interface FileTrashRepository {
    suspend fun moveToTrash(path: String): Boolean
}
