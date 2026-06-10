package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules

enum class ImportRulesPreset(
    val title: String,
    val description: String,
    val rules: ImportOrganizationRules,
) {
    YearMonth(
        title = "Год / месяц",
        description = "Удобно для большого архива без слишком глубоких папок.",
        rules = ImportOrganizationRules.Default,
    ),
    YearMonthDay(
        title = "Год / месяц / день",
        description = "Лучше для дней с большим количеством фото.",
        rules = ImportOrganizationRules(
            libraryFolderName = "Library",
            folderTemplate = "YYYY/YYYY-MM/YYYY-MM-DD",
            fileNameTemplate = "HH-mm-ss_original-name.ext",
        ),
    ),
    YearOnly(
        title = "Только год",
        description = "Минимум папок, сортировка в основном по имени файла.",
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
