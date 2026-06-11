package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineDeleteResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedSourceFile
import com.anvar.photolibraryorganizer.domain.repository.UnsupportedFileQuarantineRepository

object PreviewUnsupportedFileQuarantineRepository : UnsupportedFileQuarantineRepository {
    override suspend fun moveToQuarantine(
        destinationFolder: String?,
        unsupportedFiles: List<UnsupportedSourceFile>,
    ): AppResult<UnsupportedFileQuarantineResult> {
        return AppResult.Success(
            UnsupportedFileQuarantineResult(
                movedFiles = unsupportedFiles.size,
                failedFiles = 0,
            ),
        )
    }

    override suspend fun deleteFromQuarantine(
        destinationFolder: String?,
        quarantineFiles: List<PlannedMediaFile>,
    ): AppResult<UnsupportedFileQuarantineDeleteResult> {
        return AppResult.Success(
            UnsupportedFileQuarantineDeleteResult(
                deletedFiles = quarantineFiles.size,
                failedFiles = 0,
            ),
        )
    }
}
