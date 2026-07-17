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
                text = "Инспектор",
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
            OutlinedButton(
                onClick = onRefreshLibraryClick,
                enabled = !plan.destinationFolder.isNullOrBlank() && !isLibraryRefreshing,
            ) {
                Text(if (isLibraryRefreshing) "Обновляю..." else "Обновить библиотеку")
            }
            if (isLibraryRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = libraryRefreshProgress.toRefreshText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onCancelRefreshLibraryClick) {
                    Text("Остановить обновление")
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
            FileNavigationControls(
                selectedFileIndex = selectedFileIndex,
                navigationFileCount = navigationFileCount,
                onPreviousFileClick = onPreviousFileClick,
                onNextFileClick = onNextFileClick,
            )
            SummaryRow("Файл", selectedFile.fileName)
            SummaryRow("Размер", selectedFile.sizeBytes.toReadableSize())
            SummaryRow("Дата съемки", selectedFile.capturedAtEpochMillis.toReadableDateTimeOrEmpty())
            SummaryRow("Дата файла", selectedFile.modifiedAtEpochMillis.toReadableDateTimeOrEmpty())
            SummaryRow("Использована для папки", selectedFile.libraryDateLabel())
            SummaryRow("SHA-256", selectedFile.contentHash?.take(12) ?: "Нет")
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
                    Text("Крупно")
                }
                Button(
                    onClick = onOpenFileClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Открыть")
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
                    Text("В папке")
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
                            selectedFileTrashInProgress -> "Перенос..."
                            selectedFileTrashAwaitingConfirmation -> "Подтвердить"
                            else -> "В Корзину"
                        },
                    )
                }
                if (selectedFileTrashAwaitingConfirmation && !selectedFileTrashInProgress) {
                    OutlinedButton(
                        onClick = onCancelMoveSelectedFileToTrashClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Отмена")
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
                "${selectedFileIndex + 1} из $navigationFileCount"
            } else {
                "Файл выбран"
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
                Text("Назад")
            }
            OutlinedButton(
                onClick = onNextFileClick,
                modifier = Modifier.weight(1f),
                enabled = canNavigate,
            ) {
                Text("Вперед")
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
        SummaryRow("Всего файлов", scanUiState.summary.scannedFiles.toString())
        SummaryRow("Медиа", scanUiState.summary.mediaFiles.toString())
        SummaryRow("Фото", scanUiState.summary.imageFiles.toString())
        SummaryRow("Видео", scanUiState.summary.videoFiles.toString())
        SummaryRow("С датой съемки", scanUiState.summary.capturedDateFiles.toString())
        SummaryRow("Неподдерживаемые", scanUiState.summary.unsupportedFiles.toString())
        UnsupportedExtensionsRows(scanUiState.summary.unsupportedFileExtensions)
        SummaryRow("Размер медиа", scanUiState.summary.totalMediaBytes.toReadableSize())
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
            text = "Типы пропущенных",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        visibleExtensions.forEach { (extension, count) ->
            SummaryRow(extension, count.toString())
        }
        val hiddenCount = sortedExtensions.size - initialVisibleCount
        if (hiddenCount > 0) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Свернуть" else "Показать еще $hiddenCount")
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
        "дата съемки"
    } else if (modifiedAtEpochMillis != null) {
        "дата файла"
    } else {
        null
    }
    return month?.takeIf { it.length == 7 }
        ?.let { value -> if (source == null) value else "$value ($source)" }
        ?: "Не определена"
}

private fun LibraryRefreshProgress?.toRefreshText(): String {
    if (this == null) return "Готовлю обновление библиотеки..."
    return if (section == LibraryRefreshSection.Saving) {
        "Сохраняю локальный индекс. Окно можно оставить открытым."
    } else {
        "${section.title}: просмотрено $scannedFiles, медиа $mediaFiles, пропущено $unsupportedFiles."
    }
}

private fun Long?.toReadableDateTimeOrEmpty(): String {
    if (this == null) return "Нет"
    val dateTime = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val month = (dateTime.month.ordinal + 1).toString().padStart(2, '0')
    val day = dateTime.day.toString().padStart(2, '0')
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "${dateTime.year}-$month-$day $hour:$minute"
}
