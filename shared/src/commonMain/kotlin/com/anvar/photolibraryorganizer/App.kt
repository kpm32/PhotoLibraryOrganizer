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
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ImportMediaFilesUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import com.anvar.photolibraryorganizer.presentation.AppSettings
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.PreviewAppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.PreviewFolderPicker
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
                imagePreviewLoader.loadImage(file.sourcePath)?.let { image ->
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
            imagePreviewLoader = imagePreviewLoader,
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
                    libraryFiles = emptyList()
                    scanUiState = try {
                        when (val result = withContext(Dispatchers.Default) { scanSourceFolderUseCase(sourceFolder) }) {
                            is AppResult.Success -> ScanUiState.Success(
                                summary = result.data.summary,
                                plannedFiles = buildMediaFilePlanUseCase(
                                    destinationFolder = destinationFolder,
                                    mediaFiles = result.data.mediaFiles,
                                ),
                            ).also { selectedFile = it.plannedFiles.firstOrNull() }

                            is AppResult.Error -> ScanUiState.Error(result.error.toUserMessage())
                        }
                    } catch (exception: Throwable) {
                        ScanUiState.Error("Сканирование прервалось: ${exception.message ?: "без деталей"}")
                    }
                }
            },
            onImportClick = {
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
            )
        }

        is AppResult.Error -> emptyList()
    }
}
