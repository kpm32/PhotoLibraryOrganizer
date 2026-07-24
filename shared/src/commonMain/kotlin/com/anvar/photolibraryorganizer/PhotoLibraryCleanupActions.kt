package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.MoveSelectedFileToTrashResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.DuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.domain.repository.EmptyFolderCleanupRepository
import com.anvar.photolibraryorganizer.domain.repository.UnsupportedFileQuarantineRepository
import com.anvar.photolibraryorganizer.presentation.AppSection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates duplicate, skipped-file, empty-folder, and selected-file cleanup
 * actions from the app shell.
 */
internal class PhotoLibraryCleanupActions(
    private val appState: PhotoLibraryAppState,
    private val useCases: PhotoLibraryUseCases,
    private val duplicateQuarantineRepository: DuplicateQuarantineRepository,
    private val unsupportedFileQuarantineRepository: UnsupportedFileQuarantineRepository,
    private val emptyFolderCleanupRepository: EmptyFolderCleanupRepository,
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

    fun requestDeleteDuplicateQuarantine() {
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

    fun cancelDeleteDuplicateQuarantine() {
        appState.duplicateDeleteAwaitingConfirmation = false
        appState.duplicateActionMessage = uiText("Перенос в Корзину отменен.", "Move to Trash canceled.")
    }

    fun confirmDeleteDuplicateQuarantine() {
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

    fun requestEmptyFolderCleanup() {
        appState.emptyFolderCleanupAwaitingConfirmation = true
        appState.emptyFolderCleanupMessage = uiText(
            ru = "Будут удалены только пустые подпапки внутри исходной папки. Файлы не удаляются.",
            en = "Only empty subfolders inside the source folder will be removed. Files are not deleted.",
        )
    }

    fun cancelEmptyFolderCleanup() {
        appState.emptyFolderCleanupAwaitingConfirmation = false
        appState.emptyFolderCleanupMessage = uiText("Очистка пустых папок отменена.", "Empty folder cleanup canceled.")
    }

    fun confirmEmptyFolderCleanup() {
        coroutineScope.launch {
            appState.emptyFolderCleanupMessage = uiText("Удаляю пустые папки источника...", "Removing empty source folders...")
            appState.emptyFolderCleanupMessage = try {
                when (
                    val result = withContext(Dispatchers.Default) {
                        emptyFolderCleanupRepository.deleteEmptyFolders(appState.sourceFolder)
                    }
                ) {
                    is AppResult.Success -> {
                        appState.emptyFolderCleanupAwaitingConfirmation = false
                        if (result.data.failedFolders > 0) {
                            appState.addIssue(
                                title = uiText("Пустые папки", "Empty Folders"),
                                detail = uiText(
                                    ru = "Часть пустых папок не удалось удалить: ${result.data.failedFolders}.",
                                    en = "Some empty folders could not be removed: ${result.data.failedFolders}.",
                                ),
                            )
                        }
                        uiText(
                            ru = "Удалено пустых папок: ${result.data.deletedFolders}, ошибок: ${result.data.failedFolders}.",
                            en = "Removed empty folders: ${result.data.deletedFolders}, errors: ${result.data.failedFolders}.",
                        )
                    }

                    is AppResult.Error -> {
                        appState.emptyFolderCleanupAwaitingConfirmation = false
                        result.error.toUserMessage().also { message ->
                            appState.addIssue(uiText("Пустые папки", "Empty Folders"), message)
                        }
                    }
                }
            } catch (exception: Throwable) {
                appState.emptyFolderCleanupAwaitingConfirmation = false
                val message = uiText(
                    ru = "Не удалось удалить пустые папки: ${exception.message ?: "без деталей"}",
                    en = "Could not remove empty folders: ${exception.message ?: "no details"}",
                )
                appState.addIssue(uiText("Пустые папки", "Empty Folders"), message)
                message
            }
        }
    }

    fun requestMoveSelectedFileToTrash(
        navigationFiles: List<PlannedMediaFile>,
    ) {
        if (appState.selectedFileTrashInProgress) return

        val file = appState.selectedFile
        if (file == null) {
            appState.selectedFileTrashAwaitingConfirmation = false
            appState.selectedFileTrashMessage = uiText("Файл не выбран.", "No file selected.")
        } else if (appState.selectedSection == AppSection.Import) {
            appState.selectedFileTrashAwaitingConfirmation = false
            appState.selectedFileTrashMessage = sourceTrashBlockedMessage()
        } else if (appState.selectedFileTrashAwaitingConfirmation) {
            moveSelectedFileToTrash(file, navigationFiles)
        } else {
            appState.selectedFileTrashAwaitingConfirmation = true
            appState.selectedFileTrashMessage = uiText(
                ru = "Будет перемещен в Корзину только выбранный файл: ${file.fileName}",
                en = "Only the selected file will be moved to Trash: ${file.fileName}",
            )
        }
    }

    fun cancelMoveSelectedFileToTrash() {
        appState.selectedFileTrashAwaitingConfirmation = false
        appState.selectedFileTrashMessage = uiText("Перенос выбранного файла отменен.", "Moving selected file canceled.")
    }

    private fun moveSelectedFileToTrash(
        file: PlannedMediaFile,
        navigationFiles: List<PlannedMediaFile>,
    ) {
        coroutineScope.launch {
            appState.selectedFileTrashInProgress = true
            val path = file.sourcePath
            appState.selectedFileTrashMessage = uiText("Перемещаю выбранный файл в Корзину...", "Moving selected file to Trash...")
            appState.selectedFileTrashMessage = try {
                val result = withContext(Dispatchers.Default) {
                    useCases.moveSelectedFileToTrash(
                        selectedFile = file,
                        isSourceFileActionAllowed = appState.selectedSection != AppSection.Import,
                    )
                }
                appState.selectedFileTrashAwaitingConfirmation = false
                result.toSelectedFileTrashMessage(path, navigationFiles)
            } catch (exception: Throwable) {
                appState.selectedFileTrashAwaitingConfirmation = false
                val message = uiText(
                    ru = "Не удалось переместить файл в Корзину: ${exception.message ?: "без деталей"}",
                    en = "Could not move file to Trash: ${exception.message ?: "no details"}",
                )
                appState.addIssue(uiText("Корзина", "Trash"), message)
                message
            } finally {
                appState.selectedFileTrashInProgress = false
            }
        }
    }

    private fun MoveSelectedFileToTrashResult.toSelectedFileTrashMessage(
        path: String,
        navigationFiles: List<PlannedMediaFile>,
    ): String {
        return when (this) {
            is MoveSelectedFileToTrashResult.Moved -> {
                appState.removeFileFromVisibleState(
                    path = this.path,
                    preferredSelectionFiles = navigationFiles,
                )
                coroutineScope.launch {
                    try {
                        libraryActions.refreshLibraryIndexFromDisk(selectFirstFile = false)
                    } catch (exception: Throwable) {
                        // The file is already in Trash. A refresh failure should not turn
                        // a successful delete into a blocking system dialog.
                    }
                }
                uiText("Файл перемещен в Корзину.", "File moved to Trash.")
            }

            MoveSelectedFileToTrashResult.FileNotSelected -> uiText("Файл не выбран.", "No file selected.")
            MoveSelectedFileToTrashResult.SourceFileActionNotAllowed -> sourceTrashBlockedMessage()
            MoveSelectedFileToTrashResult.TimedOut -> {
                val message = uiText(
                    ru = "Перенос в Корзину занял слишком много времени. Проверь доступ к диску или попробуй открыть файл в папке.",
                    en = "Moving to Trash took too long. Check disk access or try opening the file in its folder.",
                )
                appState.addIssue(
                    uiText("Корзина", "Trash"),
                    uiText("$message Файл: $path", "$message File: $path"),
                )
                message
            }

            MoveSelectedFileToTrashResult.Failed -> {
                appState.addIssue(
                    uiText("Корзина", "Trash"),
                    uiText("Не удалось переместить файл в Корзину: $path", "Could not move file to Trash: $path"),
                )
                uiText("Не удалось переместить файл в Корзину.", "Could not move file to Trash.")
            }

            is MoveSelectedFileToTrashResult.Error -> {
                val detail = this.message
                val message = uiText(
                    ru = "Не удалось переместить файл в Корзину: ${detail ?: "без деталей"}",
                    en = "Could not move file to Trash: ${detail ?: "no details"}",
                )
                appState.addIssue(uiText("Корзина", "Trash"), message)
                message
            }
        }
    }

    private fun sourceTrashBlockedMessage(): String {
        return uiText(
            ru = "В разделе импорта файл из исходной папки не переносится в Корзину. Сначала проверь план импорта.",
            en = "In the import section, source files are not moved to Trash. Review the import plan first.",
        )
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
