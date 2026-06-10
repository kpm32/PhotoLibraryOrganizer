package com.anvar.photolibraryorganizer.domain

data class PhotoLibraryPlan(
    val sourceFolder: String?,
    val destinationFolder: String?,
    val importMode: ImportMode,
) {
    val canScan: Boolean
        get() = !sourceFolder.isNullOrBlank() && !destinationFolder.isNullOrBlank()
}
