package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.EmptyFolderCleanupResult
import com.anvar.photolibraryorganizer.domain.repository.EmptyFolderCleanupRepository

object PreviewEmptyFolderCleanupRepository : EmptyFolderCleanupRepository {
    override suspend fun deleteEmptyFolders(sourceFolder: String?): AppResult<EmptyFolderCleanupResult> {
        return AppResult.Success(
            EmptyFolderCleanupResult(
                deletedFolders = 3,
                failedFolders = 0,
            ),
        )
    }
}
