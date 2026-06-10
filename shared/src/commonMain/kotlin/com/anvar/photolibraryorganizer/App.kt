package com.anvar.photolibraryorganizer

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import com.anvar.photolibraryorganizer.presentation.PreviewFolderPicker
import com.anvar.photolibraryorganizer.presentation.PreviewPhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.ScanUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@Preview
fun App(
    photoSourceScanner: PhotoSourceScanner = PreviewPhotoSourceScanner,
    folderPicker: FolderPicker = PreviewFolderPicker,
) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        var sourceFolder by remember { mutableStateOf<String?>(null) }
        var destinationFolder by remember { mutableStateOf<String?>(null) }
        var importMode by remember { mutableStateOf(ImportMode.ScanOnly) }
        var scanUiState by remember { mutableStateOf<ScanUiState>(ScanUiState.Idle) }
        val coroutineScope = rememberCoroutineScope()
        val scanSourceFolderUseCase = remember(photoSourceScanner) {
            ScanSourceFolderUseCase(photoSourceScanner)
        }
        val buildMediaFilePlanUseCase = remember { BuildMediaFilePlanUseCase() }
        val resolveImportAvailabilityUseCase = remember { ResolveImportAvailabilityUseCase() }

        val plan = PhotoLibraryPlan(
            sourceFolder = sourceFolder,
            destinationFolder = destinationFolder,
            importMode = importMode,
        )

        PhotoLibraryOrganizerApp(
            plan = plan,
            scanUiState = scanUiState,
            importAvailability = resolveImportAvailabilityUseCase(
                importMode = importMode,
                plannedFiles = (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty(),
            ),
            onSourceFolderClick = {
                folderPicker.chooseFolder("Выбери исходную папку")?.let { sourceFolder = it }
                scanUiState = ScanUiState.Idle
            },
            onDestinationFolderClick = {
                folderPicker.chooseFolder("Выбери папку библиотеки")?.let { destinationFolder = it }
                scanUiState = ScanUiState.Idle
            },
            onImportModeSelected = { importMode = it },
            onScanClick = {
                coroutineScope.launch {
                    scanUiState = ScanUiState.Loading
                    scanUiState = try {
                        when (val result = withContext(Dispatchers.Default) { scanSourceFolderUseCase(sourceFolder) }) {
                            is AppResult.Success -> ScanUiState.Success(
                                summary = result.data.summary,
                                plannedFiles = buildMediaFilePlanUseCase(
                                    destinationFolder = destinationFolder,
                                    mediaFiles = result.data.mediaFiles,
                                ),
                            )
                            is AppResult.Error -> ScanUiState.Error(result.error.toUserMessage())
                        }
                    } catch (exception: Throwable) {
                        ScanUiState.Error("Сканирование прервалось: ${exception.message ?: "без деталей"}")
                    }
                }
            },
        )
    }
}

@Composable
private fun PhotoLibraryOrganizerApp(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importAvailability: ImportAvailability,
    onSourceFolderClick: () -> Unit,
    onDestinationFolderClick: () -> Unit,
    onImportModeSelected: (ImportMode) -> Unit,
    onScanClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .safeContentPadding()
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 32.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FolderSelector(
                    title = "Исходная папка",
                    description = "Большая папка с подпапками, которую надо разобрать.",
                    path = plan.sourceFolder,
                    actionText = "Выбрать источник",
                    onClick = onSourceFolderClick,
                    modifier = Modifier.weight(1f),
                )
                FolderSelector(
                    title = "Папка библиотеки",
                    description = "Новая или существующая папка, куда ляжет порядок.",
                    path = plan.destinationFolder,
                    actionText = "Выбрать назначение",
                    onClick = onDestinationFolderClick,
                    modifier = Modifier.weight(1f),
                )
            }

            ScanPreview(
                plan = plan,
                scanUiState = scanUiState,
                importAvailability = importAvailability,
                onScanClick = onScanClick,
            )

            ImportModeSelector(
                selectedMode = plan.importMode,
                onImportModeSelected = onImportModeSelected,
            )
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Photo Library Organizer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Сначала сканируем архив без изменений, потом копируем или переносим только после подтверждения.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FolderSelector(
    title: String,
    description: String,
    path: String?,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.defaultMinSize(minHeight = 172.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = path ?: "Папка пока не выбрана",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (path == null) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                )
            }
            OutlinedButton(onClick = onClick) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun ImportModeSelector(
    selectedMode: ImportMode,
    onImportModeSelected: (ImportMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Режим обработки",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ImportMode.entries.forEach { mode ->
                ModeOption(
                    mode = mode,
                    selected = mode == selectedMode,
                    onClick = { onImportModeSelected(mode) },
                    modifier = Modifier.weight(1f),
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
            modifier = Modifier.padding(14.dp),
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
                )
            }
        }
    }
}

@Composable
private fun ScanPreview(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importAvailability: ImportAvailability,
    onScanClick: () -> Unit,
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
                text = "Предпросмотр сканирования",
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
                ScanSummaryRows(scanUiState)
                Text(
                    text = "Пока используем дату изменения файла. Дату съемки из EXIF подключим отдельным шагом.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PlannedFilesPreview(scanUiState.plannedFiles)
                ImportAvailabilityHint(importAvailability)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onScanClick,
                    enabled = plan.canScan && scanUiState !is ScanUiState.Loading,
                ) {
                    Text(if (scanUiState is ScanUiState.Loading) "Сканирую..." else "Сканировать")
                }
                OutlinedButton(
                    onClick = {},
                    enabled = importAvailability is ImportAvailability.Available,
                ) {
                    Text("Импорт")
                }
            }
        }
    }
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
        is PhotoLibraryError.FileSystem -> "Не удалось просканировать папку: $message"
        is PhotoLibraryError.Unknown -> "Неизвестная ошибка сканирования: ${message ?: "без деталей"}"
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
