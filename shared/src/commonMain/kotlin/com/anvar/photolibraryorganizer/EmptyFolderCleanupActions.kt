package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.repository.EmptyFolderCleanupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates safe cleanup of empty source subfolders after move imports.
 */
internal class EmptyFolderCleanupActions(
    private val appState: PhotoLibraryAppState,
    private val emptyFolderCleanupRepository: EmptyFolderCleanupRepository,
    private val coroutineScope: CoroutineScope,
) {
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
}
