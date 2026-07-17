package com.anvar.photolibraryorganizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ScanUiState

@Composable
internal fun MediaList(
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
        modifier = modifier
            .fillMaxWidth()
            .fillMaxSize(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
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
                    modifier = Modifier.weight(1f),
                )
                scanUiState == ScanUiState.Idle -> EmptyListText(emptyText)
                scanUiState == ScanUiState.Canceled -> EmptyListText("Сканирование остановлено. Файлы не изменялись.")
                scanUiState is ScanUiState.Loading -> EmptyListText(scanUiState.progress.toMediaListProgressText())
                scanUiState is ScanUiState.Error -> EmptyListText(scanUiState.message)
                files.isEmpty() -> EmptyListText("Медиафайлы не найдены.")
                else -> MediaGrid(
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
internal fun GroupedMediaList(
    title: String,
    emptyText: String,
    groups: Map<String, List<PlannedMediaFile>>,
    imagePreviewLoader: ImagePreviewLoader,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    actionText: String? = null,
    secondaryActionText: String? = null,
    actionMessage: String? = null,
    actionInProgress: Boolean = false,
    onActionClick: (() -> Unit)? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    groupActionText: String? = null,
    onGroupActionClick: ((String) -> Unit)? = null,
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
                actionEnabled = !actionInProgress,
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
            if (activeGroup != null && groupActionText != null && onGroupActionClick != null) {
                OutlinedButton(
                    onClick = { onGroupActionClick(activeGroup) },
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = "$groupActionText: $activeGroup",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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
            .width(170.dp)
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
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = group.key,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = group.value.sumOf { it.sizeBytes }.toReadableSize(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
internal fun EmptyListText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress?.toMediaListProgressText(): String {
    return if (this == null) {
        "Сканирую папку..."
    } else {
        "Просмотрено файлов: $scannedFiles. Найдено медиа: $mediaFiles. Пропущено: $unsupportedFiles."
    }
}

internal data class LibraryDateGroup(
    val year: String,
    val month: String,
)

internal fun PlannedMediaFile.libraryDateGroup(): LibraryDateGroup? {
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
