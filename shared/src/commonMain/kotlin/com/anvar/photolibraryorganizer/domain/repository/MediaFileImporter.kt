package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile

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
