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
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.DuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.ImportPlanTargetResolver
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ImportMediaFilesUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import com.anvar.photolibraryorganizer.presentation.AppSettings
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.CachingImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.PreviewDuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.presentation.PreviewAppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.PreviewFolderPicker
import com.anvar.photolibraryorganizer.presentation.PreviewImportPlanTargetResolver
import com.anvar.photolibraryorganizer.presentation.PreviewImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.PreviewMediaFileImporter
import com.anvar.photolibraryorganizer.presentation.PreviewPhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.ScanUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@Preview
fun App(
    photoSourceScanner: PhotoSourceScanner = PreviewPhotoSourceScanner,
    mediaFileImporter: MediaFileImporter = PreviewMediaFileImporter,
    importPlanTargetResolver: ImportPlanTargetResolver = PreviewImportPlanTargetResolver,
    duplicateQuarantineRepository: DuplicateQuarantineRepository = PreviewDuplicateQuarantineRepository,
    imagePreviewLoader: ImagePreviewLoader = PreviewImagePreviewLoader,
    appSettingsStorage: AppSettingsStorage = PreviewAppSettingsStorage,
    folderPicker: FolderPicker = PreviewFolderPicker,
) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        var sourceFolder by remember { mutableStateOf<String?>(null) }
        var destinationFolder by remember { mutableStateOf<String?>(null) }
        var importMode by remember { mutableStateOf(ImportMode.ScanOnly) }
        var selectedSection by remember { mutableStateOf(AppSection.AllPhotos) }
        var scanUiState by remember { mutableStateOf<ScanUiState>(ScanUiState.Idle) }
        var importUiState by remember { mutableStateOf<ImportUiState>(ImportUiState.Idle) }
        var selectedFile by remember { mutableStateOf<PlannedMediaFile?>(null) }
        var libraryFiles by remember { mutableStateOf<List<PlannedMediaFile>>(emptyList()) }
        var duplicateActionMessage by remember { mutableStateOf<String?>(null) }
        var imagePreviewUiState by remember { mutableStateOf<ImagePreviewUiState>(ImagePreviewUiState.Empty) }

        val coroutineScope = rememberCoroutineScope()
        val scanSourceFolderUseCase = remember(photoSourceScanner) {
            ScanSourceFolderUseCase(photoSourceScanner)
        }
        val importMediaFilesUseCase = remember(mediaFileImporter) {
            ImportMediaFilesUseCase(mediaFileImporter)
        }
        val buildMediaFilePlanUseCase = remember { BuildMediaFilePlanUseCase() }
        val resolveImportAvailabilityUseCase = remember { ResolveImportAvailabilityUseCase() }
        val cachedImagePreviewLoader = remember(imagePreviewLoader) {
            CachingImagePreviewLoader(imagePreviewLoader)
        }

        LaunchedEffect(Unit) {
            val settings = appSettingsStorage.loadSettings()
            sourceFolder = settings.sourceFolder
            destinationFolder = settings.destinationFolder
            if (!settings.destinationFolder.isNullOrBlank()) {
                libraryFiles = refreshLibraryFiles(
                    destinationFolder = settings.destinationFolder,
                    photoSourceScanner = photoSourceScanner,
                )
                selectedFile = libraryFiles.firstOrNull()
            }
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
        )

        PhotoLibraryOrganizerApp(
            plan = plan,
            scanUiState = scanUiState,
            importUiState = importUiState,
            libraryFiles = libraryFiles,
            selectedSection = selectedSection,
            imagePreviewLoader = cachedImagePreviewLoader,
            duplicateActionMessage = duplicateActionMessage,
            importAvailability = resolveImportAvailabilityUseCase(
                importMode = importMode,
                plannedFiles = (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty(),
            ),
            onSourceFolderClick = {
                folderPicker.chooseFolder("Выбери исходную папку")?.let {
                    sourceFolder = it
                    coroutineScope.launch {
                        appSettingsStorage.saveSettings(
                            AppSettings(
                                sourceFolder = sourceFolder,
                                destinationFolder = destinationFolder,
                            ),
                        )
                    }
                }
                scanUiState = ScanUiState.Idle
                importUiState = ImportUiState.Idle
                duplicateActionMessage = null
                selectedFile = null
                libraryFiles = emptyList()
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
                            ),
                        )
                    }
                }
                scanUiState = ScanUiState.Idle
                importUiState = ImportUiState.Idle
                duplicateActionMessage = null
                selectedFile = null
                coroutineScope.launch {
                    libraryFiles = refreshLibraryFiles(
                        destinationFolder = destinationFolder,
                        photoSourceScanner = photoSourceScanner,
                    )
                    selectedFile = libraryFiles.firstOrNull()
                }
                imagePreviewUiState = ImagePreviewUiState.Empty
            },
            onImportModeSelected = {
                importMode = it
                importUiState = ImportUiState.Idle
            },
            onScanClick = {
                coroutineScope.launch {
                    scanUiState = ScanUiState.Loading
                    importUiState = ImportUiState.Idle
                    duplicateActionMessage = null
                    libraryFiles = emptyList()
                    scanUiState = try {
                        when (val result = withContext(Dispatchers.Default) { scanSourceFolderUseCase(sourceFolder) }) {
                            is AppResult.Success -> {
                                val plannedFiles = buildMediaFilePlanUseCase(
                                    destinationFolder = destinationFolder,
                                    mediaFiles = result.data.mediaFiles,
                                )
                                val resolvedFiles = importPlanTargetResolver.resolve(plannedFiles)
                                ScanUiState.Success(
                                    summary = result.data.summary,
                                    plannedFiles = resolvedFiles,
                                ).also { selectedFile = it.plannedFiles.firstOrNull() }
                            }

                            is AppResult.Error -> ScanUiState.Error(result.error.toUserMessage())
                        }
                    } catch (exception: Throwable) {
                        ScanUiState.Error("Сканирование прервалось: ${exception.message ?: "без деталей"}")
                    }
                }
            },
            onImportClick = {
                val plannedFiles = (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty()
                importUiState = ImportUiState.AwaitingConfirmation(
                    readyFileCount = plannedFiles.count { it.targetStatus != ImportTargetStatus.AlreadyExists },
                    existingFileCount = plannedFiles.count { it.targetStatus == ImportTargetStatus.AlreadyExists },
                )
            },
            onCancelImportClick = {
                importUiState = ImportUiState.Idle
            },
            onConfirmImportClick = {
                coroutineScope.launch {
                    val plannedFiles = (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty()
                    importUiState = ImportUiState.Loading
                    importUiState = try {
                        when (
                            val result = withContext(Dispatchers.Default) {
                                importMediaFilesUseCase(importMode, plannedFiles)
                            }
                        ) {
                            is AppResult.Success -> {
                                libraryFiles = refreshLibraryFiles(
                                    destinationFolder = destinationFolder,
                                    photoSourceScanner = photoSourceScanner,
                                )
                                selectedFile = libraryFiles.firstOrNull() ?: selectedFile
                                ImportUiState.Success(result.data)
                            }

                            is AppResult.Error -> ImportUiState.Error(result.error.toUserMessage())
                        }
                    } catch (exception: Throwable) {
                        ImportUiState.Error("Импорт прервался: ${exception.message ?: "без деталей"}")
                    }
                }
            },
            onRefreshLibraryClick = {
                coroutineScope.launch {
                    libraryFiles = refreshLibraryFiles(
                        destinationFolder = destinationFolder,
                        photoSourceScanner = photoSourceScanner,
                    )
                    selectedFile = libraryFiles.firstOrNull()
                }
            },
            onMoveDuplicatesClick = {
                coroutineScope.launch {
                    val duplicatesToMove = libraryFiles.duplicateQuarantineCandidates()
                    if (duplicatesToMove.isEmpty()) {
                        duplicateActionMessage = "Дубликаты для переноса не найдены."
                        return@launch
                    }

                    duplicateActionMessage = "Переношу дубликаты в папку Duplicates..."
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
                                libraryFiles = refreshLibraryFiles(
                                    destinationFolder = destinationFolder,
                                    photoSourceScanner = photoSourceScanner,
                                )
                                selectedFile = libraryFiles.firstOrNull()
                                "Перенесено в Duplicates: ${result.data.movedFiles}, ошибок: ${result.data.failedFiles}."
                            }

                            is AppResult.Error -> result.error.toUserMessage()
                        }
                    } catch (exception: Throwable) {
                        "Не удалось перенести дубликаты: ${exception.message ?: "без деталей"}"
                    }
                }
            },
            onSectionSelected = { selectedSection = it },
            selectedFile = selectedFile,
            imagePreviewUiState = imagePreviewUiState,
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

private suspend fun refreshLibraryFiles(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
): List<PlannedMediaFile> {
    val libraryFolder = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/Library" }
        ?: return emptyList()

    return when (val result = photoSourceScanner.scanFolder(libraryFolder)) {
        is AppResult.Success -> result.data.mediaFiles.map { mediaFile ->
            PlannedMediaFile(
                sourcePath = mediaFile.path,
                fileName = mediaFile.fileName,
                targetRelativePath = mediaFile.path,
                sizeBytes = mediaFile.sizeBytes,
                contentHash = mediaFile.contentHash,
            )
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
