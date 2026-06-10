package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner

class ScanSourceFolderUseCase(
    private val photoSourceScanner: PhotoSourceScanner,
) {
    suspend operator fun invoke(sourceFolder: String?): AppResult<ScanSourceFolderResult> {
        val path = sourceFolder?.trim()

        if (path.isNullOrEmpty()) {
            return AppResult.Error(PhotoLibraryError.InvalidSourceFolder)
        }

        return photoSourceScanner.scanFolder(path)
    }
}
