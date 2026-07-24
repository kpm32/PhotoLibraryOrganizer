package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.model.MoveSelectedFileToTrashResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.AppSection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates the inspector action that moves one selected library file to
 * macOS Trash after explicit confirmation.
 */
internal class SelectedFileTrashActions(
    private val appState: PhotoLibraryAppState,
    private val useCases: PhotoLibraryUseCases,
    private val libraryActions: PhotoLibraryLibraryActions,
    private val coroutineScope: CoroutineScope,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
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
                val result = withContext(backgroundDispatcher) {
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
}
