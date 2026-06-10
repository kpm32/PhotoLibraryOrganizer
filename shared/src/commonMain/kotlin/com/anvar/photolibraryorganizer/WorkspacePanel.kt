package com.anvar.photolibraryorganizer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ScanUiState

@Composable
internal fun MainWorkspace(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importUiState: ImportUiState,
    libraryFiles: List<PlannedMediaFile>,
    selectedSection: AppSection,
    imagePreviewLoader: ImagePreviewLoader,
    importAvailability: ImportAvailability,
    selectedFile: PlannedMediaFile?,
    selectedMode: ImportMode,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onImportModeSelected: (ImportMode) -> Unit,
    onFileSelected: (PlannedMediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WorkspaceHeader(selectedSection)
        if (selectedSection == AppSection.Import) {
            ScanPreview(
                plan = plan,
                scanUiState = scanUiState,
                importUiState = importUiState,
                importAvailability = importAvailability,
                selectedMode = selectedMode,
                onScanClick = onScanClick,
                onImportClick = onImportClick,
                onImportModeSelected = onImportModeSelected,
            )
            MediaList(
                title = "Файлы к импорту",
                emptyText = "После сканирования здесь появится список найденных фото.",
                scanUiState = scanUiState,
                files = (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty(),
                imagePreviewLoader = imagePreviewLoader,
                selectedFile = selectedFile,
                onFileSelected = onFileSelected,
                modifier = Modifier.weight(1f),
            )
        } else {
            LibrarySection(
                selectedSection = selectedSection,
                libraryFiles = libraryFiles,
                imagePreviewLoader = imagePreviewLoader,
                selectedFile = selectedFile,
                onFileSelected = onFileSelected,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun WorkspaceHeader(selectedSection: AppSection) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = selectedSection.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = when (selectedSection) {
                AppSection.Import -> "Сначала показываем план, потом копируем. Исходники не удаляются."
                AppSection.AllPhotos -> "Просмотр уже разложенной библиотеки."
                AppSection.Years -> "Библиотека, сгруппированная по годам."
                AppSection.Months -> "Библиотека, сгруппированная по месяцам."
                AppSection.WithoutDate -> "Файлы, для которых пока не удалось определить дату."
                AppSection.Duplicates -> "Поиск дубликатов будет отдельным безопасным сценарием."
                AppSection.Errors -> "Ошибки импорта и сканирования будут собираться здесь."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScanPreview(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importUiState: ImportUiState,
    importAvailability: ImportAvailability,
    selectedMode: ImportMode,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onImportModeSelected: (ImportMode) -> Unit,
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
            ImportModeChips(
                selectedMode = selectedMode,
                onImportModeSelected = onImportModeSelected,
            )
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
private fun ImportModeChips(
    selectedMode: ImportMode,
    onImportModeSelected: (ImportMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ImportMode.entries.forEach { mode ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onImportModeSelected(mode) },
                shape = MaterialTheme.shapes.small,
                color = if (mode == selectedMode) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Text(
                    text = mode.title,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
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
private fun LibrarySection(
    selectedSection: AppSection,
    libraryFiles: List<PlannedMediaFile>,
    imagePreviewLoader: ImagePreviewLoader,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (selectedSection) {
        AppSection.AllPhotos -> MediaList(
            title = "Библиотека",
            emptyText = "Выбери папку библиотеки или нажми «Обновить библиотеку».",
            scanUiState = ScanUiState.Idle,
            files = libraryFiles,
            imagePreviewLoader = imagePreviewLoader,
            selectedFile = selectedFile,
            onFileSelected = onFileSelected,
            modifier = modifier,
        )

        AppSection.Years -> GroupedMediaList(
            title = "Годы",
            emptyText = "В библиотеке пока нет файлов с годом.",
            groups = libraryFiles
                .groupBy { it.libraryDateGroup()?.year ?: "Без даты" }
                .toSortedMap(compareByDescending { it }),
            imagePreviewLoader = imagePreviewLoader,
            selectedFile = selectedFile,
            onFileSelected = onFileSelected,
            modifier = modifier,
        )

        AppSection.Months -> GroupedMediaList(
            title = "Месяцы",
            emptyText = "В библиотеке пока нет файлов с месяцем.",
            groups = libraryFiles
                .groupBy { it.libraryDateGroup()?.month ?: "Без даты" }
                .toSortedMap(compareByDescending { it }),
            imagePreviewLoader = imagePreviewLoader,
            selectedFile = selectedFile,
            onFileSelected = onFileSelected,
            modifier = modifier,
        )

        AppSection.WithoutDate -> MediaList(
            title = "Без даты",
            emptyText = "Файлов без даты пока нет.",
            scanUiState = ScanUiState.Idle,
            files = libraryFiles.filter { it.libraryDateGroup() == null },
            imagePreviewLoader = imagePreviewLoader,
            selectedFile = selectedFile,
            onFileSelected = onFileSelected,
            modifier = modifier,
        )

        AppSection.Duplicates,
        AppSection.Errors,
        AppSection.Import,
        -> PlaceholderPanel(
            title = selectedSection.title,
            text = "Этот раздел уже есть в навигации, но его логика будет добавлена отдельным шагом.",
            modifier = modifier,
        )
    }
}

@Composable
private fun PlaceholderPanel(
    title: String,
    text: String,
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            EmptyListText(text)
        }
    }
}

@Composable
private fun MediaList(
    title: String,
    emptyText: String,
    scanUiState: ScanUiState,
    files: List<PlannedMediaFile>,
    imagePreviewLoader: ImagePreviewLoader,
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            when {
                files.isNotEmpty() -> MediaRows(
                    files = files,
                    imagePreviewLoader = imagePreviewLoader,
                    selectedFile = selectedFile,
                    onFileSelected = onFileSelected,
                )
                scanUiState == ScanUiState.Idle -> EmptyListText(emptyText)
                scanUiState == ScanUiState.Loading -> EmptyListText("Сканирую папку...")
                scanUiState is ScanUiState.Error -> EmptyListText(scanUiState.message)
                files.isEmpty() -> EmptyListText("Медиафайлы не найдены.")
                else -> MediaRows(
                    files = files,
                    imagePreviewLoader = imagePreviewLoader,
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
    imagePreviewLoader: ImagePreviewLoader,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(files, key = { it.sourcePath }) { plannedFile ->
            MediaListRow(
                plannedFile = plannedFile,
                imagePreviewLoader = imagePreviewLoader,
                selected = plannedFile == selectedFile,
                onClick = { onFileSelected(plannedFile) },
            )
        }
    }
}

@Composable
private fun GroupedMediaList(
    title: String,
    emptyText: String,
    groups: Map<String, List<PlannedMediaFile>>,
    imagePreviewLoader: ImagePreviewLoader,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupTitles = groups.keys.toList()
    var selectedGroup by remember(title, groupTitles) { mutableStateOf(groupTitles.firstOrNull()) }
    val activeGroup = selectedGroup?.takeIf { groups.containsKey(it) } ?: groupTitles.firstOrNull()
    val activeFiles = activeGroup?.let { groups.getValue(it) }.orEmpty()

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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            if (groups.isEmpty()) {
                EmptyListText(emptyText)
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GroupSelector(
                        groups = groups,
                        selectedGroup = activeGroup,
                        onGroupSelected = { selectedGroup = it },
                    )
                    MediaRows(
                        files = activeFiles,
                        imagePreviewLoader = imagePreviewLoader,
                        selectedFile = selectedFile,
                        onFileSelected = onFileSelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupSelector(
    groups: Map<String, List<PlannedMediaFile>>,
    selectedGroup: String?,
    onGroupSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .width(150.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(groups.entries.toList(), key = { it.key }) { group ->
            val selected = group.key == selectedGroup
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroupSelected(group.key) },
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = group.key,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Text(
                        text = group.value.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
    imagePreviewLoader: ImagePreviewLoader,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val thumbnail by produceState<ImageBitmap?>(initialValue = null, plannedFile.sourcePath) {
        value = imagePreviewLoader.loadImage(plannedFile.sourcePath)
    }

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
                    .width(52.dp)
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                thumbnail?.let { image ->
                    Image(
                        bitmap = image,
                        contentDescription = plannedFile.fileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } ?: run {
                    Text("Фото", style = MaterialTheme.typography.labelSmall)
                }
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

private data class LibraryDateGroup(
    val year: String,
    val month: String,
)

private fun PlannedMediaFile.libraryDateGroup(): LibraryDateGroup? {
    val pathParts = targetRelativePath.replace('\\', '/').split('/')
    val libraryIndex = pathParts.indexOfLast { it == "Library" }
    val year = pathParts.getOrNull(libraryIndex + 1)
    val month = pathParts.getOrNull(libraryIndex + 2)

    return if (
        year != null &&
        month != null &&
        year.length == 4 &&
        month.length == 7 &&
        month.startsWith("$year-")
    ) {
        LibraryDateGroup(year = year, month = month)
    } else {
        null
    }
}
