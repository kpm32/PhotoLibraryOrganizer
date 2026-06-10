package com.anvar.photolibraryorganizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
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
    duplicateFiles: List<PlannedMediaFile>,
    selectedSection: AppSection,
    imagePreviewLoader: ImagePreviewLoader,
    duplicateActionMessage: String?,
    duplicateDeleteAwaitingConfirmation: Boolean,
    importAvailability: ImportAvailability,
    selectedFile: PlannedMediaFile?,
    selectedMode: ImportMode,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImportClick: () -> Unit,
    onCancelImportClick: () -> Unit,
    onMoveDuplicatesClick: () -> Unit,
    onRequestDeleteQuarantineClick: () -> Unit,
    onConfirmDeleteQuarantineClick: () -> Unit,
    onCancelDeleteQuarantineClick: () -> Unit,
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
                onConfirmImportClick = onConfirmImportClick,
                onCancelImportClick = onCancelImportClick,
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
                duplicateFiles = duplicateFiles,
                imagePreviewLoader = imagePreviewLoader,
                duplicateActionMessage = duplicateActionMessage,
                duplicateDeleteAwaitingConfirmation = duplicateDeleteAwaitingConfirmation,
                selectedFile = selectedFile,
                onFileSelected = onFileSelected,
                onMoveDuplicatesClick = onMoveDuplicatesClick,
                onRequestDeleteQuarantineClick = onRequestDeleteQuarantineClick,
                onConfirmDeleteQuarantineClick = onConfirmDeleteQuarantineClick,
                onCancelDeleteQuarantineClick = onCancelDeleteQuarantineClick,
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
                AppSection.Duplicates -> "Файлы с одинаковым SHA-256 хэшем."
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
    onConfirmImportClick: () -> Unit,
    onCancelImportClick: () -> Unit,
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
                    text = "Если в JPEG есть EXIF-дата съемки, используем ее. Для остальных файлов берем дату изменения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ImportAvailabilityHint(importAvailability)
            }
            ImportStatus(importUiState)
            ImportConfirmation(
                importUiState = importUiState,
                selectedMode = selectedMode,
                onConfirmImportClick = onConfirmImportClick,
                onCancelImportClick = onCancelImportClick,
            )
            ImportRulesSummary(plan.importRules)
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
                        importUiState !is ImportUiState.AwaitingConfirmation &&
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
        is ImportUiState.AwaitingConfirmation -> return
        ImportUiState.Loading -> "Выполняю импорт по выбранному режиму."
        is ImportUiState.Success -> {
            "Импорт завершен: скопировано ${importUiState.result.copiedFiles}, перенесено ${importUiState.result.movedFiles}, пропущено ${importUiState.result.skippedFiles}, ошибок ${importUiState.result.failedFiles}."
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
private fun ImportConfirmation(
    importUiState: ImportUiState,
    selectedMode: ImportMode,
    onConfirmImportClick: () -> Unit,
    onCancelImportClick: () -> Unit,
) {
    if (importUiState !is ImportUiState.AwaitingConfirmation) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = importConfirmationText(
                    importUiState = importUiState,
                    selectedMode = selectedMode,
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancelImportClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Отмена")
                }
                Button(
                    onClick = onConfirmImportClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Начать импорт")
                }
            }
        }
    }
}

@Composable
private fun ImportRulesSummary(importRules: ImportOrganizationRules) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Правила импорта",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            CompactRuleRow("Папка", "${importRules.libraryFolderName}/${importRules.folderTemplate}")
            CompactRuleRow("Имя", importRules.fileNameTemplate)
        }
    }
}

@Composable
private fun CompactRuleRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

private fun importConfirmationText(
    importUiState: ImportUiState.AwaitingConfirmation,
    selectedMode: ImportMode,
): String {
    val action = when (selectedMode) {
        ImportMode.Copy -> "Будет скопировано"
        ImportMode.Move -> "Будет перенесено"
        ImportMode.ScanOnly -> "Будет обработано"
    }
    val sourceNote = when (selectedMode) {
        ImportMode.Copy -> "Исходники останутся на месте."
        ImportMode.Move -> "Исходники исчезнут из старой папки после успешного переноса."
        ImportMode.ScanOnly -> "Файлы не изменяются."
    }
    return "$action: ${importUiState.readyFileCount}. Уже есть: ${importUiState.existingFileCount}. $sourceNote"
}

