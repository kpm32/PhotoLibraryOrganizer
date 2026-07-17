package com.anvar.photolibraryorganizer.domain.model

/**
 * Final import summary shown to the user and stored in import history.
 */
data class ImportMediaFilesResult(
    val copiedFiles: Int,
    val movedFiles: Int = 0,
    val quarantinedUnsupportedFiles: Int = 0,
    val skippedFiles: Int,
    val failedFiles: Int,
    val failedUnsupportedFiles: Int = 0,
    val failureDetails: List<ImportFailureDetail> = emptyList(),
)

/**
 * Compact per-file error detail for the first failures in an import batch.
 */
data class ImportFailureDetail(
    val sourcePath: String,
    val targetPath: String,
    val reason: String,
)
