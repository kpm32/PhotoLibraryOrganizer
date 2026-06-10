package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineDeleteResult
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile

interface DuplicateQuarantineRepository {
    suspend fun moveToQuarantine(
        destinationFolder: String?,
        duplicateFiles: List<PlannedMediaFile>,
    ): AppResult<DuplicateQuarantineResult>

    suspend fun deleteFromQuarantine(
        destinationFolder: String?,
        quarantineFiles: List<PlannedMediaFile>,
    ): AppResult<DuplicateQuarantineDeleteResult>
}
