package com.anvar.photolibraryorganizer.domain.model

data class LibraryIndexSnapshot(
    val destinationFolder: String,
    val libraryFiles: List<PlannedMediaFile>,
    val duplicateFiles: List<PlannedMediaFile>,
    val unsupportedFiles: List<PlannedMediaFile>,
    val updatedAtEpochMillis: Long,
)
