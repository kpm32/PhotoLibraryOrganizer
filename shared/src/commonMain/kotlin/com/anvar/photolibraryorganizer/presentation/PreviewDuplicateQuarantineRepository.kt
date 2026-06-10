package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.AppResult
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
}
