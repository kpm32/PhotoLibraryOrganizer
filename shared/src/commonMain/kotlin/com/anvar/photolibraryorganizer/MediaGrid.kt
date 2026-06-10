package com.anvar.photolibraryorganizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader

@Composable
internal fun MediaGrid(
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
        items(files, key = { it.sourcePath }) { plannedFile ->
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

private fun PlannedMediaFile.shortLibraryCaption(): String {
    val normalizedPath = targetRelativePath.replace('\\', '/')
    val sectionPath = when {
        "Library/" in normalizedPath -> normalizedPath.substringAfter("Library/")
        "Duplicates/" in normalizedPath -> normalizedPath.substringAfter("Duplicates/")
        else -> return targetStatus.label
    }

    return sectionPath.substringBeforeLast('/', missingDelimiterValue = targetStatus.label)
}

private val ImportTargetStatus.label: String
    get() = when (this) {
        ImportTargetStatus.NotChecked -> "Цель не проверена"
        ImportTargetStatus.Ready -> "Будет скопировано"
        ImportTargetStatus.AlreadyExists -> "Уже есть в библиотеке"
    }
