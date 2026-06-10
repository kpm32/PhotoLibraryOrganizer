package com.anvar.photolibraryorganizer.domain.model

data class ScanSourceFolderResult(
    val sourceFolder: String,
    val summary: ScanSourceFolderSummary,
    val mediaFiles: List<ScannedMediaFile>,
)
