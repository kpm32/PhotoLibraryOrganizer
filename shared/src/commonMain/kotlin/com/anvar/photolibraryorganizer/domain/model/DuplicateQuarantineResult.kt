package com.anvar.photolibraryorganizer.domain.model

data class DuplicateQuarantineResult(
    val movedFiles: Int,
    val failedFiles: Int,
)

data class DuplicateQuarantineDeleteResult(
    val deletedFiles: Int,
    val failedFiles: Int,
)
