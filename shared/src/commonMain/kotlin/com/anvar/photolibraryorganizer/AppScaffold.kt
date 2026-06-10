package com.anvar.photolibraryorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.ImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ScanUiState

@Composable
internal fun PhotoLibraryOrganizerApp(
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
    onSourceFolderClick: () -> Unit,
    onDestinationFolderClick: () -> Unit,
    onRefreshLibraryClick: () -> Unit,
    onMoveDuplicatesClick: () -> Unit,
    onRequestDeleteQuarantineClick: () -> Unit,
    onConfirmDeleteQuarantineClick: () -> Unit,
    onCancelDeleteQuarantineClick: () -> Unit,
    onSectionSelected: (AppSection) -> Unit,
    onImportModeSelected: (ImportMode) -> Unit,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImportClick: () -> Unit,
    onCancelImportClick: () -> Unit,
    selectedFile: PlannedMediaFile?,
    imagePreviewUiState: ImagePreviewUiState,
    onRevealFileClick: () -> Unit,
    onFileSelected: (PlannedMediaFile) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .safeContentPadding()
                .fillMaxSize()
                .padding(start = 14.dp, top = 16.dp, end = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LibrarySidebar(
                selectedSection = selectedSection,
                onSectionSelected = onSectionSelected,
            )
            MainWorkspace(
                plan = plan,
                scanUiState = scanUiState,
                importUiState = importUiState,
                libraryFiles = libraryFiles,
                duplicateFiles = duplicateFiles,
                selectedSection = selectedSection,
                imagePreviewLoader = imagePreviewLoader,
                duplicateActionMessage = duplicateActionMessage,
                duplicateDeleteAwaitingConfirmation = duplicateDeleteAwaitingConfirmation,
                importAvailability = importAvailability,
                selectedFile = selectedFile,
                selectedMode = plan.importMode,
                onScanClick = onScanClick,
                onImportClick = onImportClick,
                onConfirmImportClick = onConfirmImportClick,
                onCancelImportClick = onCancelImportClick,
                onMoveDuplicatesClick = onMoveDuplicatesClick,
                onRequestDeleteQuarantineClick = onRequestDeleteQuarantineClick,
                onConfirmDeleteQuarantineClick = onConfirmDeleteQuarantineClick,
                onCancelDeleteQuarantineClick = onCancelDeleteQuarantineClick,
                onImportModeSelected = onImportModeSelected,
                onFileSelected = onFileSelected,
                modifier = Modifier.weight(1f),
            )
            InspectorPanel(
                plan = plan,
                scanUiState = scanUiState,
                selectedFile = selectedFile,
                imagePreviewUiState = imagePreviewUiState,
                onRevealFileClick = onRevealFileClick,
                onSourceFolderClick = onSourceFolderClick,
                onDestinationFolderClick = onDestinationFolderClick,
                onRefreshLibraryClick = onRefreshLibraryClick,
            )
        }
    }
}

@Composable
private fun LibrarySidebar(
    selectedSection: AppSection,
    onSectionSelected: (AppSection) -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Фотоархив",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            SidebarSection(
                title = "Библиотека",
                items = listOf(
                    AppSection.AllPhotos,
                    AppSection.Years,
                    AppSection.Months,
                    AppSection.WithoutDate,
                ),
                selectedSection = selectedSection,
                onSectionSelected = onSectionSelected,
            )
            SidebarSection(
                title = "Работа",
                items = listOf(
                    AppSection.Import,
                    AppSection.Duplicates,
                    AppSection.Errors,
                ),
                selectedSection = selectedSection,
                onSectionSelected = onSectionSelected,
            )
        }
    }
}

@Composable
private fun SidebarSection(
    title: String,
    items: List<AppSection>,
    selectedSection: AppSection,
    onSectionSelected: (AppSection) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        items.forEach { item ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSectionSelected(item) },
                color = if (item == selectedSection) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = item.title,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (item == selectedSection) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
