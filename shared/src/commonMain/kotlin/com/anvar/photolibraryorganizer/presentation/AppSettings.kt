package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules

data class AppSettings(
    val sourceFolder: String? = null,
    val destinationFolder: String? = null,
    val importRules: ImportOrganizationRules = ImportOrganizationRules.Default,
)
