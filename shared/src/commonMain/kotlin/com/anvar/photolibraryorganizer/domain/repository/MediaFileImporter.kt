package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile

/**
 * Executes a reviewed import plan against the filesystem.
 *
 * The domain layer decides what should be imported and where it should go.
 * Implementations perform the actual copy/move work and report incremental
 * progress without deciding the import rules themselves.
 */
interface MediaFileImporter {
    suspend fun copyFiles(
        plannedFiles: List<PlannedMediaFile>,
        onProgress: (ImportMediaFilesProgress) -> Unit = {},
    ): AppResult<ImportMediaFilesResult>

    suspend fun moveFiles(
        plannedFiles: List<PlannedMediaFile>,
        onProgress: (ImportMediaFilesProgress) -> Unit = {},
    ): AppResult<ImportMediaFilesResult>
}
