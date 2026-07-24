package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.presentation.FileRevealHandler

/**
 * Handles simple OS file actions and maps failures into app issues.
 *
 * The object is intentionally small: it keeps `App.kt` free from repeated
 * Finder/open error handling while more complex workflows remain in use cases.
 */
internal class PhotoLibraryFileActions(
    private val fileRevealHandler: FileRevealHandler,
    private val addIssue: (String, String) -> Unit,
) {
    fun openLibraryFolder(destinationFolder: String?) {
        openLibrarySubfolder(
            destinationFolder = destinationFolder,
            subfolder = "Library",
            title = uiText("Библиотека", "Library"),
        )
    }

    fun openUnsupportedFolder(destinationFolder: String?) {
        openLibrarySubfolder(
            destinationFolder = destinationFolder,
            subfolder = "Unsupported",
            title = uiText("Пропущенные", "Skipped"),
        )
    }

    fun openUnsupportedTypeFolder(
        destinationFolder: String?,
        type: String,
    ) {
        openLibrarySubfolder(
            destinationFolder = destinationFolder,
            subfolder = "Unsupported/${type.toUnsupportedFolderName()}",
            title = uiText("Неподдерживаемые", "Unsupported"),
        )
    }

    fun openDuplicatesFolder(destinationFolder: String?) {
        openLibrarySubfolder(
            destinationFolder = destinationFolder,
            subfolder = "Duplicates",
            title = uiText("Дубли", "Duplicates"),
        )
    }

    fun openSelectedFile(file: PlannedMediaFile?) {
        val path = file?.sourcePath ?: return
        if (!fileRevealHandler.open(path)) {
            addIssue(
                uiText("Просмотр", "Preview"),
                uiText("Не удалось открыть файл: $path", "Could not open file: $path"),
            )
        }
    }

    fun revealSelectedFile(file: PlannedMediaFile?) {
        val path = file?.sourcePath ?: return
        if (!fileRevealHandler.reveal(path)) {
            addIssue(
                uiText("Просмотр", "Preview"),
                uiText("Не удалось показать файл в папке: $path", "Could not reveal file in folder: $path"),
            )
        }
    }

    private fun openLibrarySubfolder(
        destinationFolder: String?,
        subfolder: String,
        title: String,
    ) {
        val path = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/$subfolder" }
        if (path == null) {
            addIssue(
                title,
                uiText(
                    ru = "Папка библиотеки не выбрана.",
                    en = "Library folder is not selected.",
                ),
            )
            return
        }

        if (!fileRevealHandler.open(path)) {
            addIssue(
                title,
                uiText(
                    ru = "Не удалось открыть папку: $path",
                    en = "Could not open folder: $path",
                ),
            )
        }
    }
}

/**
 * Merges unsupported-file quarantine statistics into the import result shown in
 * the UI. Media import stays the primary operation; unsupported files are an
 * additional cleanup step.
 */
internal fun ImportMediaFilesResult.withUnsupportedQuarantine(
    result: AppResult<UnsupportedFileQuarantineResult>?,
    fallbackFailedFiles: Int,
): ImportMediaFilesResult {
    return when (result) {
        null -> this
        is AppResult.Success -> copy(
            quarantinedUnsupportedFiles = result.data.movedFiles,
            failedUnsupportedFiles = result.data.failedFiles,
        )
        is AppResult.Error -> copy(failedUnsupportedFiles = failedUnsupportedFiles + fallbackFailedFiles)
    }
}

internal fun String.toUnsupportedFolderName(): String {
    return if (this == "без расширения" || this == "no extension") "no-extension" else this
}

internal fun List<PlannedMediaFile>.duplicateQuarantineCandidates(): List<PlannedMediaFile> {
    return asSequence()
        .filter { it.contentHash != null }
        .groupBy { it.contentHash }
        .values
        .filter { it.size > 1 }
        .flatMap { files -> files.sortedBy { it.targetRelativePath }.drop(1) }
}
