package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner

/**
 * Validates the selected source folder and delegates recursive scanning.
 *
 * Keeping the null/blank source check here lets the UI stay thin and keeps
 * scanner implementations focused on filesystem traversal.
 */
class ScanSourceFolderUseCase(
    private val photoSourceScanner: PhotoSourceScanner,
) {
    suspend operator fun invoke(
        sourceFolder: String?,
        onProgress: (ScanSourceFolderProgress) -> Unit = {},
    ): AppResult<ScanSourceFolderResult> {
        val path = sourceFolder?.trim()

        if (path.isNullOrEmpty()) {
            return AppResult.Error(PhotoLibraryError.InvalidSourceFolder)
        }

        return photoSourceScanner.scanFolder(path, onProgress)
    }
}
