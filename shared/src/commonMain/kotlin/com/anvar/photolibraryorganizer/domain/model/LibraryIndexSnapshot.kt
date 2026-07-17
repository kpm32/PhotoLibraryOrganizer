package com.anvar.photolibraryorganizer.domain.model

/**
 * Cached view of the organized library used for fast startup.
 *
 * The snapshot is disposable: if it is stale or missing, the app can rebuild it
 * from the actual folders on disk.
 */
data class LibraryIndexSnapshot(
    val destinationFolder: String,
    val libraryFiles: List<PlannedMediaFile>,
    val duplicateFiles: List<PlannedMediaFile>,
    val unsupportedFiles: List<PlannedMediaFile>,
    val updatedAtEpochMillis: Long,
)
