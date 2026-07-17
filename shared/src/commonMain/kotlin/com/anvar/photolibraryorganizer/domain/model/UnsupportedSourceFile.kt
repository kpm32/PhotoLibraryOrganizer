package com.anvar.photolibraryorganizer.domain.model

/**
 * Non-media file discovered during source scanning.
 *
 * These files can be moved into `Unsupported` during move import so the source
 * folder can become empty without silently deleting unknown content.
 */
data class UnsupportedSourceFile(
    val path: String,
    val relativePath: String,
    val fileName: String,
    val extensionLabel: String,
    val sizeBytes: Long,
    val modifiedAtEpochMillis: Long,
)
