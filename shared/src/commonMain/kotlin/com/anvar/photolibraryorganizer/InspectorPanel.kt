package com.anvar.photolibraryorganizer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.LibraryRefreshProgress
import com.anvar.photolibraryorganizer.presentation.LibraryRefreshSection
import com.anvar.photolibraryorganizer.presentation.ScanUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Right-side inspector for the selected file and the active source/library
 * folders.
 *
 * Dangerous actions here are intentionally single-file actions and require
 * confirmation before moving anything to Trash.
 */
@Composable
internal fun InspectorPanel(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    selectedFile: PlannedMediaFile?,
    selectedFileIndex: Int,
    navigationFileCount: Int,
    imagePreviewUiState: ImagePreviewUiState,
    onOpenPreviewClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onRevealFileClick: () -> Unit,
    onRequestMoveSelectedFileToTrashClick: () -> Unit,
    onCancelMoveSelectedFileToTrashClick: () -> Unit,
    onPreviousFileClick: () -> Unit,
    onNextFileClick: () -> Unit,
    onSourceFolderClick: () -> Unit,
    onDestinationFolderClick: () -> Unit,
    onRefreshLibraryClick: () -> Unit,
    onCancelRefreshLibraryClick: () -> Unit,
    isLibraryRefreshing: Boolean,
    libraryRefreshProgress: LibraryRefreshProgress?,
    selectedFileTrashAwaitingConfirmation: Boolean,
    selectedFileTrashMessage: String?,
    selectedFileTrashInProgress: Boolean,
) {
    Surface(
        modifier = Modifier
            .width(340.dp)
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
                text = uiText("Инспектор", "Inspector"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SelectedFilePreview(
                selectedFile = selectedFile,
                selectedFileIndex = selectedFileIndex,
                navigationFileCount = navigationFileCount,
                imagePreviewUiState = imagePreviewUiState,
                onOpenPreviewClick = onOpenPreviewClick,
                onOpenFileClick = onOpenFileClick,
                onRevealFileClick = onRevealFileClick,
                onRequestMoveSelectedFileToTrashClick = onRequestMoveSelectedFileToTrashClick,
                onCancelMoveSelectedFileToTrashClick = onCancelMoveSelectedFileToTrashClick,
                onPreviousFileClick = onPreviousFileClick,
                onNextFileClick = onNextFileClick,
                selectedFileTrashAwaitingConfirmation = selectedFileTrashAwaitingConfirmation,
                selectedFileTrashMessage = selectedFileTrashMessage,
                selectedFileTrashInProgress = selectedFileTrashInProgress,
            )
            HorizontalDivider()
            FolderSelector(
                title = uiText("Источник", "Source"),
                path = plan.sourceFolder,
                actionText = uiText("Выбрать", "Choose"),
                onClick = onSourceFolderClick,
            )
            FolderSelector(
                title = uiText("Библиотека", "Library"),
                path = plan.destinationFolder,
                actionText = uiText("Выбрать", "Choose"),
                onClick = onDestinationFolderClick,
            )
            OutlinedButton(
                onClick = onRefreshLibraryClick,
                enabled = !plan.destinationFolder.isNullOrBlank() && !isLibraryRefreshing,
            ) {
                Text(
                    if (isLibraryRefreshing) {
                        uiText("Обновляю...", "Refreshing...")
                    } else {
                        uiText("Обновить библиотеку", "Refresh Library")
                    },
                )
            }
            if (isLibraryRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = libraryRefreshProgress.toRefreshText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onCancelRefreshLibraryClick) {
                    Text(uiText("Остановить обновление", "Stop Refresh"))
                }
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = path ?: uiText("Папка пока не выбрана", "Folder is not selected yet"),
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
private fun SelectedFilePreview(
    selectedFile: PlannedMediaFile?,
    selectedFileIndex: Int,
    navigationFileCount: Int,
    imagePreviewUiState: ImagePreviewUiState,
    onOpenPreviewClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onRevealFileClick: () -> Unit,
    onRequestMoveSelectedFileToTrashClick: () -> Unit,
    onCancelMoveSelectedFileToTrashClick: () -> Unit,
    onPreviousFileClick: () -> Unit,
    onNextFileClick: () -> Unit,
    selectedFileTrashAwaitingConfirmation: Boolean,
    selectedFileTrashMessage: String?,
    selectedFileTrashInProgress: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            when (imagePreviewUiState) {
                ImagePreviewUiState.Empty -> PreviewPlaceholder(uiText("Фото не выбрано", "No photo selected"))
                ImagePreviewUiState.Loading -> PreviewPlaceholder(uiText("Загружаю превью...", "Loading preview..."))
                ImagePreviewUiState.Unsupported -> PreviewPlaceholder(uiText("Превью пока недоступно", "Preview is not available yet"))
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
            FileNavigationControls(
                selectedFileIndex = selectedFileIndex,
                navigationFileCount = navigationFileCount,
                onPreviousFileClick = onPreviousFileClick,
                onNextFileClick = onNextFileClick,
            )
            SummaryRow(uiText("Файл", "File"), selectedFile.fileName)
            SummaryRow(uiText("Размер", "Size"), selectedFile.sizeBytes.toReadableSize())
            SummaryRow(uiText("Дата съемки", "Capture Date"), selectedFile.capturedAtEpochMillis.toReadableDateTimeOrEmpty())
            SummaryRow(uiText("Дата файла", "File Date"), selectedFile.modifiedAtEpochMillis.toReadableDateTimeOrEmpty())
            SummaryRow(uiText("Использована для папки", "Used for Folder"), selectedFile.libraryDateLabel())
            SummaryRow("SHA-256", selectedFile.contentHash?.take(12) ?: uiText("Нет", "No"))
            Text(
                text = selectedFile.targetRelativePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpenPreviewClick,
                    modifier = Modifier.weight(1f),
                    enabled = imagePreviewUiState is ImagePreviewUiState.Success,
                ) {
                    Text(uiText("Крупно", "Large"))
                }
                Button(
                    onClick = onOpenFileClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(uiText("Открыть", "Open"))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRevealFileClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(uiText("В папке", "In Folder"))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onRequestMoveSelectedFileToTrashClick,
                    modifier = Modifier.weight(1f),
                    enabled = !selectedFileTrashInProgress,
                ) {
                    Text(
                        when {
                            selectedFileTrashInProgress -> uiText("Перенос...", "Moving...")
                            selectedFileTrashAwaitingConfirmation -> uiText("Подтвердить", "Confirm")
                            else -> uiText("В Корзину", "To Trash")
                        },
                    )
                }
                if (selectedFileTrashAwaitingConfirmation && !selectedFileTrashInProgress) {
                    OutlinedButton(
                        onClick = onCancelMoveSelectedFileToTrashClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(uiText("Отмена", "Cancel"))
                    }
                }
            }
            selectedFileTrashMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FileNavigationControls(
    selectedFileIndex: Int,
    navigationFileCount: Int,
    onPreviousFileClick: () -> Unit,
    onNextFileClick: () -> Unit,
) {
    val canNavigate = navigationFileCount > 1 && selectedFileIndex >= 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (selectedFileIndex >= 0 && navigationFileCount > 0) {
                uiText(
                    ru = "${selectedFileIndex + 1} из $navigationFileCount",
                    en = "${selectedFileIndex + 1} of $navigationFileCount",
                )
            } else {
                uiText("Файл выбран", "File selected")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onPreviousFileClick,
                modifier = Modifier.weight(1f),
                enabled = canNavigate,
            ) {
                Text(uiText("Назад", "Back"))
            }
            OutlinedButton(
                onClick = onNextFileClick,
                modifier = Modifier.weight(1f),
                enabled = canNavigate,
            ) {
                Text(uiText("Вперед", "Forward"))
            }
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
private fun ScanSummaryRows(scanUiState: ScanUiState.Success) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SummaryRow(uiText("Всего файлов", "Total Files"), scanUiState.summary.scannedFiles.toString())
        SummaryRow(uiText("Медиа", "Media"), scanUiState.summary.mediaFiles.toString())
        SummaryRow(uiText("Фото", "Photos"), scanUiState.summary.imageFiles.toString())
        SummaryRow(uiText("Видео", "Videos"), scanUiState.summary.videoFiles.toString())
        SummaryRow(uiText("С датой съемки", "With Capture Date"), scanUiState.summary.capturedDateFiles.toString())
        SummaryRow(uiText("Неподдерживаемые", "Unsupported"), scanUiState.summary.unsupportedFiles.toString())
        UnsupportedExtensionsRows(scanUiState.summary.unsupportedFileExtensions)
        SummaryRow(uiText("Размер медиа", "Media Size"), scanUiState.summary.totalMediaBytes.toReadableSize())
    }
}

@Composable
private fun UnsupportedExtensionsRows(unsupportedFileExtensions: Map<String, Int>) {
    if (unsupportedFileExtensions.isEmpty()) return

    var expanded by rememberSaveable(unsupportedFileExtensions) { mutableStateOf(false) }
    val sortedExtensions = unsupportedFileExtensions.entries.sortedByDescending { it.value }
    val initialVisibleCount = 5
    val visibleExtensions = if (expanded) sortedExtensions else sortedExtensions.take(initialVisibleCount)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = uiText("Типы пропущенных", "Skipped Types"),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        visibleExtensions.forEach { (extension, count) ->
            SummaryRow(extension, count.toString())
        }
        val hiddenCount = sortedExtensions.size - initialVisibleCount
        if (hiddenCount > 0) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) {
                        uiText("Свернуть", "Collapse")
                    } else {
                        uiText("Показать еще $hiddenCount", "Show $hiddenCount more")
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun PlannedMediaFile.libraryDateLabel(): String {
    val normalizedPath = targetRelativePath.replace('\\', '/')
    val libraryPart = normalizedPath.substringAfter("Library/", missingDelimiterValue = "")
    val parts = libraryPart.split('/')
    val month = parts.getOrNull(1)
    val source = if (capturedAtEpochMillis != null) {
        uiText("дата съемки", "capture date")
    } else if (modifiedAtEpochMillis != null) {
        uiText("дата файла", "file date")
    } else {
        null
    }
    return month?.takeIf { it.length == 7 }
        ?.let { value -> if (source == null) value else "$value ($source)" }
        ?: uiText("Не определена", "Not determined")
}

private fun LibraryRefreshProgress?.toRefreshText(): String {
    if (this == null) {
        return uiText("Готовлю обновление библиотеки...", "Preparing library refresh...")
    }
    return if (section == LibraryRefreshSection.Saving) {
        uiText(
            ru = "Сохраняю локальный индекс. Окно можно оставить открытым.",
            en = "Saving the local index. You can keep the window open.",
        )
    } else {
        val title = uiText(section.ruTitle, section.enTitle)
        uiText(
            ru = "$title: просмотрено $scannedFiles, медиа $mediaFiles, пропущено $unsupportedFiles.",
            en = "$title: scanned $scannedFiles, media $mediaFiles, skipped $unsupportedFiles.",
        )
    }
}

private fun Long?.toReadableDateTimeOrEmpty(): String {
    if (this == null) return uiText("Нет", "No")
    val dateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = (dateTime.month.ordinal + 1).toString().padStart(2, '0')
    val day = dateTime.day.toString().padStart(2, '0')
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "${dateTime.year}-$month-$day $hour:$minute"
}
