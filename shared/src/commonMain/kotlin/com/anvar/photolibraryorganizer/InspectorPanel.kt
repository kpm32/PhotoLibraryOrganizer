package com.anvar.photolibraryorganizer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ScanUiState

@Composable
internal fun InspectorPanel(
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = mode.description,
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
