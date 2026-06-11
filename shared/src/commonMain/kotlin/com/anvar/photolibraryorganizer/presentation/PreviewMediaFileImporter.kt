package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter

object PreviewMediaFileImporter : MediaFileImporter {
    override suspend fun copyFiles(
        plannedFiles: List<PlannedMediaFile>,
        onProgress: (ImportMediaFilesProgress) -> Unit,
    ): AppResult<ImportMediaFilesResult> {
        onProgress(
            ImportMediaFilesProgress(
                totalFiles = plannedFiles.size,
                processedFiles = plannedFiles.size,
                copiedFiles = plannedFiles.size,
                movedFiles = 0,
                skippedFiles = 0,
                failedFiles = 0,
            ),
        )
        return AppResult.Success(
            ImportMediaFilesResult(
                copiedFiles = plannedFiles.size,
                movedFiles = 0,
                skippedFiles = 0,
                failedFiles = 0,
            ),
        )
    }

    override suspend fun moveFiles(
        plannedFiles: List<PlannedMediaFile>,
        onProgress: (ImportMediaFilesProgress) -> Unit,
    ): AppResult<ImportMediaFilesResult> {
        onProgress(
            ImportMediaFilesProgress(
                totalFiles = plannedFiles.size,
                processedFiles = plannedFiles.size,
                copiedFiles = 0,
                movedFiles = plannedFiles.size,
                skippedFiles = 0,
                failedFiles = 0,
            ),
        )
        return AppResult.Success(
            ImportMediaFilesResult(
                copiedFiles = 0,
                movedFiles = plannedFiles.size,
                skippedFiles = 0,
                failedFiles = 0,
            ),
        )
    }
}
