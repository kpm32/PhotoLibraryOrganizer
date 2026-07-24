package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules

enum class ImportRulesPreset(
    val rules: ImportOrganizationRules,
) {
    YearMonth(
        rules = ImportOrganizationRules.Default,
    ),
    YearMonthDay(
        rules = ImportOrganizationRules(
            libraryFolderName = "Library",
            folderTemplate = "YYYY/YYYY-MM/YYYY-MM-DD",
            fileNameTemplate = "HH-mm-ss_original-name.ext",
        ),
    ),
    YearOnly(
        rules = ImportOrganizationRules(
            libraryFolderName = "Library",
            folderTemplate = "YYYY",
            fileNameTemplate = "YYYY-MM-DD_HH-mm-ss_original-name.ext",
        ),
    );

    companion object {
        fun fromRules(rules: ImportOrganizationRules): ImportRulesPreset {
            return entries.firstOrNull { it.rules == rules } ?: YearMonth
        }
    }
}
