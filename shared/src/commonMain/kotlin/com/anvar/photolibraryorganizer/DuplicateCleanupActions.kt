package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.repository.DuplicateQuarantineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates exact-duplicate quarantine and Trash operations.
 */
internal class DuplicateCleanupActions(
    private val appState: PhotoLibraryAppState,
    private val duplicateQuarantineRepository: DuplicateQuarantineRepository,
    private val libraryActions: PhotoLibraryLibraryActions,
    private val coroutineScope: CoroutineScope,
) {
    fun moveDuplicatesToQuarantine() {
        if (appState.duplicateActionInProgress) return

        coroutineScope.launch {
            appState.duplicateActionInProgress = true
            appState.duplicateDeleteAwaitingConfirmation = false
            try {
                val duplicatesToMove = appState.libraryFiles.duplicateQuarantineCandidates()
                if (duplicatesToMove.isEmpty()) {
                    appState.duplicateActionMessage = uiText("Дубликаты для переноса не найдены.", "No duplicates found to move.")
                    return@launch
                }

                appState.duplicateActionMessage = uiText("Переношу дубликаты в папку дублей...", "Moving duplicates to the duplicates folder...")
                appState.duplicateActionMessage = try {
                    when (
                        val result = withContext(Dispatchers.Default) {
                            duplicateQuarantineRepository.moveToQuarantine(
                                destinationFolder = appState.destinationFolder,
                                duplicateFiles = duplicatesToMove,
                            )
                        }
                    ) {
                        is AppResult.Success -> {
                            libraryActions.refreshLibraryIndexFromDisk()
                            reportPartialDuplicateMoveFailure(result.data.failedFiles)
                            uiText(
                                ru = "Перенесено в папку дублей: ${result.data.movedFiles}, ошибок: ${result.data.failedFiles}.",
                                en = "Moved to duplicates folder: ${result.data.movedFiles}, errors: ${result.data.failedFiles}.",
                            )
                        }

                        is AppResult.Error -> result.error.toUserMessage().also { message ->
                            appState.addIssue(uiText("Дубликаты", "Duplicates"), message)
                        }
                    }
                } catch (exception: Throwable) {
                    val message = uiText(
                        ru = "Не удалось перенести дубликаты: ${exception.message ?: "без деталей"}",
                        en = "Could not move duplicates: ${exception.message ?: "no details"}",
                    )
                    appState.addIssue(uiText("Дубликаты", "Duplicates"), message)
                    message
                }
            } finally {
                appState.duplicateActionInProgress = false
            }
        }
    }

    fun requestDeleteQuarantine() {
        if (appState.duplicateFiles.isEmpty()) {
            appState.duplicateActionMessage = uiText(
                ru = "В папке дублей пока нет файлов для переноса в Корзину.",
                en = "There are no files in the duplicates folder to move to Trash.",
            )
            appState.duplicateDeleteAwaitingConfirmation = false
        } else {
            appState.duplicateDeleteAwaitingConfirmation = true
            appState.duplicateActionMessage = uiText(
                ru = "Будет перемещено в Корзину из папки дублей: ${appState.duplicateFiles.size}. Библиотеку не трогаем.",
                en = "Will move files from the duplicates folder to Trash: ${appState.duplicateFiles.size}. The library will not be touched.",
            )
        }
    }

    fun cancelDeleteQuarantine() {
        appState.duplicateDeleteAwaitingConfirmation = false
        appState.duplicateActionMessage = uiText("Перенос в Корзину отменен.", "Move to Trash canceled.")
    }

    fun confirmDeleteQuarantine() {
        if (appState.duplicateActionInProgress) return

        coroutineScope.launch {
            appState.duplicateActionInProgress = true
            try {
                if (appState.duplicateFiles.isEmpty()) {
                    appState.duplicateDeleteAwaitingConfirmation = false
                    appState.duplicateActionMessage = uiText(
                        ru = "В папке дублей пока нет файлов для переноса в Корзину.",
                        en = "There are no files in the duplicates folder to move to Trash.",
                    )
                    return@launch
                }

                appState.duplicateActionMessage = uiText(
                    ru = "Перемещаю файлы из папки дублей в Корзину...",
                    en = "Moving files from the duplicates folder to Trash...",
                )
                appState.duplicateActionMessage = try {
                    when (
                        val result = withContext(Dispatchers.Default) {
                            duplicateQuarantineRepository.deleteFromQuarantine(
                                destinationFolder = appState.destinationFolder,
                                quarantineFiles = appState.duplicateFiles,
                            )
                        }
                    ) {
                        is AppResult.Success -> {
                            appState.duplicateDeleteAwaitingConfirmation = false
                            libraryActions.refreshLibraryIndexFromDisk()
                            reportPartialDuplicateTrashFailure(result.data.failedFiles)
                            uiText(
                                ru = "Перемещено в Корзину из папки дублей: ${result.data.deletedFiles}, ошибок: ${result.data.failedFiles}.",
                                en = "Moved from duplicates folder to Trash: ${result.data.deletedFiles}, errors: ${result.data.failedFiles}.",
                            )
                        }

                        is AppResult.Error -> {
                            appState.duplicateDeleteAwaitingConfirmation = false
                            result.error.toUserMessage().also { message ->
                                appState.addIssue(uiText("Дубликаты", "Duplicates"), message)
                            }
                        }
                    }
                } catch (exception: Throwable) {
                    appState.duplicateDeleteAwaitingConfirmation = false
                    val message = uiText(
                        ru = "Не удалось переместить файлы из папки дублей в Корзину: ${exception.message ?: "без деталей"}",
                        en = "Could not move files from the duplicates folder to Trash: ${exception.message ?: "no details"}",
                    )
                    appState.addIssue(uiText("Дубликаты", "Duplicates"), message)
                    message
                }
            } finally {
                appState.duplicateActionInProgress = false
            }
        }
    }

    private fun reportPartialDuplicateMoveFailure(failedFiles: Int) {
        if (failedFiles > 0) {
            appState.addIssue(
                title = uiText("Дубликаты", "Duplicates"),
                detail = uiText(
                    ru = "Часть дублей не удалось перенести в папку дублей: $failedFiles.",
                    en = "Some duplicates could not be moved to the duplicates folder: $failedFiles.",
                ),
            )
        }
    }

    private fun reportPartialDuplicateTrashFailure(failedFiles: Int) {
        if (failedFiles > 0) {
            appState.addIssue(
                title = uiText("Дубликаты", "Duplicates"),
                detail = uiText(
                    ru = "Часть файлов из папки дублей не удалось переместить в Корзину: $failedFiles.",
                    en = "Some files from the duplicates folder could not be moved to Trash: $failedFiles.",
                ),
            )
        }
    }
}
