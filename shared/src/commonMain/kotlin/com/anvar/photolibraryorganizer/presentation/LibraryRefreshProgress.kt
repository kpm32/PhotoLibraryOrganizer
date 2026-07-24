package com.anvar.photolibraryorganizer.presentation

data class LibraryRefreshProgress(
    val section: LibraryRefreshSection,
    val scannedFiles: Int = 0,
    val mediaFiles: Int = 0,
    val unsupportedFiles: Int = 0,
)

enum class LibraryRefreshSection(
    val ruTitle: String,
    val enTitle: String,
) {
    Library("Библиотека", "Library"),
    Duplicates("Дубли", "Duplicates"),
    Unsupported("Пропущенные", "Skipped"),
    Saving("Сохранение индекса", "Saving index"),
}
