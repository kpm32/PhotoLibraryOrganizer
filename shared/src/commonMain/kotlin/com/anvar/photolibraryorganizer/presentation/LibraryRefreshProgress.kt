package com.anvar.photolibraryorganizer.presentation

data class LibraryRefreshProgress(
    val section: LibraryRefreshSection,
    val scannedFiles: Int = 0,
    val mediaFiles: Int = 0,
    val unsupportedFiles: Int = 0,
)

enum class LibraryRefreshSection(
    val title: String,
) {
    Library("Библиотека"),
    Duplicates("Дубли"),
    Unsupported("Пропущенные"),
    Saving("Сохранение индекса"),
}
