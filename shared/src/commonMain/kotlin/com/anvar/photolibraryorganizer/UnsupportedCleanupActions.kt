package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.repository.UnsupportedFileQuarantineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates skipped-file quarantine cleanup operations.
 */
internal class UnsupportedCleanupActions(
    private val appState: PhotoLibraryAppState,
    private val unsupportedFileQuarantineRepository: UnsupportedFileQuarantineRepository,
    private val libraryActions: PhotoLibraryLibraryActions,
    private val coroutineScope: CoroutineScope,
) {
    fun requestDeleteUnsupported() {
        if (appState.unsupportedFiles.isEmpty()) {
            appState.unsupportedActionMessage = uiText(
                ru = "В папке пропущенных файлов пока нечего переносить в Корзину.",
                en = "There are no skipped files to move to Trash.",
            )
            appState.unsupportedDeleteAwaitingConfirmation = false
        } else {
            appState.unsupportedDeleteAwaitingConfirmation = true
            appState.unsupportedActionMessage = uiText(
                ru = "Будет перемещено в Корзину пропущенных файлов: ${appState.unsupportedFiles.size}. Библиотеку и исходники не трогаем.",
                en = "Will move skipped files to Trash: ${appState.unsupportedFiles.size}. The library and source files will not be touched.",
            )
        }
    }

    fun cancelDeleteUnsupported() {
        appState.unsupportedDeleteAwaitingConfirmation = false
        appState.unsupportedActionMessage = uiText("Перенос в Корзину отменен.", "Move to Trash canceled.")
    }

    fun confirmDeleteUnsupported() {
        if (appState.unsupportedActionInProgress) return

        coroutineScope.launch {
            appState.unsupportedActionInProgress = true
            try {
                if (appState.unsupportedFiles.isEmpty()) {
                    appState.unsupportedDeleteAwaitingConfirmation = false
                    appState.unsupportedActionMessage = uiText(
                        ru = "В папке пропущенных файлов пока нечего переносить в Корзину.",
                        en = "There are no skipped files to move to Trash.",
                    )
                    return@launch
                }

                appState.unsupportedActionMessage = uiText(
                    ru = "Перемещаю пропущенные файлы в Корзину...",
                    en = "Moving skipped files to Trash...",
                )
                appState.unsupportedActionMessage = try {
                    when (
                        val result = withContext(Dispatchers.Default) {
                            unsupportedFileQuarantineRepository.deleteFromQuarantine(
                                destinationFolder = appState.destinationFolder,
                                quarantineFiles = appState.unsupportedFiles,
                            )
                        }
                    ) {
                        is AppResult.Success -> {
                            appState.unsupportedDeleteAwaitingConfirmation = false
                            libraryActions.refreshLibraryIndexFromDisk(selectFirstFile = false)
                            reportPartialUnsupportedTrashFailure(result.data.failedFiles)
                            uiText(
                                ru = "Перемещено в Корзину пропущенных файлов: ${result.data.deletedFiles}, ошибок: ${result.data.failedFiles}.",
                                en = "Moved skipped files to Trash: ${result.data.deletedFiles}, errors: ${result.data.failedFiles}.",
                            )
                        }

                        is AppResult.Error -> {
                            appState.unsupportedDeleteAwaitingConfirmation = false
                            result.error.toUserMessage().also { message ->
                                appState.addIssue(uiText("Неподдерживаемые", "Unsupported"), message)
                            }
                        }
                    }
                } catch (exception: Throwable) {
                    appState.unsupportedDeleteAwaitingConfirmation = false
                    val message = uiText(
                        ru = "Не удалось переместить пропущенные файлы в Корзину: ${exception.message ?: "без деталей"}",
                        en = "Could not move skipped files to Trash: ${exception.message ?: "no details"}",
                    )
                    appState.addIssue(uiText("Неподдерживаемые", "Unsupported"), message)
                    message
                }
            } finally {
                appState.unsupportedActionInProgress = false
            }
        }
    }

    private fun reportPartialUnsupportedTrashFailure(failedFiles: Int) {
        if (failedFiles > 0) {
            appState.addIssue(
                title = uiText("Неподдерживаемые", "Unsupported"),
                detail = uiText(
                    ru = "Часть пропущенных файлов не удалось переместить в Корзину: $failedFiles.",
                    en = "Some skipped files could not be moved to Trash: $failedFiles.",
                ),
            )
        }
    }
}
