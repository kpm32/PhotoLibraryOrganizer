package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineDeleteResult
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.DuplicateQuarantineRepository

object PreviewDuplicateQuarantineRepository : DuplicateQuarantineRepository {
    override suspend fun moveToQuarantine(
        destinationFolder: String?,
        duplicateFiles: List<PlannedMediaFile>,
    ): AppResult<DuplicateQuarantineResult> {
        return AppResult.Success(
            DuplicateQuarantineResult(
                movedFiles = duplicateFiles.size,
                failedFiles = 0,
            ),
        )
    }

    override suspend fun deleteFromQuarantine(
        destinationFolder: String?,
        quarantineFiles: List<PlannedMediaFile>,
    ): AppResult<DuplicateQuarantineDeleteResult> {
        return AppResult.Success(
            DuplicateQuarantineDeleteResult(
                deletedFiles = quarantineFiles.size,
                failedFiles = 0,
            ),
        )
    }
}
