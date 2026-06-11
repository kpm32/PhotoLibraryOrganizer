package com.anvar.photolibraryorganizer.domain.model

data class UnsupportedFileQuarantineResult(
    val movedFiles: Int,
    val failedFiles: Int,
)

data class UnsupportedFileQuarantineDeleteResult(
    val deletedFiles: Int,
    val failedFiles: Int,
)
