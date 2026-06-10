package com.anvar.photolibraryorganizer.domain.model

data class PlannedMediaFile(
    val sourcePath: String,
    val fileName: String,
    val targetRelativePath: String,
    val sizeBytes: Long,
    val targetStatus: ImportTargetStatus = ImportTargetStatus.NotChecked,
    val contentHash: String? = null,
)
