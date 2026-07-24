package com.anvar.photolibraryorganizer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.repository.DuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.domain.repository.EmptyFolderCleanupRepository
import com.anvar.photolibraryorganizer.domain.repository.FileTrashRepository
import com.anvar.photolibraryorganizer.domain.repository.LibraryIndexStorage
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.ImportPlanTargetResolver
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.repository.UnsupportedFileQuarantineRepository
import com.anvar.photolibraryorganizer.domain.repository.StorageSpaceProvider
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.CachingImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.FileRevealHandler
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImportHistoryStorage
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.PreviewDuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.presentation.PreviewEmptyFolderCleanupRepository
import com.anvar.photolibraryorganizer.presentation.PreviewAppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.PreviewFileRevealHandler
import com.anvar.photolibraryorganizer.presentation.PreviewFileTrashRepository
import com.anvar.photolibraryorganizer.presentation.PreviewFolderPicker
import com.anvar.photolibraryorganizer.presentation.PreviewImportHistoryStorage
import com.anvar.photolibraryorganizer.presentation.PreviewImportPlanTargetResolver
import com.anvar.photolibraryorganizer.presentation.PreviewImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.PreviewLibraryIndexStorage
import com.anvar.photolibraryorganizer.presentation.PreviewMediaFileImporter
import com.anvar.photolibraryorganizer.presentation.PreviewPhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.PreviewUnsupportedFileQuarantineRepository
import com.anvar.photolibraryorganizer.presentation.PreviewStorageSpaceProvider
import com.anvar.photolibraryorganizer.presentation.ScanUiState

/**
 * Root Compose entry point that wires domain use cases, repositories, persisted
 * settings, and UI state together.
 *
 * The app currently keeps orchestration state here while domain rules live in
 * use cases and filesystem details live behind repository interfaces.
 */
