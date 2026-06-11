package com.anvar.photolibraryorganizer.domain.model

data class ImportMediaFilesResult(
    val copiedFiles: Int,
    val movedFiles: Int = 0,
    val quarantinedUnsupportedFiles: Int = 0,
    val skippedFiles: Int,
    val failedFiles: Int,
    val failedUnsupportedFiles: Int = 0,
)
