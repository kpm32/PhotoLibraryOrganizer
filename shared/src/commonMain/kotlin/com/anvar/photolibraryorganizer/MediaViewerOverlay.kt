package com.anvar.photolibraryorganizer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState

@Composable
internal fun MediaViewerOverlay(
    selectedFile: PlannedMediaFile,
    selectedFileIndex: Int,
    navigationFileCount: Int,
    imagePreviewUiState: ImagePreviewUiState,
    onCloseClick: () -> Unit,
    onPreviousFileClick: () -> Unit,
    onNextFileClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onRevealFileClick: () -> Unit,
) {
    val canNavigate = navigationFileCount > 1 && selectedFileIndex >= 0

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedFile.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = viewerCounterLabel(selectedFileIndex, navigationFileCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = onPreviousFileClick,
                    enabled = canNavigate,
                ) {
                    Text("Назад")
                }
                OutlinedButton(
                    onClick = onNextFileClick,
                    enabled = canNavigate,
                ) {
                    Text("Вперед")
                }
                TextButton(onClick = onCloseClick) {
                    Text("Закрыть")
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                when (imagePreviewUiState) {
                    ImagePreviewUiState.Empty -> ViewerPlaceholder("Фото не выбрано")
                    ImagePreviewUiState.Loading -> ViewerPlaceholder("Загружаю превью...")
                    ImagePreviewUiState.Unsupported -> ViewerPlaceholder("Превью пока недоступно")
                    is ImagePreviewUiState.Success -> {
                        Image(
                            bitmap = imagePreviewUiState.image,
                            contentDescription = selectedFile.fileName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedFile.targetRelativePath,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(onClick = onOpenFileClick) {
                    Text("Открыть")
                }
                OutlinedButton(onClick = onRevealFileClick) {
                    Text("В папке")
                }
            }
        }
    }
}

@Composable
private fun ViewerPlaceholder(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun viewerCounterLabel(selectedFileIndex: Int, navigationFileCount: Int): String {
    return if (selectedFileIndex >= 0 && navigationFileCount > 0) {
        "${selectedFileIndex + 1} из $navigationFileCount"
    } else {
        "Файл выбран"
    }
}
