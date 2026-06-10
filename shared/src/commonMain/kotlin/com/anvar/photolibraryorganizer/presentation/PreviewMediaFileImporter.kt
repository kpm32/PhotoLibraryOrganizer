package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter

object PreviewMediaFileImporter : MediaFileImporter {
    override suspend fun copyFiles(plannedFiles: List<PlannedMediaFile>): AppResult<ImportMediaFilesResult> {
        return AppResult.Success(
            ImportMediaFilesResult(
                copiedFiles = plannedFiles.size,
                skippedFiles = 0,
                failedFiles = 0,
            ),
        )
    }
}
