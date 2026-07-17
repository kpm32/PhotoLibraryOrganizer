package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineDeleteResult
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile

/**
 * Manages exact duplicate files after they have been identified by content hash.
 *
 * The repository separates two operations on purpose: moving duplicate candidates
 * into `Duplicates`, then later moving the reviewed quarantine files to Trash.
 */
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
