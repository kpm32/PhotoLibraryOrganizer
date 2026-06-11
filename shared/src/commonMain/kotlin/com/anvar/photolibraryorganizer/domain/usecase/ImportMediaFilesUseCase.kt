package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter

class ImportMediaFilesUseCase(
    private val mediaFileImporter: MediaFileImporter,
) {
    suspend operator fun invoke(
        importMode: ImportMode,
        plannedFiles: List<PlannedMediaFile>,
        onProgress: (ImportMediaFilesProgress) -> Unit = {},
    ): AppResult<ImportMediaFilesResult> {
        if (plannedFiles.isEmpty()) {
            return AppResult.Error(PhotoLibraryError.ImportPlanIsEmpty)
        }

        return when (importMode) {
            ImportMode.Copy -> mediaFileImporter.copyFiles(plannedFiles, onProgress)
            ImportMode.Move -> mediaFileImporter.moveFiles(plannedFiles, onProgress)
            ImportMode.ScanOnly -> AppResult.Error(PhotoLibraryError.UnsupportedImportMode)
        }
    }
}
