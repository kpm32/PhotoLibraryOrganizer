package com.anvar.photolibraryorganizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.detectMediaFileType
import com.anvar.photolibraryorganizer.presentation.AppIssue
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImportReport
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.ScanUiState

@Composable
internal fun MainWorkspace(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importUiState: ImportUiState,
    lastImportReport: ImportReport?,
    emptyFolderCleanupMessage: String?,
    emptyFolderCleanupAwaitingConfirmation: Boolean,
    importHistory: List<ImportReport>,
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
    unsupportedFiles: List<PlannedMediaFile>,
    selectedSection: AppSection,
    imagePreviewLoader: ImagePreviewLoader,
    duplicateActionMessage: String?,
    duplicateDeleteAwaitingConfirmation: Boolean,
    unsupportedActionMessage: String?,
    unsupportedDeleteAwaitingConfirmation: Boolean,
    importAvailability: ImportAvailability,
    isLibraryRefreshing: Boolean,
    issues: List<AppIssue>,
    selectedFile: PlannedMediaFile?,
    selectedMode: ImportMode,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImportClick: () -> Unit,
    onCancelImportClick: () -> Unit,
    onCancelRunningImportClick: () -> Unit,
    onRequestEmptyFolderCleanupClick: () -> Unit,
    onConfirmEmptyFolderCleanupClick: () -> Unit,
    onCancelEmptyFolderCleanupClick: () -> Unit,
    onOpenLibraryFolderClick: () -> Unit,
    onOpenUnsupportedFolderClick: () -> Unit,
    onOpenUnsupportedTypeFolderClick: (String) -> Unit,
    onOpenDuplicatesFolderClick: () -> Unit,
    onMoveDuplicatesClick: () -> Unit,
    onRequestDeleteQuarantineClick: () -> Unit,
    onConfirmDeleteQuarantineClick: () -> Unit,
    onCancelDeleteQuarantineClick: () -> Unit,
    onRequestDeleteUnsupportedClick: () -> Unit,
    onConfirmDeleteUnsupportedClick: () -> Unit,
    onCancelDeleteUnsupportedClick: () -> Unit,
    onClearIssuesClick: () -> Unit,
    onImportModeSelected: (ImportMode) -> Unit,
    onImportRulesSelected: (ImportOrganizationRules) -> Unit,
    onCancelScanClick: () -> Unit,
    onFileSelected: (PlannedMediaFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var librarySearchQuery by remember { mutableStateOf("") }
    var mediaCategoryFilter by remember { mutableStateOf(MediaCategoryFilter.All) }
    var extensionFilter by remember { mutableStateOf("") }
    val searchEnabled = selectedSection.supportsLibrarySearch()
    val visibleLibraryFiles = if (searchEnabled) {
        libraryFiles.filterLibraryFiles(
            query = librarySearchQuery,
            mediaCategoryFilter = mediaCategoryFilter,
            extensionFilter = extensionFilter,
        )
    } else {
        libraryFiles
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WorkspaceHeader(selectedSection)
        if (selectedSection == AppSection.Import) {
            ImportPanel(
                plan = plan,
                scanUiState = scanUiState,
                importUiState = importUiState,
                lastImportReport = lastImportReport,
                emptyFolderCleanupMessage = emptyFolderCleanupMessage,
                emptyFolderCleanupAwaitingConfirmation = emptyFolderCleanupAwaitingConfirmation,
                importHistory = importHistory,
                importAvailability = importAvailability,
                selectedMode = selectedMode,
                onScanClick = onScanClick,
                onCancelScanClick = onCancelScanClick,
                onImportClick = onImportClick,
                onConfirmImportClick = onConfirmImportClick,
                onCancelImportClick = onCancelImportClick,
                onCancelRunningImportClick = onCancelRunningImportClick,
                onRequestEmptyFolderCleanupClick = onRequestEmptyFolderCleanupClick,
                onConfirmEmptyFolderCleanupClick = onConfirmEmptyFolderCleanupClick,
                onCancelEmptyFolderCleanupClick = onCancelEmptyFolderCleanupClick,
                onOpenLibraryFolderClick = onOpenLibraryFolderClick,
                onOpenUnsupportedFolderClick = onOpenUnsupportedFolderClick,
                onOpenDuplicatesFolderClick = onOpenDuplicatesFolderClick,
                onImportModeSelected = onImportModeSelected,
                onImportRulesSelected = onImportRulesSelected,
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
            if (searchEnabled) {
                LibrarySearchBar(
                    query = librarySearchQuery,
                    onQueryChange = { librarySearchQuery = it },
                    mediaCategoryFilter = mediaCategoryFilter,
                    onMediaCategoryFilterChange = { mediaCategoryFilter = it },
                    extensionFilter = extensionFilter,
                    onExtensionFilterChange = { extensionFilter = it },
                    totalFiles = libraryFiles.size,
                    visibleFiles = visibleLibraryFiles.size,
                )
            }
            LibrarySection(
                selectedSection = selectedSection,
                libraryFiles = visibleLibraryFiles,
                duplicateFiles = duplicateFiles,
                unsupportedFiles = unsupportedFiles,
                imagePreviewLoader = imagePreviewLoader,
                duplicateActionMessage = duplicateActionMessage,
                duplicateDeleteAwaitingConfirmation = duplicateDeleteAwaitingConfirmation,
                unsupportedActionMessage = unsupportedActionMessage,
                unsupportedDeleteAwaitingConfirmation = unsupportedDeleteAwaitingConfirmation,
                issues = issues,
                selectedFile = selectedFile,
                onFileSelected = onFileSelected,
                onMoveDuplicatesClick = onMoveDuplicatesClick,
                onRequestDeleteQuarantineClick = onRequestDeleteQuarantineClick,
                onConfirmDeleteQuarantineClick = onConfirmDeleteQuarantineClick,
                onCancelDeleteQuarantineClick = onCancelDeleteQuarantineClick,
                onRequestDeleteUnsupportedClick = onRequestDeleteUnsupportedClick,
                onConfirmDeleteUnsupportedClick = onConfirmDeleteUnsupportedClick,
                onCancelDeleteUnsupportedClick = onCancelDeleteUnsupportedClick,
                onOpenUnsupportedTypeFolderClick = onOpenUnsupportedTypeFolderClick,
                onClearIssuesClick = onClearIssuesClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    mediaCategoryFilter: MediaCategoryFilter,
    onMediaCategoryFilterChange: (MediaCategoryFilter) -> Unit,
    extensionFilter: String,
    onExtensionFilterChange: (String) -> Unit,
    totalFiles: Int,
    visibleFiles: Int,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск по имени, пути или SHA-256") },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MediaCategoryFilter.entries.forEach { filter ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = if (filter == mediaCategoryFilter) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        shape = MaterialTheme.shapes.small,
                        onClick = { onMediaCategoryFilterChange(filter) },
                    ) {
                        Text(
                            text = filter.title,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = extensionFilter,
                onValueChange = onExtensionFilterChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Расширение, например jpg или mov") },
            )
            Text(
                text = "Показано $visibleFiles из $totalFiles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                AppSection.Import -> "Сначала показываем план, потом выполняем выбранное действие: сканирование, копирование или перенос."
                AppSection.AllPhotos -> "Просмотр уже разложенной библиотеки."
                AppSection.Years -> "Библиотека, сгруппированная по годам."
                AppSection.Months -> "Библиотека, сгруппированная по месяцам."
                AppSection.WithoutDate -> "Файлы, для которых пока не удалось определить дату."
                AppSection.Duplicates -> "Файлы с одинаковым SHA-256 хэшем."
                AppSection.Unsupported -> "Файлы, которые приложение не считает фото или видео, лежат отдельно и не потеряны."
                AppSection.Errors -> "Ошибки импорта и сканирования будут собираться здесь."
                AppSection.About -> "Версия, статус релиза, лицензия и заметки по установке."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LibrarySection(
    selectedSection: AppSection,
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
    unsupportedFiles: List<PlannedMediaFile>,
    imagePreviewLoader: ImagePreviewLoader,
    duplicateActionMessage: String?,
    duplicateDeleteAwaitingConfirmation: Boolean,
    unsupportedActionMessage: String?,
    unsupportedDeleteAwaitingConfirmation: Boolean,
    issues: List<AppIssue>,
    selectedFile: PlannedMediaFile?,
    onFileSelected: (PlannedMediaFile) -> Unit,
    onMoveDuplicatesClick: () -> Unit,
    onRequestDeleteQuarantineClick: () -> Unit,
    onConfirmDeleteQuarantineClick: () -> Unit,
    onCancelDeleteQuarantineClick: () -> Unit,
    onRequestDeleteUnsupportedClick: () -> Unit,
    onConfirmDeleteUnsupportedClick: () -> Unit,
    onCancelDeleteUnsupportedClick: () -> Unit,
    onOpenUnsupportedTypeFolderClick: (String) -> Unit,
    onClearIssuesClick: () -> Unit,
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
            title = if (duplicateFiles.isEmpty()) "Дубликаты" else "Папка дублей",
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

        AppSection.Unsupported -> GroupedMediaList(
            title = "Неподдерживаемые",
            emptyText = "Папка пропущенных файлов пока пуста.",
            groups = unsupportedFiles
                .groupBy { it.unsupportedTypeGroup() }
                .toList()
                .sortedWith(
                    compareByDescending<Pair<String, List<PlannedMediaFile>>> { (_, files) -> files.sumOf { it.sizeBytes } }
                        .thenBy { it.first },
                )
                .toMap(),
            imagePreviewLoader = imagePreviewLoader,
            selectedFile = selectedFile,
            onFileSelected = onFileSelected,
            actionText = unsupportedActionText(
                unsupportedFiles = unsupportedFiles,
                unsupportedDeleteAwaitingConfirmation = unsupportedDeleteAwaitingConfirmation,
            ),
            secondaryActionText = if (unsupportedDeleteAwaitingConfirmation && unsupportedFiles.isNotEmpty()) {
                "Отмена"
            } else {
                null
            },
            actionMessage = unsupportedActionMessage,
            onActionClick = when {
                unsupportedDeleteAwaitingConfirmation && unsupportedFiles.isNotEmpty() -> onConfirmDeleteUnsupportedClick
                unsupportedFiles.isNotEmpty() -> onRequestDeleteUnsupportedClick
                else -> null
            },
            onSecondaryActionClick = onCancelDeleteUnsupportedClick,
            onGroupActionClick = onOpenUnsupportedTypeFolderClick,
            groupActionText = "Открыть группу",
            modifier = modifier,
        )

        AppSection.Errors -> ErrorsPanel(
            issues = issues,
            onClearIssuesClick = onClearIssuesClick,
            modifier = modifier,
        )

        AppSection.About -> AboutPanel(modifier = modifier)

        AppSection.Import -> PlaceholderPanel(
            title = selectedSection.title,
            text = "Этот раздел уже есть в навигации, но его логика будет добавлена отдельным шагом.",
            modifier = modifier,
        )
    }
}

@Composable
private fun AboutPanel(
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Photo Library Organizer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            AboutRow("Версия", "1.0.0-preview.7")
            AboutRow("Статус", "Preview-релиз для macOS")
            AboutRow("Интерфейс", "Русский")
            AboutRow("Лицензия", "MIT")
            AboutRow("GitHub", "github.com/kpm32/PhotoLibraryOrganizer")
            AboutRow(
                label = "Установка",
                value = "Сборка пока не подписана Apple Developer ID. Если macOS блокирует запуск, открой приложение через правый клик или разреши запуск в Privacy & Security.",
            )
            AboutRow(
                label = "Безопасность",
                value = "Для первого реального архива используй только сканирование или копирование. Перенос запускай после проверки результата.",
            )
        }
    }
}

@Composable
private fun AboutRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private fun duplicateActionText(
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
    duplicateDeleteAwaitingConfirmation: Boolean,
): String? {
    return when {
        duplicateDeleteAwaitingConfirmation && duplicateFiles.isNotEmpty() -> "Подтвердить перенос"
        duplicateFiles.isNotEmpty() -> "В Корзину из папки дублей"
        libraryFiles.hasDuplicateGroups() -> "Перенести дубли в папку дублей"
        else -> null
    }
}

private fun List<PlannedMediaFile>.hasDuplicateGroups(): Boolean {
    return asSequence()
        .filter { it.contentHash != null }
        .groupBy { it.contentHash }
        .values
        .any { it.size > 1 }
}

private fun unsupportedActionText(
    unsupportedFiles: List<PlannedMediaFile>,
    unsupportedDeleteAwaitingConfirmation: Boolean,
): String? {
    return when {
        unsupportedDeleteAwaitingConfirmation && unsupportedFiles.isNotEmpty() -> "Подтвердить перенос"
        unsupportedFiles.isNotEmpty() -> "В Корзину все пропущенные"
        else -> null
    }
}

private enum class MediaCategoryFilter(
    val title: String,
) {
    All("Все"),
    Images("Фото"),
    Videos("Видео"),
}

private fun AppSection.supportsLibrarySearch(): Boolean {
    return this == AppSection.AllPhotos ||
        this == AppSection.Years ||
        this == AppSection.Months ||
        this == AppSection.WithoutDate
}

private fun PlannedMediaFile.unsupportedTypeGroup(): String {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .ifBlank { "без расширения" }
    return extension
}

private fun List<PlannedMediaFile>.filterLibraryFiles(
    query: String,
    mediaCategoryFilter: MediaCategoryFilter,
    extensionFilter: String,
): List<PlannedMediaFile> {
    val normalizedQuery = query.trim()
    val normalizedExtension = extensionFilter.trim().removePrefix(".").lowercase()

    return filter { file ->
        val mediaType = detectMediaFileType(file.fileName)
        val queryMatches = normalizedQuery.isBlank() ||
            file.fileName.contains(normalizedQuery, ignoreCase = true) ||
            file.targetRelativePath.contains(normalizedQuery, ignoreCase = true) ||
            file.contentHash?.contains(normalizedQuery, ignoreCase = true) == true
        val categoryMatches = when (mediaCategoryFilter) {
            MediaCategoryFilter.All -> true
            MediaCategoryFilter.Images -> mediaType?.category == MediaFileCategory.Image
            MediaCategoryFilter.Videos -> mediaType?.category == MediaFileCategory.Video
        }
        val extensionMatches = normalizedExtension.isBlank() ||
            file.fileName.substringAfterLast('.', missingDelimiterValue = "")
                .equals(normalizedExtension, ignoreCase = true)

        queryMatches && categoryMatches && extensionMatches
    }
}
