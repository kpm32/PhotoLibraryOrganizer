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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ScanUiState

@Composable
internal fun InspectorPanel(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    selectedFile: PlannedMediaFile?,
    selectedFileIndex: Int,
    navigationFileCount: Int,
    imagePreviewUiState: ImagePreviewUiState,
    onOpenFileClick: () -> Unit,
    onRevealFileClick: () -> Unit,
    onPreviousFileClick: () -> Unit,
    onNextFileClick: () -> Unit,
    onSourceFolderClick: () -> Unit,
    onDestinationFolderClick: () -> Unit,
    onRefreshLibraryClick: () -> Unit,
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
                onOpenFileClick = onOpenFileClick,
                onRevealFileClick = onRevealFileClick,
                onPreviousFileClick = onPreviousFileClick,
                onNextFileClick = onNextFileClick,
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
                enabled = !plan.destinationFolder.isNullOrBlank(),
            ) {
                Text("Обновить библиотеку")
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
    onOpenFileClick: () -> Unit,
    onRevealFileClick: () -> Unit,
    onPreviousFileClick: () -> Unit,
    onNextFileClick: () -> Unit,
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
            SummaryRow("Дата", selectedFile.libraryDateLabel())
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
                Button(
                    onClick = onOpenFileClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Открыть")
                }
                OutlinedButton(
                    onClick = onRevealFileClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("В папке")
                }
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

    val visibleExtensions = unsupportedFileExtensions.entries.take(8)
    SummaryRow(
        label = "Типы пропущенных",
        value = visibleExtensions.joinToString { (extension, count) -> "$extension: $count" },
    )
    val hiddenCount = unsupportedFileExtensions.size - visibleExtensions.size
    if (hiddenCount > 0) {
        SummaryRow("Еще типов", hiddenCount.toString())
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
    return month?.takeIf { it.length == 7 } ?: "Не определена"
}
