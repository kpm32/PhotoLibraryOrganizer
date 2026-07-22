package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.model.MoveSelectedFileToTrashResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.FileTrashRepository
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Moves the currently selected library file to the OS Trash.
 *
 * UI code decides whether the current screen is allowed to perform this action,
 * while this use case centralizes the selected-file validation, timeout, and
 * repository result mapping.
 */
class MoveSelectedFileToTrashUseCase(
    private val fileTrashRepository: FileTrashRepository,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) {
    suspend operator fun invoke(
        selectedFile: PlannedMediaFile?,
        isSourceFileActionAllowed: Boolean,
    ): MoveSelectedFileToTrashResult {
        val file = selectedFile ?: return MoveSelectedFileToTrashResult.FileNotSelected
        if (!isSourceFileActionAllowed) return MoveSelectedFileToTrashResult.SourceFileActionNotAllowed

        return try {
            val moved = withTimeoutOrNull(timeoutMillis) {
                fileTrashRepository.moveToTrash(file.sourcePath)
            }
            when (moved) {
                true -> MoveSelectedFileToTrashResult.Moved(file.sourcePath)
                false -> MoveSelectedFileToTrashResult.Failed
                null -> MoveSelectedFileToTrashResult.TimedOut
            }
        } catch (exception: Throwable) {
            MoveSelectedFileToTrashResult.Error(exception.message)
        }
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 20_000L
    }
}
