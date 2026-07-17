package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.EmptyFolderCleanupResult

/**
 * Removes empty folders left in the source tree after a move import.
 *
 * Implementations must delete folders only, never files.
 */
interface EmptyFolderCleanupRepository {
    suspend fun deleteEmptyFolders(sourceFolder: String?): AppResult<EmptyFolderCleanupResult>
}
