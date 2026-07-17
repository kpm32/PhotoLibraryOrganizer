package com.anvar.photolibraryorganizer.domain.model

/**
 * Incremental progress emitted while a reviewed import plan is being copied or moved.
 */
data class ImportMediaFilesProgress(
    val totalFiles: Int,
    val processedFiles: Int,
    val copiedFiles: Int,
    val movedFiles: Int,
    val skippedFiles: Int,
    val failedFiles: Int,
)
