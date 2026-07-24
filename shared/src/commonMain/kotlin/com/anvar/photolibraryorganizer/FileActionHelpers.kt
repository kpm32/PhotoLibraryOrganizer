package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.presentation.FileRevealHandler

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

internal fun openLibrarySubfolder(
    destinationFolder: String?,
    subfolder: String,
    title: String,
    fileRevealHandler: FileRevealHandler,
    addIssue: (String, String) -> Unit,
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
