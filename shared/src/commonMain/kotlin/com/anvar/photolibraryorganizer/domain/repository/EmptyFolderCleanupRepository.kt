package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.EmptyFolderCleanupResult

interface EmptyFolderCleanupRepository {
    suspend fun deleteEmptyFolders(sourceFolder: String?): AppResult<EmptyFolderCleanupResult>
}
