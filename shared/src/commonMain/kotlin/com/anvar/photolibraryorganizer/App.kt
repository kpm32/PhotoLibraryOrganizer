package com.anvar.photolibraryorganizer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportStorageSpace
import com.anvar.photolibraryorganizer.domain.model.MoveSelectedFileToTrashResult
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
import com.anvar.photolibraryorganizer.presentation.AppSettings
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.CachingImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.FileRevealHandler
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImportHistoryStorage
import com.anvar.photolibraryorganizer.presentation.ImportReport
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.LibraryRefreshProgress
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

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

        suspend fun loadLibraryIndexIfAvailable(
            destination: String?,
            selectFirstFile: Boolean = true,
        ): Boolean {
            val snapshot = libraryIndexStorage.load(destination) ?: return false
            applyLibraryIndexSnapshot(snapshot, selectFirstFile)
            return true
        }

        suspend fun refreshLibraryIndexFromDisk(
            selectFirstFile: Boolean = true,
            onProgress: (LibraryRefreshProgress) -> Unit = {},
        ) {
            val snapshot = refreshLibraryIndexSnapshot(
                destinationFolder = destinationFolder,
                photoSourceScanner = photoSourceScanner,
                libraryIndexStorage = libraryIndexStorage,
                onProgress = onProgress,
            )
            applyLibraryIndexSnapshot(snapshot, selectFirstFile)
        }

        RestorePersistedAppStateEffect(
            appState = appState,
            appSettingsStorage = appSettingsStorage,
            importHistoryStorage = importHistoryStorage,
            loadLibraryIndexIfAvailable = ::loadLibraryIndexIfAvailable,
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
                folderPicker.chooseFolder(uiText("Выбери исходную папку", "Choose Source Folder"))?.let {
                    sourceFolder = it
                    coroutineScope.launch {
                        appSettingsStorage.saveSettings(
                            AppSettings(
                                sourceFolder = sourceFolder,
                                destinationFolder = destinationFolder,
                                importRules = importRules,
                            ),
                        )
                    }
                }
                resetAfterFolderSelection(clearLibraryFiles = true)
            },
            onDestinationFolderClick = {
                folderPicker.chooseFolder(uiText("Выбери папку библиотеки", "Choose Library Folder"))?.let {
                    destinationFolder = it
                    coroutineScope.launch {
                        appSettingsStorage.saveSettings(
                            AppSettings(
                                sourceFolder = sourceFolder,
                                destinationFolder = destinationFolder,
                                importRules = importRules,
                            ),
                        )
                    }
                }
                resetAfterFolderSelection(clearLibraryFiles = false)
                coroutineScope.launch {
                    if (!loadLibraryIndexIfAvailable(destinationFolder)) {
                        libraryFiles = emptyList()
                        duplicateFiles = emptyList()
                        unsupportedFiles = emptyList()
                    }
                }
            },
            onImportModeSelected = {
                importMode = it
                importUiState = ImportUiState.Idle
            },
            onImportRulesSelected = { selectedImportRules ->
                importRules = selectedImportRules
                scanUiState = ScanUiState.Idle
                importUiState = ImportUiState.Idle
                lastImportReport = null
                coroutineScope.launch {
                    appSettingsStorage.saveSettings(
                        AppSettings(
                            sourceFolder = sourceFolder,
                            destinationFolder = destinationFolder,
                            importRules = selectedImportRules,
                        ),
                    )
                }
            },
            onScanClick = {
                val currentScanToken = scanRequestToken + 1
                scanRequestToken = currentScanToken
                scanJob?.cancel()
                scanJob = coroutineScope.launch {
                    scanUiState = ScanUiState.Loading()
                    importUiState = ImportUiState.Idle
                    lastImportReport = null
                    emptyFolderCleanupMessage = null
                    emptyFolderCleanupAwaitingConfirmation = false
                    duplicateActionMessage = null
                    duplicateDeleteAwaitingConfirmation = false
                    unsupportedActionMessage = null
                    unsupportedDeleteAwaitingConfirmation = false
                    libraryFiles = emptyList()
                    duplicateFiles = emptyList()
                    try {
                        when (
                            val result = withContext(Dispatchers.Default) {
                                useCases.scanSourceFolder(sourceFolder) { progress ->
                                    coroutineScope.launch {
                                        if (scanRequestToken == currentScanToken && scanUiState is ScanUiState.Loading) {
                                            scanUiState = ScanUiState.Loading(progress)
                                        }
                                    }
                                }
                            }
                        ) {
                            is AppResult.Success -> {
                                if (scanRequestToken != currentScanToken) return@launch
                                val plannedFiles = useCases.buildMediaFilePlan(
                                    destinationFolder = destinationFolder,
                                    mediaFiles = result.data.mediaFiles,
                                    importRules = plan.importRules,
                                )
                                val resolvedFiles = importPlanTargetResolver.resolve(plannedFiles)
                                ScanUiState.Success(
                                    summary = result.data.summary,
                                    plannedFiles = resolvedFiles,
                                    unsupportedFiles = result.data.unsupportedFiles,
                                ).also {
                                    scanUiState = it
                                    selectedFile = it.plannedFiles.firstOrNull()
                                }
                            }

                            is AppResult.Error -> {
                                if (scanRequestToken != currentScanToken) return@launch
                                val message = result.error.toUserMessage()
                                addIssue(uiText("Сканирование", "Scan"), message)
                                scanUiState = ScanUiState.Error(message)
                            }
                        }
                    } catch (exception: CancellationException) {
                        if (scanRequestToken == currentScanToken) {
                            scanUiState = ScanUiState.Canceled
                        }
                    } catch (exception: Throwable) {
                        if (scanRequestToken != currentScanToken) return@launch
                        val message = uiText(
                            ru = "Сканирование прервалось: ${exception.message ?: "без деталей"}",
                            en = "Scan failed: ${exception.message ?: "no details"}",
                        )
                        addIssue(uiText("Сканирование", "Scan"), message)
                        scanUiState = ScanUiState.Error(message)
                    } finally {
                        if (scanRequestToken == currentScanToken) {
                            scanJob = null
                        }
                    }
                }
            },
            onCancelScanClick = {
                val activeScanJob = scanJob
                if (activeScanJob != null) {
                    scanRequestToken += 1
                    activeScanJob.cancel()
                    scanJob = null
                    scanUiState = ScanUiState.Canceled
                }
            },
            onImportClick = {
                val targetFolder = destinationFolder
                if (targetFolder.isNullOrBlank()) {
                    importUiState = ImportUiState.Error(uiText("Не выбрана папка библиотеки.", "Library folder is not selected."))
                } else {
                    importUiState = ImportUiState.CheckingStorageSpace
                    coroutineScope.launch {
                        try {
                            val currentScanSuccess = scanUiState as? ScanUiState.Success
                            val plannedFiles = withContext(Dispatchers.Default) {
                                importPlanTargetResolver.resolve(currentScanSuccess?.plannedFiles.orEmpty())
                            }
                            if (currentScanSuccess != null) {
                                scanUiState = currentScanSuccess.copy(plannedFiles = plannedFiles)
                            }
                            val space = if (importMode == ImportMode.Copy) {
                                withContext(Dispatchers.IO) {
                                    useCases.checkImportStorageSpace(targetFolder, plannedFiles)
                                }
                            } else {
                                null
                            }
                            when (space) {
                                is ImportStorageSpace.Insufficient -> {
                                    importUiState = ImportUiState.Error(
                                        uiText(
                                            ru = "Недостаточно места в папке библиотеки: нужно ${space.requiredBytes.toReadableSize()}, свободно ${space.availableBytes.toReadableSize()}.",
                                            en = "Not enough space in the library folder: required ${space.requiredBytes.toReadableSize()}, available ${space.availableBytes.toReadableSize()}.",
                                        ),
                                    )
                                }

                                ImportStorageSpace.Unavailable -> {
                                    importUiState = ImportUiState.Error(
                                        uiText(
                                            ru = "Не удалось определить свободное место в папке библиотеки. Проверь доступ к диску.",
                                            en = "Could not determine free space in the library folder. Check disk access.",
                                        ),
                                    )
                                }

                                is ImportStorageSpace.Available -> {
                                    importUiState = ImportUiState.AwaitingConfirmation(
                                        readyFileCount = plannedFiles.count { it.targetStatus != ImportTargetStatus.AlreadyExists },
                                        existingFileCount = plannedFiles.count { it.targetStatus == ImportTargetStatus.AlreadyExists },
                                        unsupportedFileCount = currentScanSuccess?.unsupportedFiles.orEmpty().size,
                                        requiredBytes = space.requiredBytes,
                                        availableBytes = space.availableBytes,
                                    )
                                }

                                null -> {
                                    importUiState = ImportUiState.AwaitingConfirmation(
                                        readyFileCount = plannedFiles.count { it.targetStatus != ImportTargetStatus.AlreadyExists },
                                        existingFileCount = plannedFiles.count { it.targetStatus == ImportTargetStatus.AlreadyExists },
                                        unsupportedFileCount = currentScanSuccess?.unsupportedFiles.orEmpty().size,
                                    )
                                }
                            }
                        } catch (exception: Throwable) {
                            val message = uiText(
                                ru = "Не удалось подготовить импорт: ${exception.message ?: "без деталей"}",
                                en = "Could not prepare import: ${exception.message ?: "no details"}",
                            )
                            addIssue(uiText("Импорт", "Import"), message)
                            importUiState = ImportUiState.Error(message)
                        }
                    }
                }
            },
            onCancelImportClick = {
                importUiState = ImportUiState.Idle
            },
            onConfirmImportClick = {
                importJob?.cancel()
                importJob = coroutineScope.launch {
                    val currentScanSuccess = scanUiState as? ScanUiState.Success
                    val scannedPlannedFiles = currentScanSuccess?.plannedFiles.orEmpty()
                    val plannedFiles = withContext(Dispatchers.Default) {
                        importPlanTargetResolver.resolve(scannedPlannedFiles)
                    }
                    if (currentScanSuccess != null) {
                        scanUiState = currentScanSuccess.copy(plannedFiles = plannedFiles)
                    }
                    if (importMode == ImportMode.Copy) {
                        when (val space = withContext(Dispatchers.IO) {
                            useCases.checkImportStorageSpace(destinationFolder.orEmpty(), plannedFiles)
                        }) {
                            is ImportStorageSpace.Insufficient -> {
                                importUiState = ImportUiState.Error(
                                    uiText(
                                        ru = "Недостаточно места в папке библиотеки: нужно ${space.requiredBytes.toReadableSize()}, свободно ${space.availableBytes.toReadableSize()}.",
                                        en = "Not enough space in the library folder: required ${space.requiredBytes.toReadableSize()}, available ${space.availableBytes.toReadableSize()}.",
                                    ),
                                )
                                return@launch
                            }

                            ImportStorageSpace.Unavailable -> {
                                importUiState = ImportUiState.Error(
                                    uiText(
                                        ru = "Не удалось определить свободное место в папке библиотеки. Проверь доступ к диску.",
                                        en = "Could not determine free space in the library folder. Check disk access.",
                                    ),
                                )
                                return@launch
                            }

                            is ImportStorageSpace.Available -> Unit
                        }
                    }
                    val unsupportedSourceFiles = (scanUiState as? ScanUiState.Success)?.unsupportedFiles.orEmpty()
                    val readyFileCount = plannedFiles.count { it.targetStatus != ImportTargetStatus.AlreadyExists }
                    val existingFileCount = plannedFiles.count { it.targetStatus == ImportTargetStatus.AlreadyExists }
                    lastImportProgress = null
                    importUiState = ImportUiState.Loading()
                    try {
                        when (
                            val result = withContext(Dispatchers.Default) {
                                useCases.importMediaFiles(importMode, plannedFiles) { progress ->
                                    coroutineScope.launch {
                                        if (importUiState is ImportUiState.Loading) {
                                            lastImportProgress = progress
                                            importUiState = ImportUiState.Loading(progress)
                                        }
                                    }
                                }
                            }
                        ) {
                            is AppResult.Success -> {
                                val unsupportedQuarantineResult = if (importMode == ImportMode.Move && unsupportedSourceFiles.isNotEmpty()) {
                                    withContext(Dispatchers.Default) {
                                        unsupportedFileQuarantineRepository.moveToQuarantine(
                                            destinationFolder = destinationFolder,
                                            unsupportedFiles = unsupportedSourceFiles,
                                        )
                                    }
                                } else {
                                    null
                                }
                                val finalImportResult = result.data.withUnsupportedQuarantine(
                                    result = unsupportedQuarantineResult,
                                    fallbackFailedFiles = unsupportedSourceFiles.size,
                                )
                                val report = ImportReport(
                                    importMode = importMode,
                                    plannedFiles = plannedFiles.size,
                                    readyFiles = readyFileCount,
                                    existingFiles = existingFileCount,
                                    copiedFiles = finalImportResult.copiedFiles,
                                    movedFiles = finalImportResult.movedFiles,
                                    skippedFiles = finalImportResult.skippedFiles,
                                    failedFiles = finalImportResult.failedFiles,
                                    failedUnsupportedFiles = finalImportResult.failedUnsupportedFiles,
                                    createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                                )
                                lastImportReport = report
                                emptyFolderCleanupMessage = null
                                emptyFolderCleanupAwaitingConfirmation = false
                                importHistoryStorage.appendReport(report)
                                importHistory = importHistoryStorage.loadHistory()
                                refreshLibraryIndexFromDisk(selectFirstFile = false)
                                if (finalImportResult.failedFiles > 0 || finalImportResult.failedUnsupportedFiles > 0) {
                                    addIssue(
                                        title = uiText("Импорт", "Import"),
                                        detail = uiText(
                                            ru = "Ошибки медиа: ${finalImportResult.failedFiles}. Ошибки неподдерживаемых: ${finalImportResult.failedUnsupportedFiles}.",
                                            en = "Media errors: ${finalImportResult.failedFiles}. Unsupported-file errors: ${finalImportResult.failedUnsupportedFiles}.",
                                        ),
                                    )
                                }
                                importUiState = ImportUiState.Success(finalImportResult)
                            }

                            is AppResult.Error -> {
                                val message = result.error.toUserMessage()
                                addIssue(uiText("Импорт", "Import"), message)
                                importUiState = ImportUiState.Error(message)
                            }
                        }
                    } catch (exception: CancellationException) {
                        importUiState = ImportUiState.Canceled(lastImportProgress)
                        refreshLibraryIndexFromDisk(selectFirstFile = false)
                    } catch (exception: Throwable) {
                        val message = uiText(
                            ru = "Импорт прервался: ${exception.message ?: "без деталей"}",
                            en = "Import failed: ${exception.message ?: "no details"}",
                        )
                        addIssue(uiText("Импорт", "Import"), message)
                        importUiState = ImportUiState.Error(message)
                    } finally {
                        importJob = null
                    }
                }
            },
            onCancelRunningImportClick = {
                importJob?.cancel()
                importUiState = ImportUiState.Canceled(lastImportProgress)
                coroutineScope.launch {
                    refreshLibraryIndexFromDisk(selectFirstFile = false)
                }
            },
            onRefreshLibraryClick = {
                if (!isLibraryRefreshing) {
                    refreshLibraryJob = coroutineScope.launch {
                        isLibraryRefreshing = true
                        libraryRefreshProgress = null
                        try {
                            refreshLibraryIndexFromDisk { progress ->
                                coroutineScope.launch {
                                    if (isLibraryRefreshing) {
                                        libraryRefreshProgress = progress
                                    }
                                }
                            }
                        } catch (exception: CancellationException) {
                            libraryRefreshProgress = null
                        } finally {
                            isLibraryRefreshing = false
                            refreshLibraryJob = null
                        }
                    }
                }
            },
            onCancelRefreshLibraryClick = {
                refreshLibraryJob?.cancel()
                isLibraryRefreshing = false
                libraryRefreshProgress = null
            },
            onMoveDuplicatesClick = {
                if (!duplicateActionInProgress) {
                    coroutineScope.launch {
                        duplicateActionInProgress = true
                        duplicateDeleteAwaitingConfirmation = false
                        try {
                            val duplicatesToMove = libraryFiles.duplicateQuarantineCandidates()
                            if (duplicatesToMove.isEmpty()) {
                                duplicateActionMessage = uiText("Дубликаты для переноса не найдены.", "No duplicates found to move.")
                                return@launch
                            }

                            duplicateActionMessage = uiText("Переношу дубликаты в папку дублей...", "Moving duplicates to the duplicates folder...")
                            duplicateActionMessage = try {
                                when (
                                    val result = withContext(Dispatchers.Default) {
                                        duplicateQuarantineRepository.moveToQuarantine(
                                            destinationFolder = destinationFolder,
                                            duplicateFiles = duplicatesToMove,
                                        )
                                    }
                                ) {
                                    is AppResult.Success -> {
                                        refreshLibraryIndexFromDisk()
                                        if (result.data.failedFiles > 0) {
                                            addIssue(
                                                title = uiText("Дубликаты", "Duplicates"),
                                                detail = uiText(
                                                    ru = "Часть дублей не удалось перенести в папку дублей: ${result.data.failedFiles}.",
                                                    en = "Some duplicates could not be moved to the duplicates folder: ${result.data.failedFiles}.",
                                                ),
                                            )
                                        }
                                        uiText(
                                            ru = "Перенесено в папку дублей: ${result.data.movedFiles}, ошибок: ${result.data.failedFiles}.",
                                            en = "Moved to duplicates folder: ${result.data.movedFiles}, errors: ${result.data.failedFiles}.",
                                        )
                                    }

                                    is AppResult.Error -> {
                                        val message = result.error.toUserMessage()
                                        addIssue(uiText("Дубликаты", "Duplicates"), message)
                                        message
                                    }
                                }
                            } catch (exception: Throwable) {
                                val message = uiText(
                                    ru = "Не удалось перенести дубликаты: ${exception.message ?: "без деталей"}",
                                    en = "Could not move duplicates: ${exception.message ?: "no details"}",
                                )
                                addIssue(uiText("Дубликаты", "Duplicates"), message)
                                message
                            }
                        } finally {
                            duplicateActionInProgress = false
                        }
                    }
                }
            },
            onRequestDeleteQuarantineClick = {
                if (duplicateFiles.isEmpty()) {
                    duplicateActionMessage = uiText(
                        ru = "В папке дублей пока нет файлов для переноса в Корзину.",
                        en = "There are no files in the duplicates folder to move to Trash.",
                    )
                    duplicateDeleteAwaitingConfirmation = false
                } else {
                    duplicateDeleteAwaitingConfirmation = true
                    duplicateActionMessage = uiText(
                        ru = "Будет перемещено в Корзину из папки дублей: ${duplicateFiles.size}. Библиотеку не трогаем.",
                        en = "Will move files from the duplicates folder to Trash: ${duplicateFiles.size}. The library will not be touched.",
                    )
                }
            },
            onCancelDeleteQuarantineClick = {
                duplicateDeleteAwaitingConfirmation = false
                duplicateActionMessage = uiText("Перенос в Корзину отменен.", "Move to Trash canceled.")
            },
            onConfirmDeleteQuarantineClick = {
                if (!duplicateActionInProgress) {
                    coroutineScope.launch {
                        duplicateActionInProgress = true
                        try {
                            if (duplicateFiles.isEmpty()) {
                                duplicateDeleteAwaitingConfirmation = false
                                duplicateActionMessage = uiText(
                                    ru = "В папке дублей пока нет файлов для переноса в Корзину.",
                                    en = "There are no files in the duplicates folder to move to Trash.",
                                )
                                return@launch
                            }

                            duplicateActionMessage = uiText(
                                ru = "Перемещаю файлы из папки дублей в Корзину...",
                                en = "Moving files from the duplicates folder to Trash...",
                            )
                            duplicateActionMessage = try {
                                when (
                                    val result = withContext(Dispatchers.Default) {
                                        duplicateQuarantineRepository.deleteFromQuarantine(
                                            destinationFolder = destinationFolder,
                                            quarantineFiles = duplicateFiles,
                                        )
                                    }
                                ) {
                                    is AppResult.Success -> {
                                        duplicateDeleteAwaitingConfirmation = false
                                        refreshLibraryIndexFromDisk()
                                        if (result.data.failedFiles > 0) {
                                            addIssue(
                                                title = uiText("Дубликаты", "Duplicates"),
                                                detail = uiText(
                                                    ru = "Часть файлов из папки дублей не удалось переместить в Корзину: ${result.data.failedFiles}.",
                                                    en = "Some files from the duplicates folder could not be moved to Trash: ${result.data.failedFiles}.",
                                                ),
                                            )
                                        }
                                        uiText(
                                            ru = "Перемещено в Корзину из папки дублей: ${result.data.deletedFiles}, ошибок: ${result.data.failedFiles}.",
                                            en = "Moved from duplicates folder to Trash: ${result.data.deletedFiles}, errors: ${result.data.failedFiles}.",
                                        )
                                    }

                                    is AppResult.Error -> {
                                        duplicateDeleteAwaitingConfirmation = false
                                        val message = result.error.toUserMessage()
                                        addIssue(uiText("Дубликаты", "Duplicates"), message)
                                        message
                                    }
                                }
                            } catch (exception: Throwable) {
                                duplicateDeleteAwaitingConfirmation = false
                                val message = uiText(
                                    ru = "Не удалось переместить файлы из папки дублей в Корзину: ${exception.message ?: "без деталей"}",
                                    en = "Could not move files from the duplicates folder to Trash: ${exception.message ?: "no details"}",
                                )
                                addIssue(uiText("Дубликаты", "Duplicates"), message)
                                message
                            }
                        } finally {
                            duplicateActionInProgress = false
                        }
                    }
                }
            },
            onRequestDeleteUnsupportedClick = {
                if (unsupportedFiles.isEmpty()) {
                    unsupportedActionMessage = uiText(
                        ru = "В папке пропущенных файлов пока нечего переносить в Корзину.",
                        en = "There are no skipped files to move to Trash.",
                    )
                    unsupportedDeleteAwaitingConfirmation = false
                } else {
                    unsupportedDeleteAwaitingConfirmation = true
                    unsupportedActionMessage = uiText(
                        ru = "Будет перемещено в Корзину пропущенных файлов: ${unsupportedFiles.size}. Библиотеку и исходники не трогаем.",
                        en = "Will move skipped files to Trash: ${unsupportedFiles.size}. The library and source files will not be touched.",
                    )
                }
            },
            onCancelDeleteUnsupportedClick = {
                unsupportedDeleteAwaitingConfirmation = false
                unsupportedActionMessage = uiText("Перенос в Корзину отменен.", "Move to Trash canceled.")
            },
            onConfirmDeleteUnsupportedClick = {
                if (!unsupportedActionInProgress) {
                    coroutineScope.launch {
                        unsupportedActionInProgress = true
                        try {
                            if (unsupportedFiles.isEmpty()) {
                                unsupportedDeleteAwaitingConfirmation = false
                                unsupportedActionMessage = uiText(
                                    ru = "В папке пропущенных файлов пока нечего переносить в Корзину.",
                                    en = "There are no skipped files to move to Trash.",
                                )
                                return@launch
                            }

                            unsupportedActionMessage = uiText(
                                ru = "Перемещаю пропущенные файлы в Корзину...",
                                en = "Moving skipped files to Trash...",
                            )
                            unsupportedActionMessage = try {
                                when (
                                    val result = withContext(Dispatchers.Default) {
                                        unsupportedFileQuarantineRepository.deleteFromQuarantine(
                                            destinationFolder = destinationFolder,
                                            quarantineFiles = unsupportedFiles,
                                        )
                                    }
                                ) {
                                    is AppResult.Success -> {
                                        unsupportedDeleteAwaitingConfirmation = false
                                        refreshLibraryIndexFromDisk(selectFirstFile = false)
                                        if (result.data.failedFiles > 0) {
                                            addIssue(
                                                title = uiText("Неподдерживаемые", "Unsupported"),
                                                detail = uiText(
                                                    ru = "Часть пропущенных файлов не удалось переместить в Корзину: ${result.data.failedFiles}.",
                                                    en = "Some skipped files could not be moved to Trash: ${result.data.failedFiles}.",
                                                ),
                                            )
                                        }
                                        uiText(
                                            ru = "Перемещено в Корзину пропущенных файлов: ${result.data.deletedFiles}, ошибок: ${result.data.failedFiles}.",
                                            en = "Moved skipped files to Trash: ${result.data.deletedFiles}, errors: ${result.data.failedFiles}.",
                                        )
                                    }

                                    is AppResult.Error -> {
                                        unsupportedDeleteAwaitingConfirmation = false
                                        val message = result.error.toUserMessage()
                                        addIssue(uiText("Неподдерживаемые", "Unsupported"), message)
                                        message
                                    }
                                }
                            } catch (exception: Throwable) {
                                unsupportedDeleteAwaitingConfirmation = false
                                val message = uiText(
                                    ru = "Не удалось переместить пропущенные файлы в Корзину: ${exception.message ?: "без деталей"}",
                                    en = "Could not move skipped files to Trash: ${exception.message ?: "no details"}",
                                )
                                addIssue(uiText("Неподдерживаемые", "Unsupported"), message)
                                message
                            }
                        } finally {
                            unsupportedActionInProgress = false
                        }
                    }
                }
            },
            onRequestEmptyFolderCleanupClick = {
                emptyFolderCleanupAwaitingConfirmation = true
                emptyFolderCleanupMessage = uiText(
                    ru = "Будут удалены только пустые подпапки внутри исходной папки. Файлы не удаляются.",
                    en = "Only empty subfolders inside the source folder will be removed. Files are not deleted.",
                )
            },
            onCancelEmptyFolderCleanupClick = {
                emptyFolderCleanupAwaitingConfirmation = false
                emptyFolderCleanupMessage = uiText("Очистка пустых папок отменена.", "Empty folder cleanup canceled.")
            },
            onConfirmEmptyFolderCleanupClick = {
                coroutineScope.launch {
                    emptyFolderCleanupMessage = uiText("Удаляю пустые папки источника...", "Removing empty source folders...")
                    emptyFolderCleanupMessage = try {
                        when (
                            val result = withContext(Dispatchers.Default) {
                                emptyFolderCleanupRepository.deleteEmptyFolders(sourceFolder)
                            }
                        ) {
                            is AppResult.Success -> {
                                emptyFolderCleanupAwaitingConfirmation = false
                                if (result.data.failedFolders > 0) {
                                    addIssue(
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
                                emptyFolderCleanupAwaitingConfirmation = false
                                val message = result.error.toUserMessage()
                                addIssue(uiText("Пустые папки", "Empty Folders"), message)
                                message
                            }
                        }
                    } catch (exception: Throwable) {
                        emptyFolderCleanupAwaitingConfirmation = false
                        val message = uiText(
                            ru = "Не удалось удалить пустые папки: ${exception.message ?: "без деталей"}",
                            en = "Could not remove empty folders: ${exception.message ?: "no details"}",
                        )
                        addIssue(uiText("Пустые папки", "Empty Folders"), message)
                        message
                    }
                }
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
                if (!selectedFileTrashInProgress) {
                    val file = selectedFile
                    if (file == null) {
                        selectedFileTrashAwaitingConfirmation = false
                        selectedFileTrashMessage = uiText("Файл не выбран.", "No file selected.")
                    } else if (selectedSection == AppSection.Import) {
                        selectedFileTrashAwaitingConfirmation = false
                        selectedFileTrashMessage = uiText(
                            ru = "В разделе импорта файл из исходной папки не переносится в Корзину. Сначала проверь план импорта.",
                            en = "In the import section, source files are not moved to Trash. Review the import plan first.",
                        )
                    } else if (selectedFileTrashAwaitingConfirmation) {
                        coroutineScope.launch {
                            selectedFileTrashInProgress = true
                            val path = file.sourcePath
                            selectedFileTrashMessage = uiText("Перемещаю выбранный файл в Корзину...", "Moving selected file to Trash...")
                            selectedFileTrashMessage = try {
                                val result = withContext(Dispatchers.Default) {
                                    useCases.moveSelectedFileToTrash(
                                        selectedFile = file,
                                        isSourceFileActionAllowed = selectedSection != AppSection.Import,
                                    )
                                }
                                selectedFileTrashAwaitingConfirmation = false
                                when (result) {
                                    is MoveSelectedFileToTrashResult.Moved -> {
                                        appState.removeFileFromVisibleState(
                                            path = result.path,
                                            preferredSelectionFiles = navigationFiles,
                                        )
                                        coroutineScope.launch {
                                            try {
                                                refreshLibraryIndexFromDisk(selectFirstFile = false)
                                            } catch (exception: Throwable) {
                                                // The file is already in Trash. A refresh failure should not turn a successful
                                                // delete into a blocking system dialog.
                                            }
                                        }
                                        uiText("Файл перемещен в Корзину.", "File moved to Trash.")
                                    }
                                    MoveSelectedFileToTrashResult.FileNotSelected -> uiText("Файл не выбран.", "No file selected.")
                                    MoveSelectedFileToTrashResult.SourceFileActionNotAllowed -> {
                                        uiText(
                                            ru = "В разделе импорта файл из исходной папки не переносится в Корзину. Сначала проверь план импорта.",
                                            en = "In the import section, source files are not moved to Trash. Review the import plan first.",
                                        )
                                    }
                                    MoveSelectedFileToTrashResult.TimedOut -> {
                                        val message = uiText(
                                            ru = "Перенос в Корзину занял слишком много времени. Проверь доступ к диску или попробуй открыть файл в папке.",
                                            en = "Moving to Trash took too long. Check disk access or try opening the file in its folder.",
                                        )
                                        addIssue(
                                            uiText("Корзина", "Trash"),
                                            uiText("$message Файл: $path", "$message File: $path"),
                                        )
                                        message
                                    }
                                    MoveSelectedFileToTrashResult.Failed -> {
                                        addIssue(
                                            uiText("Корзина", "Trash"),
                                            uiText("Не удалось переместить файл в Корзину: $path", "Could not move file to Trash: $path"),
                                        )
                                        uiText("Не удалось переместить файл в Корзину.", "Could not move file to Trash.")
                                    }
                                    is MoveSelectedFileToTrashResult.Error -> {
                                        val message = uiText(
                                            ru = "Не удалось переместить файл в Корзину: ${result.message ?: "без деталей"}",
                                            en = "Could not move file to Trash: ${result.message ?: "no details"}",
                                        )
                                        addIssue(uiText("Корзина", "Trash"), message)
                                        message
                                    }
                                }
                            } catch (exception: Throwable) {
                                selectedFileTrashAwaitingConfirmation = false
                                val message = uiText(
                                    ru = "Не удалось переместить файл в Корзину: ${exception.message ?: "без деталей"}",
                                    en = "Could not move file to Trash: ${exception.message ?: "no details"}",
                                )
                                addIssue(uiText("Корзина", "Trash"), message)
                                message
                            } finally {
                                selectedFileTrashInProgress = false
                            }
                        }
                    } else {
                        selectedFileTrashAwaitingConfirmation = true
                        selectedFileTrashMessage = uiText(
                            ru = "Будет перемещен в Корзину только выбранный файл: ${file.fileName}",
                            en = "Only the selected file will be moved to Trash: ${file.fileName}",
                        )
                    }
                }
            },
            onCancelMoveSelectedFileToTrashClick = {
                selectedFileTrashAwaitingConfirmation = false
                selectedFileTrashMessage = uiText("Перенос выбранного файла отменен.", "Moving selected file canceled.")
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
