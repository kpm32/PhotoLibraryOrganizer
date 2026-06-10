package com.anvar.photolibraryorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ImportMediaFilesUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.PreviewImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.PreviewFolderPicker
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
    folderPicker: FolderPicker = PreviewFolderPicker,
) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        var sourceFolder by remember { mutableStateOf<String?>(null) }
        var destinationFolder by remember { mutableStateOf<String?>(null) }
        var importMode by remember { mutableStateOf(ImportMode.ScanOnly) }
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
            importAvailability = resolveImportAvailabilityUseCase(
                importMode = importMode,
                plannedFiles = (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty(),
            ),
            onSourceFolderClick = {
                folderPicker.chooseFolder("Выбери исходную папку")?.let { sourceFolder = it }
                scanUiState = ScanUiState.Idle
                importUiState = ImportUiState.Idle
                selectedFile = null
                libraryFiles = emptyList()
                imagePreviewUiState = ImagePreviewUiState.Empty
            },
            onDestinationFolderClick = {
                folderPicker.chooseFolder("Выбери папку библиотеки")?.let { destinationFolder = it }
                scanUiState = ScanUiState.Idle
                importUiState = ImportUiState.Idle
                selectedFile = null
                libraryFiles = emptyList()
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
            selectedFile = selectedFile,
            imagePreviewUiState = imagePreviewUiState,
            onFileSelected = { selectedFile = it },
        )
    }
}

@Composable
private fun PhotoLibraryOrganizerApp(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importUiState: ImportUiState,
    libraryFiles: List<PlannedMediaFile>,
    importAvailability: ImportAvailability,
    onSourceFolderClick: () -> Unit,
    onDestinationFolderClick: () -> Unit,
    onImportModeSelected: (ImportMode) -> Unit,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    selectedFile: PlannedMediaFile?,
    imagePreviewUiState: ImagePreviewUiState,
    onFileSelected: (PlannedMediaFile) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .safeContentPadding()
                .fillMaxSize()
                .padding(start = 14.dp, top = 16.dp, end = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LibrarySidebar()
            MainWorkspace(
                plan = plan,
                scanUiState = scanUiState,
                importUiState = importUiState,
                libraryFiles = libraryFiles,
                importAvailability = importAvailability,
                selectedFile = selectedFile,
                onScanClick = onScanClick,
                onImportClick = onImportClick,
                onFileSelected = onFileSelected,
                modifier = Modifier.weight(1f),
            )
            InspectorPanel(
                plan = plan,
                scanUiState = scanUiState,
                selectedFile = selectedFile,
                imagePreviewUiState = imagePreviewUiState,
                onSourceFolderClick = onSourceFolderClick,
                onDestinationFolderClick = onDestinationFolderClick,
                selectedMode = plan.importMode,
                onImportModeSelected = onImportModeSelected,
            )
        }
    }
}

@Composable
private fun LibrarySidebar() {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Фотоархив",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SidebarSection(
                title = "Библиотека",
                items = listOf("Все фото", "Годы", "Месяцы", "Без даты"),
            )
            SidebarSection(
                title = "Работа",
                items = listOf("Импорт", "Дубликаты", "Ошибки"),
            )
        }
    }
}

