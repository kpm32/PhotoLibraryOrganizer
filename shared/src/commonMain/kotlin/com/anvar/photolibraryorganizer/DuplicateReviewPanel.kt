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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader

@Composable
internal fun DuplicateReviewPanel(
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
    actionInProgress: Boolean = false,
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
                            title = "Оставляем в библиотеке",
                            files = activeGroup.libraryFiles,
                            emptyText = "Оригинал в библиотеке не найден.",
                            imagePreviewLoader = imagePreviewLoader,
                            selectedFile = selectedFile,
                            onFileSelected = onFileSelected,
                            modifier = Modifier.weight(1f),
                        )
                        DuplicateBucket(
                            title = if (quarantineMode) "В папке дублей" else "Кандидаты к переносу",
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

internal data class DuplicateReviewGroup(
    val id: String,
    val title: String,
    val hashLabel: String,
    val libraryFiles: List<PlannedMediaFile>,
    val duplicateFiles: List<PlannedMediaFile>,
) {
    val totalCount: Int = libraryFiles.size + duplicateFiles.size
}

internal fun buildDuplicateReviewGroups(
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

private fun PlannedMediaFile.quarantineGroupName(): String {
    val pathParts = targetRelativePath.replace('\\', '/').split('/')
    val duplicatesIndex = pathParts.indexOfLast { it == "Duplicates" }
    return pathParts.getOrNull(duplicatesIndex + 1)?.takeIf { it.isNotBlank() } ?: "Без группы"
}
