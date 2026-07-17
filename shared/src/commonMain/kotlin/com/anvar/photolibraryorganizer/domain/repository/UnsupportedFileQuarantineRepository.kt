package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineDeleteResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedSourceFile

/**
 * Manages files that were found during scan but are not part of the supported
 * media library.
 *
 * Unsupported files are never silently deleted from the source. In move mode
 * they are first relocated into the library's `Unsupported` area, where the user
 * can review them and later move that quarantine content to the OS Trash.
 */
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