@Composable
private fun LibrarySection(
    selectedSection: AppSection,
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
    imagePreviewLoader: ImagePreviewLoader,
    duplicateActionMessage: String?,
    duplicateDeleteAwaitingConfirmation: Boolean,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    onMoveDuplicatesClick: () -> Unit,
    onRequestDeleteQuarantineClick: () -> Unit,
    onConfirmDeleteQuarantineClick: () -> Unit,
    onCancelDeleteQuarantineClick: () -> Unit,
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

        AppSection.Duplicates -> DuplicateReviewPanel(
            title = if (duplicateFiles.isEmpty()) "Дубликаты" else "Карантин Duplicates",
            emptyText = "Дубликаты и файлы в карантине пока не найдены.",
            groups = buildDuplicateReviewGroups(
                libraryFiles = libraryFiles,
                duplicateFiles = duplicateFiles,
            ),
            quarantineMode = duplicateFiles.isNotEmpty(),
            imagePreviewLoader = imagePreviewLoader,
            selectedFile = selectedFile,
            onFileSelected = onFileSelected,
            actionText = duplicateActionText(
                libraryFiles = libraryFiles,
                duplicateFiles = duplicateFiles,
                duplicateDeleteAwaitingConfirmation = duplicateDeleteAwaitingConfirmation,
            ),
            secondaryActionText = if (duplicateDeleteAwaitingConfirmation && duplicateFiles.isNotEmpty()) {
                "Отмена"
            } else {
                null
            },
            actionMessage = duplicateActionMessage,
            onActionClick = when {
                duplicateDeleteAwaitingConfirmation && duplicateFiles.isNotEmpty() -> onConfirmDeleteQuarantineClick
                duplicateFiles.isNotEmpty() -> onRequestDeleteQuarantineClick
                else -> onMoveDuplicatesClick
            },
            onSecondaryActionClick = onCancelDeleteQuarantineClick,
            modifier = modifier,
        )

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
                files.isNotEmpty() -> MediaGrid(
                    files = files,
                    imagePreviewLoader = imagePreviewLoader,
                    selectedFile = selectedFile,
                    onFileSelected = onFileSelected,
                )
                scanUiState == ScanUiState.Idle -> EmptyListText(emptyText)
                scanUiState == ScanUiState.Loading -> EmptyListText("Сканирую папку...")
                scanUiState is ScanUiState.Error -> EmptyListText(scanUiState.message)
                files.isEmpty() -> EmptyListText("Медиафайлы не найдены.")
                else -> MediaGrid(
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
private fun MediaGrid(
    files: List<PlannedMediaFile>,
    imagePreviewLoader: ImagePreviewLoader,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        gridItems(files, key = { it.sourcePath }) { plannedFile ->
            MediaGridTile(
                plannedFile = plannedFile,
                imagePreviewLoader = imagePreviewLoader,
                selected = plannedFile == selectedFile,
                onClick = { onFileSelected(plannedFile) },
            )
        }
    }
}

@Composable
private fun DuplicateReviewPanel(
    title: String,
    emptyText: String,
    groups: List<DuplicateReviewGroup>,
    quarantineMode: Boolean,
    imagePreviewLoader: ImagePreviewLoader,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    actionText: String? = null,
    secondaryActionText: String? = null,
    actionMessage: String? = null,
    onActionClick: (() -> Unit)? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val groupIds = groups.map { it.id }
    var selectedGroupId by remember(title, groupIds) { mutableStateOf(groupIds.firstOrNull()) }
    val activeGroup = groups.firstOrNull { it.id == selectedGroupId } ?: groups.firstOrNull()

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
            PanelActions(
                visible = groups.isNotEmpty(),
                actionText = actionText,
                secondaryActionText = secondaryActionText,
                onActionClick = onActionClick,
                onSecondaryActionClick = onSecondaryActionClick,
            )
            if (actionMessage != null) {
                Text(
                    text = actionMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            if (groups.isEmpty() || activeGroup == null) {
                EmptyListText(emptyText)
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DuplicateGroupSelector(
                        groups = groups,
                        selectedGroupId = activeGroup.id,
                        onGroupSelected = { selectedGroupId = it },
                    )
                    Row(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DuplicateBucket(
                            title = "Оставляем в Library",
                            files = activeGroup.libraryFiles,
                            emptyText = "Оригинал в Library не найден.",
                            imagePreviewLoader = imagePreviewLoader,
                            selectedFile = selectedFile,
                            onFileSelected = onFileSelected,
                            modifier = Modifier.weight(1f),
                        )
                        DuplicateBucket(
                            title = if (quarantineMode) "В Duplicates" else "Кандидаты к переносу",
                            files = activeGroup.duplicateFiles,
                            emptyText = if (quarantineMode) {
                                "В карантине нет файлов этой группы."
                            } else {
                                "Кандидатов к переносу нет."
                            },
                            imagePreviewLoader = imagePreviewLoader,
                            selectedFile = selectedFile,
                            onFileSelected = onFileSelected,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupSelector(
    groups: List<DuplicateReviewGroup>,
    selectedGroupId: String?,
    onGroupSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .width(170.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(groups, key = { it.id }) { group ->
            val selected = group.id == selectedGroupId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGroupSelected(group.id) },
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = group.totalCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = group.hashLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DuplicateBucket(
    title: String,
    files: List<PlannedMediaFile>,
    emptyText: String,
    imagePreviewLoader: ImagePreviewLoader,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = files.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            if (files.isEmpty()) {
                EmptyListText(emptyText)
            } else {
                MediaGrid(
                    files = files,
                    imagePreviewLoader = imagePreviewLoader,
                    selectedFile = selectedFile,
                    onFileSelected = onFileSelected,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PanelActions(
    visible: Boolean,
    actionText: String?,
    secondaryActionText: String?,
    onActionClick: (() -> Unit)?,
    onSecondaryActionClick: (() -> Unit)?,
) {
    if (!visible || actionText == null || onActionClick == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onActionClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(actionText)
        }
        if (secondaryActionText != null && onSecondaryActionClick != null) {
            OutlinedButton(
                onClick = onSecondaryActionClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(secondaryActionText)
            }
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
    actionText: String? = null,
    secondaryActionText: String? = null,
    actionMessage: String? = null,
    onActionClick: (() -> Unit)? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
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
            PanelActions(
                visible = groups.isNotEmpty(),
                actionText = actionText,
                secondaryActionText = secondaryActionText,
                onActionClick = onActionClick,
                onSecondaryActionClick = onSecondaryActionClick,
            )
            if (actionMessage != null) {
                Text(
                    text = actionMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
                    MediaGrid(
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
private fun MediaGridTile(
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
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.18f)
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
            Text(
                text = plannedFile.fileName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = plannedFile.shortLibraryCaption(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val ImportTargetStatus.label: String
    get() = when (this) {
        ImportTargetStatus.NotChecked -> "Цель не проверена"
        ImportTargetStatus.Ready -> "Будет скопировано"
        ImportTargetStatus.AlreadyExists -> "Уже есть в библиотеке"
    }

private data class DuplicateReviewGroup(
    val id: String,
    val title: String,
    val hashLabel: String,
    val libraryFiles: List<PlannedMediaFile>,
    val duplicateFiles: List<PlannedMediaFile>,
) {
    val totalCount: Int = libraryFiles.size + duplicateFiles.size
}

private fun buildDuplicateReviewGroups(
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
): List<DuplicateReviewGroup> {
    return if (duplicateFiles.isNotEmpty()) {
        duplicateFiles
            .groupBy { it.contentHash ?: it.quarantineGroupName() }
            .entries
            .sortedByDescending { it.value.size }
            .mapIndexed { index, entry ->
                val hash = entry.key
                DuplicateReviewGroup(
                    id = hash,
                    title = "Группа ${index + 1}",
                    hashLabel = hash.take(12),
                    libraryFiles = libraryFiles
                        .filter { it.contentHash == hash }
                        .sortedBy { it.targetRelativePath },
                    duplicateFiles = entry.value.sortedBy { it.targetRelativePath },
                )
            }
    } else {
        libraryFiles
            .asSequence()
            .filter { it.contentHash != null }
            .groupBy { it.contentHash.orEmpty() }
            .values
            .filter { it.size > 1 }
            .sortedByDescending { it.size }
            .mapIndexed { index, files ->
                val sortedFiles = files.sortedBy { it.targetRelativePath }
                val hash = sortedFiles.firstNotNullOfOrNull { it.contentHash }.orEmpty()
                DuplicateReviewGroup(
                    id = hash.ifBlank { "duplicate-$index" },
                    title = "Дубликат ${index + 1}",
                    hashLabel = hash.take(12),
                    libraryFiles = sortedFiles.take(1),
                    duplicateFiles = sortedFiles.drop(1),
                )
            }
    }
}

private fun duplicateActionText(
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
    duplicateDeleteAwaitingConfirmation: Boolean,
): String? {
    return when {
        duplicateDeleteAwaitingConfirmation && duplicateFiles.isNotEmpty() -> "Подтвердить удаление"
        duplicateFiles.isNotEmpty() -> "Удалить файлы из Duplicates"
        libraryFiles.duplicateGroups().isNotEmpty() -> "Перенести дубли в Duplicates"
        else -> null
    }
}

private fun List<PlannedMediaFile>.duplicateGroups(): Map<String, List<PlannedMediaFile>> {
    return asSequence()
        .filter { it.contentHash != null }
        .groupBy { it.contentHash }
        .values
        .filter { it.size > 1 }
        .sortedByDescending { it.size }
        .mapIndexed { index, files -> "Дубликат ${index + 1}" to files.sortedBy { it.targetRelativePath } }
        .toMap()
}

private fun PlannedMediaFile.quarantineGroupName(): String {
    val pathParts = targetRelativePath.replace('\\', '/').split('/')
    val duplicatesIndex = pathParts.indexOfLast { it == "Duplicates" }
    return pathParts.getOrNull(duplicatesIndex + 1)?.takeIf { it.isNotBlank() } ?: "Без группы"
}

private fun PlannedMediaFile.shortLibraryCaption(): String {
    val normalizedPath = targetRelativePath.replace('\\', '/')
    val sectionPath = when {
        "Library/" in normalizedPath -> normalizedPath.substringAfter("Library/")
        "Duplicates/" in normalizedPath -> normalizedPath.substringAfter("Duplicates/")
        else -> return targetStatus.label
    }

    return sectionPath.substringBeforeLast('/', missingDelimiterValue = targetStatus.label)
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
