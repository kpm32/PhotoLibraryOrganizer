package com.anvar.photolibraryorganizer.domain.model

/**
 * Raw scanner output before import rules are applied.
 *
 * [capturedAtEpochMillis] is preferred when metadata is available; otherwise the
 * planning layer falls back to [modifiedAtEpochMillis].
 */
data class ScannedMediaFile(
    val path: String,
    val fileName: String,
    val extension: String,
    val category: MediaFileCategory,
    val sizeBytes: Long,
    val modifiedAtEpochMillis: Long,
    val capturedAtEpochMillis: Long? = null,
    val contentHash: String? = null,
)
