package com.anvar.photolibraryorganizer.domain.model

data class EmptyFolderCleanupResult(
    val deletedFolders: Int,
    val failedFolders: Int,
)
