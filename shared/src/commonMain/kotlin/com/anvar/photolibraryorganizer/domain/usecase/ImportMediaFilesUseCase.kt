package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter

/**
 * Runs a copy or move import after the user has reviewed the plan.
 *
 * Existing target files are skipped here rather than delegated to the low-level
 * importer, keeping overwrite protection in the domain flow.
 */
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
        val importableFiles = plannedFiles.filter { it.targetStatus != ImportTargetStatus.AlreadyExists }
        val alreadyExistingFiles = plannedFiles.size - importableFiles.size

        if (importableFiles.isEmpty()) {
            return AppResult.Success(
                ImportMediaFilesResult(
                    copiedFiles = 0,
                    skippedFiles = alreadyExistingFiles,
                    failedFiles = 0,
                ),
            )
        }

        val result = when (importMode) {
            ImportMode.Copy -> mediaFileImporter.copyFiles(importableFiles, onProgress)
            ImportMode.Move -> mediaFileImporter.moveFiles(importableFiles, onProgress)
            ImportMode.ScanOnly -> AppResult.Error(PhotoLibraryError.UnsupportedImportMode)
        }

        return when (result) {
            is AppResult.Success -> result.copy(
                data = result.data.copy(skippedFiles = result.data.skippedFiles + alreadyExistingFiles),
            )
            is AppResult.Error -> result
        }
    }
}
