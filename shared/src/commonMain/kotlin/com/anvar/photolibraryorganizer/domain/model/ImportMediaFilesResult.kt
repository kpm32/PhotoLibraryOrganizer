package com.anvar.photolibraryorganizer.domain.model

data class ImportMediaFilesResult(
    val copiedFiles: Int,
    val skippedFiles: Int,
    val failedFiles: Int,
)
