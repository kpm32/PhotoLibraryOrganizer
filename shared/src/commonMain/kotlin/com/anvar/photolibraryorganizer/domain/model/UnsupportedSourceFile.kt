package com.anvar.photolibraryorganizer.domain.model

data class UnsupportedSourceFile(
    val path: String,
    val relativePath: String,
    val fileName: String,
    val extensionLabel: String,
    val sizeBytes: Long,
    val modifiedAtEpochMillis: Long,
)
