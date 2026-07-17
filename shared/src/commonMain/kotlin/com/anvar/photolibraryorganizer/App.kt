package com.anvar.photolibraryorganizer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.ImportStorageSpace
import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.domain.repository.DuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.domain.repository.EmptyFolderCleanupRepository
import com.anvar.photolibraryorganizer.domain.repository.LibraryIndexStorage
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.ImportPlanTargetResolver
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.repository.UnsupportedFileQuarantineRepository
import com.anvar.photolibraryorganizer.domain.repository.StorageSpaceProvider
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.CheckImportStorageSpaceUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ImportMediaFilesUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import com.anvar.photolibraryorganizer.presentation.AppSettings
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.AppIssue
import com.anvar.photolibraryorganizer.presentation.CachingImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.FileRevealHandler
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ImportHistoryStorage
import com.anvar.photolibraryorganizer.presentation.ImportReport
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.PreviewDuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.presentation.PreviewEmptyFolderCleanupRepository
import com.anvar.photolibraryorganizer.presentation.PreviewAppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.PreviewFileRevealHandler
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

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
    folderPicker: FolderPicker = PreviewFolderPicker,
    fileRevealHandler: FileRevealHandler = PreviewFileRevealHandler,
) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        var sourceFolder by remember { mutableStateOf<String?>(null) }
        var destinationFolder by remember { mutableStateOf<String?>(null) }
        var importMode by remember { mutableStateOf(ImportMode.ScanOnly) }
        var importRules by remember { mutableStateOf(ImportOrganizationRules.Default) }
        var selectedSection by remember { mutableStateOf(AppSection.AllPhotos) }
        var scanUiState by remember { mutableStateOf<ScanUiState>(ScanUiState.Idle) }
        var scanRequestToken by remember { mutableStateOf(0) }
        var scanJob by remember { mutableStateOf<Job?>(null) }
        var importJob by remember { mutableStateOf<Job?>(null) }
        var lastImportProgress by remember { mutableStateOf<ImportMediaFilesProgress?>(null) }
        var importUiState by remember { mutableStateOf<ImportUiState>(ImportUiState.Idle) }
        var lastImportReport by remember { mutableStateOf<ImportReport?>(null) }
        var emptyFolderCleanupMessage by remember { mutableStateOf<String?>(null) }
        var emptyFolderCleanupAwaitingConfirmation by remember { mutableStateOf(false) }
        var importHistory by remember { mutableStateOf<List<ImportReport>>(emptyList()) }
        var selectedFile by remember { mutableStateOf<PlannedMediaFile?>(null) }
        var libraryFiles by remember { mutableStateOf<List<PlannedMediaFile>>(emptyList()) }
        var duplicateFiles by remember { mutableStateOf<List<PlannedMediaFile>>(emptyList()) }
        var unsupportedFiles by remember { mutableStateOf<List<PlannedMediaFile>>(emptyList()) }
        var isLibraryRefreshing by remember { mutableStateOf(false) }
        var duplicateActionMessage by remember { mutableStateOf<String?>(null) }
        var duplicateDeleteAwaitingConfirmation by remember { mutableStateOf(false) }
        var unsupportedActionMessage by remember { mutableStateOf<String?>(null) }
        var unsupportedDeleteAwaitingConfirmation by remember { mutableStateOf(false) }
        var imagePreviewUiState by remember { mutableStateOf<ImagePreviewUiState>(ImagePreviewUiState.Empty) }
        var issues by remember { mutableStateOf<List<AppIssue>>(emptyList()) }
        var nextIssueId by remember { mutableStateOf(1) }

        val coroutineScope = rememberCoroutineScope()
        fun addIssue(title: String, detail: String) {
            issues = listOf(AppIssue(nextIssueId, title, detail)) + issues
            nextIssueId += 1
        }

        val scanSourceFolderUseCase = remember(photoSourceScanner) {
            ScanSourceFolderUseCase(photoSourceScanner)
        }
        val importMediaFilesUseCase = remember(mediaFileImporter) {
            ImportMediaFilesUseCase(mediaFileImporter)
        }
        val checkImportStorageSpaceUseCase = remember(storageSpaceProvider) {
            CheckImportStorageSpaceUseCase(storageSpaceProvider)
        }
        val buildMediaFilePlanUseCase = remember { BuildMediaFilePlanUseCase() }
        val resolveImportAvailabilityUseCase = remember { ResolveImportAvailabilityUseCase() }
        val cachedImagePreviewLoader = remember(imagePreviewLoader) {
            CachingImagePreviewLoader(imagePreviewLoader)
        }

        fun applyLibraryIndexSnapshot(
            snapshot: LibraryIndexSnapshot,
            selectFirstFile: Boolean,
        ) {
            libraryFiles = snapshot.libraryFiles
            duplicateFiles = snapshot.duplicateFiles
            unsupportedFiles = snapshot.unsupportedFiles
            selectedFile = if (selectFirstFile) {
                libraryFiles.firstOrNull()
            } else {
                selectedFile?.takeIf { selected ->
                    (libraryFiles + duplicateFiles + unsupportedFiles).any { it.sourcePath == selected.sourcePath }
                } ?: libraryFiles.firstOrNull()
            }
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
        ) {
            val snapshot = refreshLibraryIndexSnapshot(
                destinationFolder = destinationFolder,
                photoSourceScanner = photoSourceScanner,
                libraryIndexStorage = libraryIndexStorage,
            )
            applyLibraryIndexSnapshot(snapshot, selectFirstFile)
        }

        LaunchedEffect(Unit) {
            val settings = appSettingsStorage.loadSettings()
            importHistory = importHistoryStorage.loadHistory()
            sourceFolder = settings.sourceFolder
            destinationFolder = settings.destinationFolder
            importRules = settings.importRules
            loadLibraryIndexIfAvailable(settings.destinationFolder)
        }

        LaunchedEffect(selectedFile) {
            val file = selectedFile
            imagePreviewUiState = if (file == null) {
                ImagePreviewUiState.Empty
            } else {
                imagePreviewUiState = ImagePreviewUiState.Loading
                cachedImagePreviewLoader.loadImage(file.sourcePath)?.let { image ->
                    ImagePreviewUiState.Success(image)
                } ?: ImagePreviewUiState.Unsupported
            }
        }

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
            unsupportedActionMessage = unsupportedActionMessage,
            unsupportedDeleteAwaitingConfirmation = unsupportedDeleteAwaitingConfirmation,
            importAvailability = resolveImportAvailabilityUseCase(
                importMode = importMode,
                plannedFiles = (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty(),
            ),
            isLibraryRefreshing = isLibraryRefreshing,
            issues = issues,
            onSourceFolderClick = {
                folderPicker.chooseFolder("Выбери исходную папку")?.let {
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
                scanUiState = ScanUiState.Idle
                importUiState = ImportUiState.Idle
                lastImportReport = null
                emptyFolderCleanupMessage = null
                emptyFolderCleanupAwaitingConfirmation = false
                duplicateActionMessage = null
                duplicateDeleteAwaitingConfirmation = false
                unsupportedActionMessage = null
                unsupportedDeleteAwaitingConfirmation = false
                selectedFile = null
                libraryFiles = emptyList()
                duplicateFiles = emptyList()
                unsupportedFiles = emptyList()
                imagePreviewUiState = ImagePreviewUiState.Empty
            },
            onDestinationFolderClick = {
                folderPicker.chooseFolder("Выбери папку библиотеки")?.let {
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
                scanUiState = ScanUiState.Idle
                importUiState = ImportUiState.Idle
                lastImportReport = null
                emptyFolderCleanupMessage = null
                emptyFolderCleanupAwaitingConfirmation = false
                duplicateActionMessage = null
                duplicateDeleteAwaitingConfirmation = false
                unsupportedActionMessage = null
                unsupportedDeleteAwaitingConfirmation = false
                selectedFile = null
                coroutineScope.launch {
                    if (!loadLibraryIndexIfAvailable(destinationFolder)) {
                        libraryFiles = emptyList()
                        duplicateFiles = emptyList()
                        unsupportedFiles = emptyList()
                    }
                }
                imagePreviewUiState = ImagePreviewUiState.Empty
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
                                scanSourceFolderUseCase(sourceFolder) { progress ->
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
                                val plannedFiles = buildMediaFilePlanUseCase(
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
                                addIssue("Сканирование", message)
                                scanUiState = ScanUiState.Error(message)
                            }
                        }
                    } catch (exception: CancellationException) {
                        if (scanRequestToken == currentScanToken) {
                            scanUiState = ScanUiState.Canceled
                        }
                    } catch (exception: Throwable) {
                        if (scanRequestToken != currentScanToken) return@launch
                        val message = "Сканирование прервалось: ${exception.message ?: "без деталей"}"
                        addIssue("Сканирование", message)
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
                    importUiState = ImportUiState.Error("Не выбрана папка библиотеки.")
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
                                    checkImportStorageSpaceUseCase(targetFolder, plannedFiles)
                                }
                            } else {
                                null
                            }
                            when (space) {
                                is ImportStorageSpace.Insufficient -> {
                                    importUiState = ImportUiState.Error(
                                        "Недостаточно места в папке библиотеки: нужно ${space.requiredBytes.toReadableSize()}, свободно ${space.availableBytes.toReadableSize()}.",
                                    )
                                }

                                ImportStorageSpace.Unavailable -> {
                                    importUiState = ImportUiState.Error(
                                        "Не удалось определить свободное место в папке библиотеки. Проверь доступ к диску.",
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
                            val message = "Не удалось подготовить импорт: ${exception.message ?: "без деталей"}"
                            addIssue("Импорт", message)
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
                            checkImportStorageSpaceUseCase(destinationFolder.orEmpty(), plannedFiles)
                        }) {
                            is ImportStorageSpace.Insufficient -> {
                                importUiState = ImportUiState.Error(
                                    "Недостаточно места в папке библиотеки: нужно ${space.requiredBytes.toReadableSize()}, свободно ${space.availableBytes.toReadableSize()}.",
                                )
                                return@launch
                            }

                            ImportStorageSpace.Unavailable -> {
                                importUiState = ImportUiState.Error(
                                    "Не удалось определить свободное место в папке библиотеки. Проверь доступ к диску.",
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
                                importMediaFilesUseCase(importMode, plannedFiles) { progress ->
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
                                        title = "Импорт",
                                        detail = "Ошибки медиа: ${finalImportResult.failedFiles}. Ошибки неподдерживаемых: ${finalImportResult.failedUnsupportedFiles}.",
                                    )
                                }
                                importUiState = ImportUiState.Success(finalImportResult)
                            }

                            is AppResult.Error -> {
                                val message = result.error.toUserMessage()
                                addIssue("Импорт", message)
                                importUiState = ImportUiState.Error(message)
                            }
                        }
                    } catch (exception: CancellationException) {
                        importUiState = ImportUiState.Canceled(lastImportProgress)
                        refreshLibraryIndexFromDisk(selectFirstFile = false)
                    } catch (exception: Throwable) {
                        val message = "Импорт прервался: ${exception.message ?: "без деталей"}"
                        addIssue("Импорт", message)
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
                coroutineScope.launch {
                    isLibraryRefreshing = true
                    try {
                        refreshLibraryIndexFromDisk()
                    } finally {
                        isLibraryRefreshing = false
                    }
                }
            },
            onMoveDuplicatesClick = {
                coroutineScope.launch {
                    duplicateDeleteAwaitingConfirmation = false
                    val duplicatesToMove = libraryFiles.duplicateQuarantineCandidates()
                    if (duplicatesToMove.isEmpty()) {
                        duplicateActionMessage = "Дубликаты для переноса не найдены."
                        return@launch
                    }

                    duplicateActionMessage = "Переношу дубликаты в папку дублей..."
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
                                        title = "Дубликаты",
                                        detail = "Часть дублей не удалось перенести в папку дублей: ${result.data.failedFiles}.",
                                    )
                                }
                                "Перенесено в папку дублей: ${result.data.movedFiles}, ошибок: ${result.data.failedFiles}."
                            }

                            is AppResult.Error -> {
                                val message = result.error.toUserMessage()
                                addIssue("Дубликаты", message)
                                message
                            }
                        }
                    } catch (exception: Throwable) {
                        val message = "Не удалось перенести дубликаты: ${exception.message ?: "без деталей"}"
                        addIssue("Дубликаты", message)
                        message
                    }
                }
            },
            onRequestDeleteQuarantineClick = {
                if (duplicateFiles.isEmpty()) {
                    duplicateActionMessage = "В папке дублей пока нет файлов для переноса в Корзину."
                    duplicateDeleteAwaitingConfirmation = false
                } else {
                    duplicateDeleteAwaitingConfirmation = true
                    duplicateActionMessage = "Будет перемещено в Корзину из папки дублей: ${duplicateFiles.size}. Библиотеку не трогаем."
                }
            },
            onCancelDeleteQuarantineClick = {
                duplicateDeleteAwaitingConfirmation = false
                duplicateActionMessage = "Перенос в Корзину отменен."
            },
            onConfirmDeleteQuarantineClick = {
                coroutineScope.launch {
                    if (duplicateFiles.isEmpty()) {
                        duplicateDeleteAwaitingConfirmation = false
                        duplicateActionMessage = "В папке дублей пока нет файлов для переноса в Корзину."
                        return@launch
                    }

                    duplicateActionMessage = "Перемещаю файлы из папки дублей в Корзину..."
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
                                        title = "Дубликаты",
                                        detail = "Часть файлов из папки дублей не удалось переместить в Корзину: ${result.data.failedFiles}.",
                                    )
                                }
                                "Перемещено в Корзину из папки дублей: ${result.data.deletedFiles}, ошибок: ${result.data.failedFiles}."
                            }

                            is AppResult.Error -> {
                                duplicateDeleteAwaitingConfirmation = false
                                val message = result.error.toUserMessage()
                                addIssue("Дубликаты", message)
                                message
                            }
                        }
                    } catch (exception: Throwable) {
                        duplicateDeleteAwaitingConfirmation = false
                        val message = "Не удалось переместить файлы из папки дублей в Корзину: ${exception.message ?: "без деталей"}"
                        addIssue("Дубликаты", message)
                        message
                    }
                }
            },
            onRequestDeleteUnsupportedClick = {
                if (unsupportedFiles.isEmpty()) {
                    unsupportedActionMessage = "В папке пропущенных файлов пока нечего переносить в Корзину."
                    unsupportedDeleteAwaitingConfirmation = false
                } else {
                    unsupportedDeleteAwaitingConfirmation = true
                    unsupportedActionMessage = "Будет перемещено в Корзину пропущенных файлов: ${unsupportedFiles.size}. Библиотеку и исходники не трогаем."
                }
            },
            onCancelDeleteUnsupportedClick = {
                unsupportedDeleteAwaitingConfirmation = false
                unsupportedActionMessage = "Перенос в Корзину отменен."
            },
            onConfirmDeleteUnsupportedClick = {
                coroutineScope.launch {
                    if (unsupportedFiles.isEmpty()) {
                        unsupportedDeleteAwaitingConfirmation = false
                        unsupportedActionMessage = "В папке пропущенных файлов пока нечего переносить в Корзину."
                        return@launch
                    }

                    unsupportedActionMessage = "Перемещаю пропущенные файлы в Корзину..."
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
                                        title = "Неподдерживаемые",
                                        detail = "Часть пропущенных файлов не удалось переместить в Корзину: ${result.data.failedFiles}.",
                                    )
                                }
                                "Перемещено в Корзину пропущенных файлов: ${result.data.deletedFiles}, ошибок: ${result.data.failedFiles}."
                            }

                            is AppResult.Error -> {
                                unsupportedDeleteAwaitingConfirmation = false
                                val message = result.error.toUserMessage()
                                addIssue("Неподдерживаемые", message)
                                message
                            }
                        }
                    } catch (exception: Throwable) {
                        unsupportedDeleteAwaitingConfirmation = false
                        val message = "Не удалось переместить пропущенные файлы в Корзину: ${exception.message ?: "без деталей"}"
                        addIssue("Неподдерживаемые", message)
                        message
                    }
                }
            },
            onRequestEmptyFolderCleanupClick = {
                emptyFolderCleanupAwaitingConfirmation = true
                emptyFolderCleanupMessage = "Будут удалены только пустые подпапки внутри исходной папки. Файлы не удаляются."
            },
            onCancelEmptyFolderCleanupClick = {
                emptyFolderCleanupAwaitingConfirmation = false
                emptyFolderCleanupMessage = "Очистка пустых папок отменена."
            },
            onConfirmEmptyFolderCleanupClick = {
                coroutineScope.launch {
                    emptyFolderCleanupMessage = "Удаляю пустые папки источника..."
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
                                        title = "Пустые папки",
                                        detail = "Часть пустых папок не удалось удалить: ${result.data.failedFolders}.",
                                    )
                                }
                                "Удалено пустых папок: ${result.data.deletedFolders}, ошибок: ${result.data.failedFolders}."
                            }

                            is AppResult.Error -> {
                                emptyFolderCleanupAwaitingConfirmation = false
                                val message = result.error.toUserMessage()
                                addIssue("Пустые папки", message)
                                message
                            }
                        }
                    } catch (exception: Throwable) {
                        emptyFolderCleanupAwaitingConfirmation = false
                        val message = "Не удалось удалить пустые папки: ${exception.message ?: "без деталей"}"
                        addIssue("Пустые папки", message)
                        message
                    }
                }
            },
            onOpenLibraryFolderClick = {
                openLibrarySubfolder(
                    destinationFolder = destinationFolder,
                    subfolder = "Library",
                    title = "Библиотека",
                    fileRevealHandler = fileRevealHandler,
                    addIssue = ::addIssue,
                )
            },
            onOpenUnsupportedFolderClick = {
                openLibrarySubfolder(
                    destinationFolder = destinationFolder,
                    subfolder = "Unsupported",
                    title = "Пропущенные",
                    fileRevealHandler = fileRevealHandler,
                    addIssue = ::addIssue,
                )
            },
            onOpenUnsupportedTypeFolderClick = { type ->
                openLibrarySubfolder(
                    destinationFolder = destinationFolder,
                    subfolder = "Unsupported/${type.toUnsupportedFolderName()}",
                    title = "Неподдерживаемые",
                    fileRevealHandler = fileRevealHandler,
                    addIssue = ::addIssue,
                )
            },
            onOpenDuplicatesFolderClick = {
                openLibrarySubfolder(
                    destinationFolder = destinationFolder,
                    subfolder = "Duplicates",
                    title = "Дубли",
                    fileRevealHandler = fileRevealHandler,
                    addIssue = ::addIssue,
                )
            },
            onClearIssuesClick = { issues = emptyList() },
            onSectionSelected = { selectedSection = it },
            selectedFile = selectedFile,
            selectedFileIndex = selectedFileIndex,
            navigationFileCount = navigationFiles.size,
            imagePreviewUiState = imagePreviewUiState,
            onOpenFileClick = {
                selectedFile?.sourcePath?.let { path ->
                    if (!fileRevealHandler.open(path)) {
                        addIssue("Просмотр", "Не удалось открыть файл: $path")
                    }
                }
            },
            onRevealFileClick = {
                selectedFile?.sourcePath?.let { path ->
                    if (!fileRevealHandler.reveal(path)) {
                        addIssue("Просмотр", "Не удалось показать файл в папке: $path")
                    }
                }
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

private fun PhotoLibraryError.toUserMessage(): String {
    return when (this) {
        PhotoLibraryError.InvalidSourceFolder -> "Исходная папка не выбрана."
        is PhotoLibraryError.SourceFolderNotFound -> "Исходная папка не найдена: $path"
        is PhotoLibraryError.SourceFolderIsNotDirectory -> "Выбранный путь не является папкой: $path"
        PhotoLibraryError.ImportPlanIsEmpty -> "Нет плана импорта. Сначала выполни сканирование."
        PhotoLibraryError.UnsupportedImportMode -> "Этот режим импорта пока не поддерживается."
        is PhotoLibraryError.FileSystem -> "Не удалось выполнить файловую операцию: $message"
        is PhotoLibraryError.Unknown -> "Неизвестная ошибка: ${message ?: "без деталей"}"
    }
}

private fun ImportMediaFilesResult.withUnsupportedQuarantine(
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

private fun openLibrarySubfolder(
    destinationFolder: String?,
    subfolder: String,
    title: String,
    fileRevealHandler: FileRevealHandler,
    addIssue: (String, String) -> Unit,
) {
    val path = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/$subfolder" }
    if (path == null) {
        addIssue(title, "Папка библиотеки не выбрана.")
        return
    }

    if (!fileRevealHandler.open(path)) {
        addIssue(title, "Не удалось открыть папку: $path")
    }
}

private fun String.toUnsupportedFolderName(): String {
    return if (this == "без расширения") "no-extension" else this
}

private suspend fun refreshLibraryFiles(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
): List<PlannedMediaFile> {
    val libraryFolder = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/Library" }
        ?: return emptyList()

    return refreshPlannedFiles(libraryFolder, photoSourceScanner)
}

private suspend fun refreshLibraryIndexSnapshot(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
    libraryIndexStorage: LibraryIndexStorage,
): LibraryIndexSnapshot {
    val destination = destinationFolder?.trim()?.trimEnd('/').orEmpty()
    val snapshot = LibraryIndexSnapshot(
        destinationFolder = destination,
        libraryFiles = refreshLibraryFiles(destination, photoSourceScanner),
        duplicateFiles = refreshDuplicateFiles(destination, photoSourceScanner),
        unsupportedFiles = refreshUnsupportedFiles(destination, photoSourceScanner),
        updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
    )
    libraryIndexStorage.save(snapshot)
    return snapshot
}

private suspend fun refreshDuplicateFiles(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
): List<PlannedMediaFile> {
    val duplicatesFolder = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/Duplicates" }
        ?: return emptyList()

    return refreshPlannedFiles(duplicatesFolder, photoSourceScanner)
}

private suspend fun refreshUnsupportedFiles(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
): List<PlannedMediaFile> {
    val unsupportedFolder = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/Unsupported" }
        ?: return emptyList()

    return refreshAllFiles(unsupportedFolder, photoSourceScanner)
}

private suspend fun refreshPlannedFiles(
    folder: String,
    photoSourceScanner: PhotoSourceScanner,
): List<PlannedMediaFile> {
    return when (
        val result = withContext(Dispatchers.Default) {
            photoSourceScanner.scanFolder(
                path = folder,
                readContentHash = false,
            )
        }
    ) {
        is AppResult.Success -> result.data.mediaFiles.map { mediaFile ->
            PlannedMediaFile(
                sourcePath = mediaFile.path,
                fileName = mediaFile.fileName,
                targetRelativePath = mediaFile.path,
                sizeBytes = mediaFile.sizeBytes,
                contentHash = mediaFile.contentHash,
                capturedAtEpochMillis = mediaFile.capturedAtEpochMillis,
                modifiedAtEpochMillis = mediaFile.modifiedAtEpochMillis,
            )
        }

        is AppResult.Error -> emptyList()
    }
}

private suspend fun refreshAllFiles(
    folder: String,
    photoSourceScanner: PhotoSourceScanner,
): List<PlannedMediaFile> {
    return when (
        val result = withContext(Dispatchers.Default) {
            photoSourceScanner.scanFolder(
                path = folder,
                readContentHash = false,
            )
        }
    ) {
        is AppResult.Success -> {
            val mediaFiles = result.data.mediaFiles.map { mediaFile ->
                PlannedMediaFile(
                    sourcePath = mediaFile.path,
                    fileName = mediaFile.fileName,
                    targetRelativePath = mediaFile.path,
                    sizeBytes = mediaFile.sizeBytes,
                    contentHash = mediaFile.contentHash,
                    capturedAtEpochMillis = mediaFile.capturedAtEpochMillis,
                    modifiedAtEpochMillis = mediaFile.modifiedAtEpochMillis,
                )
            }
            val unsupportedFiles = result.data.unsupportedFiles.map { unsupportedFile ->
                PlannedMediaFile(
                    sourcePath = unsupportedFile.path,
                    fileName = unsupportedFile.fileName,
                    targetRelativePath = unsupportedFile.path,
                    sizeBytes = unsupportedFile.sizeBytes,
                    modifiedAtEpochMillis = unsupportedFile.modifiedAtEpochMillis,
                )
            }
            mediaFiles + unsupportedFiles
        }

        is AppResult.Error -> emptyList()
    }
}

private fun List<PlannedMediaFile>.duplicateQuarantineCandidates(): List<PlannedMediaFile> {
    return asSequence()
        .filter { it.contentHash != null }
        .groupBy { it.contentHash }
        .values
        .filter { it.size > 1 }
        .flatMap { files -> files.sortedBy { it.targetRelativePath }.drop(1) }
}

private fun navigationFilesForSection(
    selectedSection: AppSection,
    scanUiState: ScanUiState,
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
    unsupportedFiles: List<PlannedMediaFile>,
): List<PlannedMediaFile> {
    return when (selectedSection) {
        AppSection.Import -> (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty()
        AppSection.AllPhotos,
        AppSection.Years,
        AppSection.Months -> libraryFiles

        AppSection.WithoutDate -> libraryFiles.filter { it.libraryDateGroup() == null }
        AppSection.Duplicates -> buildDuplicateReviewGroups(
            libraryFiles = libraryFiles,
            duplicateFiles = duplicateFiles,
        ).flatMap { group -> group.libraryFiles + group.duplicateFiles }
        AppSection.Unsupported -> unsupportedFiles
        AppSection.Errors,
        AppSection.About -> emptyList()
    }
}

private fun List<PlannedMediaFile>.nextFrom(
    selectedIndex: Int,
    step: Int,
): PlannedMediaFile {
    val nextIndex = (selectedIndex + step).floorMod(size)
    return this[nextIndex]
}

private fun Int.floorMod(size: Int): Int {
    return ((this % size) + size) % size
}
