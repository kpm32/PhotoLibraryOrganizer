package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.ImportMode

data class ImportReport(
    val importMode: ImportMode,
    val plannedFiles: Int,
    val readyFiles: Int,
    val existingFiles: Int,
    val copiedFiles: Int,
    val movedFiles: Int,
    val skippedFiles: Int,
    val failedFiles: Int,
    val failedUnsupportedFiles: Int = 0,
    val createdAtEpochMillis: Long,
) {
    val totalFailedFiles: Int
        get() = failedFiles + failedUnsupportedFiles
}
