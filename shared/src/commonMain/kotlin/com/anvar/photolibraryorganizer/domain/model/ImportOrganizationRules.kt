package com.anvar.photolibraryorganizer.domain.model

data class ImportOrganizationRules(
    val libraryFolderName: String,
    val folderTemplate: String,
    val fileNameTemplate: String,
) {
    companion object {
        val Default = ImportOrganizationRules(
            libraryFolderName = "Library",
            folderTemplate = "YYYY/YYYY-MM",
            fileNameTemplate = "YYYY-MM-DD_HH-mm-ss_original-name.ext",
        )
    }
}
