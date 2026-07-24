package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.DuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.domain.repository.EmptyFolderCleanupRepository
import com.anvar.photolibraryorganizer.domain.repository.UnsupportedFileQuarantineRepository
import kotlinx.coroutines.CoroutineScope

/**
 * Stable cleanup facade for the app composition root.
 *
 * Each cleanup workflow is delegated to a focused action class so the root app
 * can keep one dependency while implementation details stay split by feature.
 */
internal class PhotoLibraryCleanupActions(
    appState: PhotoLibraryAppState,
    useCases: PhotoLibraryUseCases,
    duplicateQuarantineRepository: DuplicateQuarantineRepository,
    unsupportedFileQuarantineRepository: UnsupportedFileQuarantineRepository,
    emptyFolderCleanupRepository: EmptyFolderCleanupRepository,
    libraryActions: PhotoLibraryLibraryActions,
    coroutineScope: CoroutineScope,
) {
    private val duplicateActions = DuplicateCleanupActions(
        appState = appState,
        duplicateQuarantineRepository = duplicateQuarantineRepository,
        libraryActions = libraryActions,
        coroutineScope = coroutineScope,
    )
    private val unsupportedActions = UnsupportedCleanupActions(
        appState = appState,
        unsupportedFileQuarantineRepository = unsupportedFileQuarantineRepository,
        libraryActions = libraryActions,
        coroutineScope = coroutineScope,
    )
    private val emptyFolderActions = EmptyFolderCleanupActions(
        appState = appState,
        emptyFolderCleanupRepository = emptyFolderCleanupRepository,
        coroutineScope = coroutineScope,
    )
    private val selectedFileTrashActions = SelectedFileTrashActions(
        appState = appState,
        useCases = useCases,
        libraryActions = libraryActions,
        coroutineScope = coroutineScope,
    )

    fun moveDuplicatesToQuarantine() {
        duplicateActions.moveDuplicatesToQuarantine()
    }

    fun requestDeleteDuplicateQuarantine() {
        duplicateActions.requestDeleteQuarantine()
    }

    fun cancelDeleteDuplicateQuarantine() {
        duplicateActions.cancelDeleteQuarantine()
    }

    fun confirmDeleteDuplicateQuarantine() {
        duplicateActions.confirmDeleteQuarantine()
    }

    fun requestDeleteUnsupported() {
        unsupportedActions.requestDeleteUnsupported()
    }

    fun cancelDeleteUnsupported() {
        unsupportedActions.cancelDeleteUnsupported()
    }

    fun confirmDeleteUnsupported() {
        unsupportedActions.confirmDeleteUnsupported()
    }

    fun requestEmptyFolderCleanup() {
        emptyFolderActions.requestEmptyFolderCleanup()
    }

    fun cancelEmptyFolderCleanup() {
        emptyFolderActions.cancelEmptyFolderCleanup()
    }

    fun confirmEmptyFolderCleanup() {
        emptyFolderActions.confirmEmptyFolderCleanup()
    }

    fun requestMoveSelectedFileToTrash(
        navigationFiles: List<PlannedMediaFile>,
    ) {
        selectedFileTrashActions.requestMoveSelectedFileToTrash(navigationFiles)
    }

    fun cancelMoveSelectedFileToTrash() {
        selectedFileTrashActions.cancelMoveSelectedFileToTrash()
    }
}
