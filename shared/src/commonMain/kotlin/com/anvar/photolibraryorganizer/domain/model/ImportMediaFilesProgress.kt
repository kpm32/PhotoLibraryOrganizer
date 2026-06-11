package com.anvar.photolibraryorganizer.domain.model

data class ImportMediaFilesProgress(
    val totalFiles: Int,
    val processedFiles: Int,
    val copiedFiles: Int,
    val movedFiles: Int,
    val skippedFiles: Int,
    val failedFiles: Int,
)
