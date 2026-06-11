package com.anvar.photolibraryorganizer.domain.model

data class ScanSourceFolderProgress(
    val scannedFiles: Int,
    val mediaFiles: Int,
    val unsupportedFiles: Int,
    val unsupportedFileExtensions: Map<String, Int> = emptyMap(),
)
