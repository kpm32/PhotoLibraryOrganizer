package com.anvar.photolibraryorganizer.domain.model

/**
 * User-selectable templates for the physical folder and file layout.
 *
 * Supported tokens are expanded by [com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase].
 */
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
