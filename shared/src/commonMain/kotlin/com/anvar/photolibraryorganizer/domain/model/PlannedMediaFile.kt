package com.anvar.photolibraryorganizer.domain.model

/**
 * A media file as the app presents it after planning or indexing.
 *
 * During import, [sourcePath] is the original file and [targetRelativePath] is
 * the planned destination. In library views, both values can point to the
 * already-organized file on disk.
 */
data class PlannedMediaFile(
    val sourcePath: String,
    val fileName: String,
    val targetRelativePath: String,
    val sizeBytes: Long,
    val targetStatus: ImportTargetStatus = ImportTargetStatus.NotChecked,
    val contentHash: String? = null,
    val capturedAtEpochMillis: Long? = null,
    val modifiedAtEpochMillis: Long? = null,
)
