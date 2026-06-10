package com.anvar.photolibraryorganizer.domain.model

data class ScannedMediaFile(
    val path: String,
    val fileName: String,
    val extension: String,
    val category: MediaFileCategory,
    val sizeBytes: Long,
)
