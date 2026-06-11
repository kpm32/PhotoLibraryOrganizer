package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineDeleteResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedSourceFile

interface UnsupportedFileQuarantineRepository {
    suspend fun moveToQuarantine(
        destinationFolder: String?,
        unsupportedFiles: List<UnsupportedSourceFile>,
    ): AppResult<UnsupportedFileQuarantineResult>

    suspend fun deleteFromQuarantine(
        destinationFolder: String?,
        quarantineFiles: List<PlannedMediaFile>,
    ): AppResult<UnsupportedFileQuarantineDeleteResult>
}
