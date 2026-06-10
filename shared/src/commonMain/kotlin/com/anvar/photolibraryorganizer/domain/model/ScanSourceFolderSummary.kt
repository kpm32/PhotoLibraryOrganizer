package com.anvar.photolibraryorganizer.domain.model

data class ScanSourceFolderSummary(
    val scannedFiles: Int,
    val mediaFiles: Int,
    val imageFiles: Int,
    val videoFiles: Int,
    val unsupportedFiles: Int,
    val totalMediaBytes: Long,
)