@Composable
private fun SidebarSection(
    title: String,
    items: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        items.forEach { item ->
            Text(
                text = item,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MainWorkspace(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importUiState: ImportUiState,
    libraryFiles: List<PlannedMediaFile>,
    importAvailability: ImportAvailability,
    selectedFile: PlannedMediaFile?,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onFileSelected: (PlannedMediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WorkspaceHeader()
        ScanPreview(
            plan = plan,
            scanUiState = scanUiState,
            importUiState = importUiState,
            importAvailability = importAvailability,
            onScanClick = onScanClick,
            onImportClick = onImportClick,
        )
        MediaList(
            scanUiState = scanUiState,
            libraryFiles = libraryFiles,
            selectedFile = selectedFile,
            onFileSelected = onFileSelected,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WorkspaceHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Импорт и каталог",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Сначала показываем план, потом копируем. Исходники не удаляются.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InspectorPanel(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    selectedFile: PlannedMediaFile?,
    imagePreviewUiState: ImagePreviewUiState,
    onSourceFolderClick: () -> Unit,
    onDestinationFolderClick: () -> Unit,
    selectedMode: ImportMode,
    onImportModeSelected: (ImportMode) -> Unit,
) {
    Surface(
            modifier = Modifier
            .width(300.dp)
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Инспектор",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SelectedFilePreview(
                selectedFile = selectedFile,
                imagePreviewUiState = imagePreviewUiState,
            )
            HorizontalDivider()
            FolderSelector(
                title = "Источник",
                path = plan.sourceFolder,
                actionText = "Выбрать",
                onClick = onSourceFolderClick,
            )
            FolderSelector(
                title = "Библиотека",
                path = plan.destinationFolder,
                actionText = "Выбрать",
                onClick = onDestinationFolderClick,
            )
            ImportModeSelector(
                selectedMode = selectedMode,
                onImportModeSelected = onImportModeSelected,
            )
            if (scanUiState is ScanUiState.Success) {
                HorizontalDivider()
                ScanSummaryRows(scanUiState)
            }
        }
    }
}

@Composable
private fun FolderSelector(
    title: String,
    path: String?,
    actionText: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = path ?: "Папка пока не выбрана",
            style = MaterialTheme.typography.bodySmall,
            color = if (path == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
        )
        OutlinedButton(onClick = onClick) {
            Text(actionText)
        }
    }
}

@Composable
private fun ImportModeSelector(
    selectedMode: ImportMode,
    onImportModeSelected: (ImportMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Режим",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ImportMode.entries.forEach { mode ->
                ModeOption(
                    mode = mode,
                    selected = mode == selectedMode,
                    onClick = { onImportModeSelected(mode) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ModeOption(
    mode: ImportMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun ScanPreview(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importUiState: ImportUiState,
    importAvailability: ImportAvailability,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Панель импорта",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            Text(
                text = scanStatusText(plan, scanUiState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (scanUiState is ScanUiState.Success) {
                Text(
                    text = "Пока используем дату изменения файла. Дату съемки из EXIF подключим отдельным шагом.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ImportAvailabilityHint(importAvailability)
            }
            ImportStatus(importUiState)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = plan.canScan && scanUiState !is ScanUiState.Loading,
                ) {
                    Text(if (scanUiState is ScanUiState.Loading) "Сканирую..." else "Сканировать")
                }
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importAvailability is ImportAvailability.Available &&
                        importUiState !is ImportUiState.Loading,
                ) {
                    Text(if (importUiState is ImportUiState.Loading) "Импортирую..." else "Импорт")
                }
            }
        }
    }
}

@Composable
private fun MediaList(
    scanUiState: ScanUiState,
    libraryFiles: List<PlannedMediaFile>,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (libraryFiles.isEmpty()) "Файлы к импорту" else "Библиотека",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            val files = libraryFiles.ifEmpty {
                (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty()
            }

            when {
                libraryFiles.isNotEmpty() -> MediaRows(
                    files = files,
                    selectedFile = selectedFile,
                    onFileSelected = onFileSelected,
                )
                scanUiState == ScanUiState.Idle -> EmptyListText("После сканирования здесь появится список найденных фото.")
                scanUiState == ScanUiState.Loading -> EmptyListText("Сканирую папку...")
                scanUiState is ScanUiState.Error -> EmptyListText(scanUiState.message)
                files.isEmpty() -> EmptyListText("Медиафайлы не найдены.")
                else -> MediaRows(
                    files = files,
                    selectedFile = selectedFile,
                    onFileSelected = onFileSelected,
                )
            }
        }
    }
}

@Composable
private fun MediaRows(
    files: List<PlannedMediaFile>,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(files) { plannedFile ->
            MediaListRow(
                plannedFile = plannedFile,
                selected = plannedFile == selectedFile,
                onClick = { onFileSelected(plannedFile) },
            )
        }
    }
}

@Composable
private fun EmptyListText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MediaListRow(
    plannedFile: PlannedMediaFile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text("Фото", style = MaterialTheme.typography.labelSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = plannedFile.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = plannedFile.targetRelativePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SelectedFilePreview(
    selectedFile: PlannedMediaFile?,
    imagePreviewUiState: ImagePreviewUiState,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            when (imagePreviewUiState) {
                ImagePreviewUiState.Empty -> PreviewPlaceholder("Фото не выбрано")
                ImagePreviewUiState.Loading -> PreviewPlaceholder("Загружаю превью...")
                ImagePreviewUiState.Unsupported -> PreviewPlaceholder("Превью пока недоступно")
                is ImagePreviewUiState.Success -> {
                    Image(
                        bitmap = imagePreviewUiState.image,
                        contentDescription = selectedFile?.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
        }
        if (selectedFile != null) {
            SummaryRow("Файл", selectedFile.fileName)
            SummaryRow("Размер", selectedFile.sizeBytes.toReadableSize())
            Text(
                text = selectedFile.targetRelativePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreviewPlaceholder(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ImportStatus(importUiState: ImportUiState) {
    val text = when (importUiState) {
        ImportUiState.Idle -> return
        ImportUiState.Loading -> "Копирую файлы в библиотеку. Исходники не удаляются."
        is ImportUiState.Success -> {
            "Импорт завершен: скопировано ${importUiState.result.copiedFiles}, пропущено ${importUiState.result.skippedFiles}, ошибок ${importUiState.result.failedFiles}."
        }
        is ImportUiState.Error -> importUiState.message
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ImportAvailabilityHint(importAvailability: ImportAvailability) {
    if (importAvailability is ImportAvailability.Unavailable) {
        Text(
            text = importAvailability.reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlannedFilesPreview(plannedFiles: List<PlannedMediaFile>) {
    if (plannedFiles.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider()
        Text(
            text = "Первые файлы и будущие пути",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        plannedFiles.take(5).forEach { plannedFile ->
            PlannedFileRow(plannedFile)
        }
        if (plannedFiles.size > 5) {
            Text(
                text = "И еще ${plannedFiles.size - 5} файлов.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlannedFileRow(plannedFile: PlannedMediaFile) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = plannedFile.fileName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = plannedFile.targetRelativePath,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
private fun ScanSummaryRows(scanUiState: ScanUiState.Success) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SummaryRow("Всего файлов", scanUiState.summary.scannedFiles.toString())
        SummaryRow("Медиа", scanUiState.summary.mediaFiles.toString())
        SummaryRow("Фото", scanUiState.summary.imageFiles.toString())
        SummaryRow("Видео", scanUiState.summary.videoFiles.toString())
        SummaryRow("Неподдерживаемые", scanUiState.summary.unsupportedFiles.toString())
        SummaryRow("Размер медиа", scanUiState.summary.totalMediaBytes.toReadableSize())
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun scanStatusText(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
): String {
    return when (scanUiState) {
        ScanUiState.Idle -> if (plan.canScan) {
            "Готово к безопасному сканированию. На этом шаге приложение еще не будет копировать, переносить или удалять файлы."
        } else {
            "Выбери исходную папку и папку библиотеки, чтобы подготовить сканирование."
        }

        ScanUiState.Loading -> "Сканирую папку и подпапки. Файлы не изменяются."
        is ScanUiState.Success -> "Сканирование завершено. Это только статистика, импорт пока не запускался."
        is ScanUiState.Error -> scanUiState.message
    }
}

private fun PhotoLibraryError.toUserMessage(): String {
    return when (this) {
        PhotoLibraryError.InvalidSourceFolder -> "Исходная папка не выбрана."
        is PhotoLibraryError.SourceFolderNotFound -> "Исходная папка не найдена: $path"
        is PhotoLibraryError.SourceFolderIsNotDirectory -> "Выбранный путь не является папкой: $path"
        PhotoLibraryError.ImportPlanIsEmpty -> "Нет плана импорта. Сначала выполни сканирование."
        PhotoLibraryError.UnsupportedImportMode -> "Этот режим импорта пока не поддерживается."
        is PhotoLibraryError.FileSystem -> "Не удалось просканировать папку: $message"
        is PhotoLibraryError.Unknown -> "Неизвестная ошибка сканирования: ${message ?: "без деталей"}"
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

private fun Long.toReadableSize(): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = toDouble()
    var unitIndex = 0

    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024
        unitIndex += 1
    }

    return if (unitIndex == 0) {
        "${value.toLong()} ${units[unitIndex]}"
    } else {
        "${(value * 10).toLong() / 10.0} ${units[unitIndex]}"
    }
}
