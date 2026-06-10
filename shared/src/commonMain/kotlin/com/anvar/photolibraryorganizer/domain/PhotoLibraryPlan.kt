package com.anvar.photolibraryorganizer.domain

import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules

data class PhotoLibraryPlan(
    val sourceFolder: String?,
    val destinationFolder: String?,
    val importMode: ImportMode,
    val importRules: ImportOrganizationRules = ImportOrganizationRules.Default,
) {
    val canScan: Boolean
        get() = !sourceFolder.isNullOrBlank() && !destinationFolder.isNullOrBlank()
}
