package com.anvar.photolibraryorganizer.domain.model

/**
 * Best-effort scan progress. The total file count is unknown during recursive
 * traversal, so progress is expressed as counters rather than a percentage.
 */
data class ScanSourceFolderProgress(
    val scannedFiles: Int,
    val mediaFiles: Int,
    val unsupportedFiles: Int,
    val unsupportedFileExtensions: Map<String, Int> = emptyMap(),
)
