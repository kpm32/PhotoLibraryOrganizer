package com.anvar.photolibraryorganizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImportUiState
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
            ImportPanel(
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

private fun duplicateActionText(
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
    duplicateDeleteAwaitingConfirmation: Boolean,
): String? {
    return when {
        duplicateDeleteAwaitingConfirmation && duplicateFiles.isNotEmpty() -> "Подтвердить удаление"
        duplicateFiles.isNotEmpty() -> "Удалить файлы из Duplicates"
        libraryFiles.hasDuplicateGroups() -> "Перенести дубли в Duplicates"
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