@Composable
@Preview
fun App(
    photoSourceScanner: PhotoSourceScanner = PreviewPhotoSourceScanner,
    mediaFileImporter: MediaFileImporter = PreviewMediaFileImporter,
    importPlanTargetResolver: ImportPlanTargetResolver = PreviewImportPlanTargetResolver,
    storageSpaceProvider: StorageSpaceProvider = PreviewStorageSpaceProvider,
    duplicateQuarantineRepository: DuplicateQuarantineRepository = PreviewDuplicateQuarantineRepository,
    unsupportedFileQuarantineRepository: UnsupportedFileQuarantineRepository = PreviewUnsupportedFileQuarantineRepository,
    emptyFolderCleanupRepository: EmptyFolderCleanupRepository = PreviewEmptyFolderCleanupRepository,
    imagePreviewLoader: ImagePreviewLoader = PreviewImagePreviewLoader,
    appSettingsStorage: AppSettingsStorage = PreviewAppSettingsStorage,
    importHistoryStorage: ImportHistoryStorage = PreviewImportHistoryStorage,
    libraryIndexStorage: LibraryIndexStorage = PreviewLibraryIndexStorage,
    fileTrashRepository: FileTrashRepository = PreviewFileTrashRepository,
    folderPicker: FolderPicker = PreviewFolderPicker,
    fileRevealHandler: FileRevealHandler = PreviewFileRevealHandler,
) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        val appState = rememberPhotoLibraryAppState()
        val coroutineScope = rememberCoroutineScope()
        with(appState) {

        val useCases = rememberPhotoLibraryUseCases(
            photoSourceScanner = photoSourceScanner,
            mediaFileImporter = mediaFileImporter,
            storageSpaceProvider = storageSpaceProvider,
            fileTrashRepository = fileTrashRepository,
        )
        val cachedImagePreviewLoader = remember(imagePreviewLoader) {
            CachingImagePreviewLoader(imagePreviewLoader)
        }
        val fileActions = remember(fileRevealHandler) {
            PhotoLibraryFileActions(
                fileRevealHandler = fileRevealHandler,
                addIssue = ::addIssue,
            )
        }
        val libraryActions = remember(
            photoSourceScanner,
            libraryIndexStorage,
            appSettingsStorage,
            folderPicker,
            coroutineScope,
        ) {
            PhotoLibraryLibraryActions(
                appState = appState,
                photoSourceScanner = photoSourceScanner,
                libraryIndexStorage = libraryIndexStorage,
                appSettingsStorage = appSettingsStorage,
                folderPicker = folderPicker,
                coroutineScope = coroutineScope,
            )
        }
        val scanImportActions = remember(
            useCases,
            importPlanTargetResolver,
            unsupportedFileQuarantineRepository,
            importHistoryStorage,
            libraryActions,
            coroutineScope,
        ) {
            PhotoLibraryScanImportActions(
                appState = appState,
                useCases = useCases,
                importPlanTargetResolver = importPlanTargetResolver,
                unsupportedFileQuarantineRepository = unsupportedFileQuarantineRepository,
                importHistoryStorage = importHistoryStorage,
                libraryActions = libraryActions,
                coroutineScope = coroutineScope,
            )
        }
        val cleanupActions = remember(
            useCases,
            duplicateQuarantineRepository,
            unsupportedFileQuarantineRepository,
            emptyFolderCleanupRepository,
            libraryActions,
            coroutineScope,
        ) {
            PhotoLibraryCleanupActions(
                appState = appState,
                useCases = useCases,
                duplicateQuarantineRepository = duplicateQuarantineRepository,
                unsupportedFileQuarantineRepository = unsupportedFileQuarantineRepository,
                emptyFolderCleanupRepository = emptyFolderCleanupRepository,
                libraryActions = libraryActions,
                coroutineScope = coroutineScope,
            )
        }

        RestorePersistedAppStateEffect(
            appState = appState,
            appSettingsStorage = appSettingsStorage,
            importHistoryStorage = importHistoryStorage,
            loadLibraryIndexIfAvailable = libraryActions::loadLibraryIndexIfAvailable,
        )
        SelectedFilePreviewEffect(
            appState = appState,
            imagePreviewLoader = cachedImagePreviewLoader,
        )

        val plan = PhotoLibraryPlan(
            sourceFolder = sourceFolder,
            destinationFolder = destinationFolder,
            importMode = importMode,
            importRules = importRules,
        )
        val navigationFiles = navigationFilesForSection(
            selectedSection = selectedSection,
            scanUiState = scanUiState,
            libraryFiles = libraryFiles,
            duplicateFiles = duplicateFiles,
            unsupportedFiles = unsupportedFiles,
        )
        val selectedFileIndex = selectedFile?.let { selected ->
            navigationFiles.indexOfFirst { it.sourcePath == selected.sourcePath }
        } ?: -1
        val canNavigateSelectedFile = navigationFiles.size > 1 && selectedFileIndex >= 0

        PhotoLibraryOrganizerApp(
            plan = plan,
            scanUiState = scanUiState,
            importUiState = importUiState,
            lastImportReport = lastImportReport,
            emptyFolderCleanupMessage = emptyFolderCleanupMessage,
            emptyFolderCleanupAwaitingConfirmation = emptyFolderCleanupAwaitingConfirmation,
            importHistory = importHistory,
            libraryFiles = libraryFiles,
            duplicateFiles = duplicateFiles,
            unsupportedFiles = unsupportedFiles,
            selectedSection = selectedSection,
            imagePreviewLoader = cachedImagePreviewLoader,
            duplicateActionMessage = duplicateActionMessage,
            duplicateDeleteAwaitingConfirmation = duplicateDeleteAwaitingConfirmation,
            duplicateActionInProgress = duplicateActionInProgress,
            unsupportedActionMessage = unsupportedActionMessage,
            unsupportedDeleteAwaitingConfirmation = unsupportedDeleteAwaitingConfirmation,
            unsupportedActionInProgress = unsupportedActionInProgress,
            selectedFileTrashAwaitingConfirmation = selectedFileTrashAwaitingConfirmation,
            selectedFileTrashMessage = selectedFileTrashMessage,
            selectedFileTrashInProgress = selectedFileTrashInProgress,
            importAvailability = useCases.resolveImportAvailability(
                importMode = importMode,
                plannedFiles = (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty(),
            ),
            isLibraryRefreshing = isLibraryRefreshing,
            libraryRefreshProgress = libraryRefreshProgress,
            issues = issues,
            onSourceFolderClick = {
                libraryActions.chooseSourceFolder()
            },
            onDestinationFolderClick = {
                libraryActions.chooseDestinationFolder()
            },
            onImportModeSelected = {
                scanImportActions.selectImportMode(it)
            },
            onImportRulesSelected = { selectedImportRules ->
                importRules = selectedImportRules
                scanUiState = ScanUiState.Idle
                importUiState = ImportUiState.Idle
                lastImportReport = null
                libraryActions.saveCurrentSettings()
            },
            onScanClick = {
                scanImportActions.startScan(plan)
            },
            onCancelScanClick = {
                scanImportActions.cancelScan()
            },
            onImportClick = {
                scanImportActions.prepareImport()
            },
            onCancelImportClick = {
                scanImportActions.cancelImportConfirmation()
            },
            onConfirmImportClick = {
                scanImportActions.confirmImport()
            },
            onCancelRunningImportClick = {
                scanImportActions.cancelRunningImport()
            },
            onRefreshLibraryClick = {
                libraryActions.startLibraryRefresh()
            },
            onCancelRefreshLibraryClick = {
                libraryActions.cancelLibraryRefresh()
            },
            onMoveDuplicatesClick = {
                cleanupActions.moveDuplicatesToQuarantine()
            },
            onRequestDeleteQuarantineClick = {
                cleanupActions.requestDeleteDuplicateQuarantine()
            },
            onCancelDeleteQuarantineClick = {
                cleanupActions.cancelDeleteDuplicateQuarantine()
            },
            onConfirmDeleteQuarantineClick = {
                cleanupActions.confirmDeleteDuplicateQuarantine()
            },
            onRequestDeleteUnsupportedClick = {
                cleanupActions.requestDeleteUnsupported()
            },
            onCancelDeleteUnsupportedClick = {
                cleanupActions.cancelDeleteUnsupported()
            },
            onConfirmDeleteUnsupportedClick = {
                cleanupActions.confirmDeleteUnsupported()
            },
            onRequestEmptyFolderCleanupClick = {
                cleanupActions.requestEmptyFolderCleanup()
            },
            onCancelEmptyFolderCleanupClick = {
                cleanupActions.cancelEmptyFolderCleanup()
            },
            onConfirmEmptyFolderCleanupClick = {
                cleanupActions.confirmEmptyFolderCleanup()
            },
            onOpenLibraryFolderClick = {
                fileActions.openLibraryFolder(destinationFolder)
            },
            onOpenUnsupportedFolderClick = {
                fileActions.openUnsupportedFolder(destinationFolder)
            },
            onOpenUnsupportedTypeFolderClick = { type ->
                fileActions.openUnsupportedTypeFolder(destinationFolder, type)
            },
            onOpenDuplicatesFolderClick = {
                fileActions.openDuplicatesFolder(destinationFolder)
            },
            onClearIssuesClick = { clearIssues() },
            onSectionSelected = { selectedSection = it },
            selectedFile = selectedFile,
            selectedFileIndex = selectedFileIndex,
            navigationFileCount = navigationFiles.size,
            imagePreviewUiState = imagePreviewUiState,
            onOpenFileClick = {
                fileActions.openSelectedFile(selectedFile)
            },
            onRevealFileClick = {
                fileActions.revealSelectedFile(selectedFile)
            },
            onRequestMoveSelectedFileToTrashClick = {
                cleanupActions.requestMoveSelectedFileToTrash(navigationFiles)
            },
            onCancelMoveSelectedFileToTrashClick = {
                cleanupActions.cancelMoveSelectedFileToTrash()
            },
            onPreviousFileClick = {
                if (canNavigateSelectedFile) {
                    selectedFile = navigationFiles.nextFrom(selectedFileIndex, step = -1)
                }
            },
            onNextFileClick = {
                if (canNavigateSelectedFile) {
                    selectedFile = navigationFiles.nextFrom(selectedFileIndex, step = 1)
                }
            },
            onFileSelected = { selectedFile = it },
        )
        }
    }
}
