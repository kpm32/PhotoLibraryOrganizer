package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.PhotoLibraryError

/**
 * Converts domain failures into short user-facing messages.
 *
 * This is intentionally kept outside domain: business errors stay language-free,
 * while the UI decides how to explain them to a person.
 */
internal fun PhotoLibraryError.toUserMessage(): String {
    return when (this) {
        PhotoLibraryError.InvalidSourceFolder -> uiText(
            ru = "Исходная папка не выбрана.",
            en = "Source folder is not selected.",
        )
        is PhotoLibraryError.SourceFolderNotFound -> uiText(
            ru = "Исходная папка не найдена: $path",
            en = "Source folder was not found: $path",
        )
        is PhotoLibraryError.SourceFolderIsNotDirectory -> uiText(
            ru = "Выбранный путь не является папкой: $path",
            en = "Selected path is not a folder: $path",
        )
        PhotoLibraryError.ImportPlanIsEmpty -> uiText(
            ru = "Нет плана импорта. Сначала выполни сканирование.",
            en = "There is no import plan. Scan the folder first.",
        )
        PhotoLibraryError.UnsupportedImportMode -> uiText(
            ru = "Этот режим импорта пока не поддерживается.",
            en = "This import mode is not supported yet.",
        )
        is PhotoLibraryError.FileSystem -> uiText(
            ru = "Не удалось выполнить файловую операцию: $message",
            en = "Could not complete the file operation: $message",
        )
        is PhotoLibraryError.Unknown -> uiText(
            ru = "Неизвестная ошибка: ${message ?: "без деталей"}",
            en = "Unknown error: ${message ?: "no details"}",
        )
    }
}
