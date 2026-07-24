package com.anvar.photolibraryorganizer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.AppIssue
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ImportReport
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.LibraryRefreshProgress
import com.anvar.photolibraryorganizer.presentation.ScanUiState
import kotlinx.coroutines.Job

/**
 * Compose state holder for the desktop application screen.
 *
 * This is intentionally not Android's `ViewModel`; it is a platform-neutral
 * state object that keeps mutable UI state outside the root composable.
 */
@Stable
internal class PhotoLibraryAppState {
    var sourceFolder by mutableStateOf<String?>(null)
    var destinationFolder by mutableStateOf<String?>(null)
    var importMode by mutableStateOf(ImportMode.ScanOnly)
    var importRules by mutableStateOf(ImportOrganizationRules.Default)
    var selectedSection by mutableStateOf(AppSection.AllPhotos)
    var scanUiState by mutableStateOf<ScanUiState>(ScanUiState.Idle)
    var scanRequestToken by mutableStateOf(0)
    var scanJob by mutableStateOf<Job?>(null)
    var importJob by mutableStateOf<Job?>(null)
    var refreshLibraryJob by mutableStateOf<Job?>(null)
    var lastImportProgress by mutableStateOf<ImportMediaFilesProgress?>(null)
    var importUiState by mutableStateOf<ImportUiState>(ImportUiState.Idle)
    var lastImportReport by mutableStateOf<ImportReport?>(null)
    var emptyFolderCleanupMessage by mutableStateOf<String?>(null)
    var emptyFolderCleanupAwaitingConfirmation by mutableStateOf(false)
    var importHistory by mutableStateOf<List<ImportReport>>(emptyList())
    var selectedFile by mutableStateOf<PlannedMediaFile?>(null)
    var libraryFiles by mutableStateOf<List<PlannedMediaFile>>(emptyList())
    var duplicateFiles by mutableStateOf<List<PlannedMediaFile>>(emptyList())
    var unsupportedFiles by mutableStateOf<List<PlannedMediaFile>>(emptyList())
    var isLibraryRefreshing by mutableStateOf(false)
    var libraryRefreshProgress by mutableStateOf<LibraryRefreshProgress?>(null)
    var duplicateActionMessage by mutableStateOf<String?>(null)
    var duplicateDeleteAwaitingConfirmation by mutableStateOf(false)
    var duplicateActionInProgress by mutableStateOf(false)
    var unsupportedActionMessage by mutableStateOf<String?>(null)
    var unsupportedDeleteAwaitingConfirmation by mutableStateOf(false)
    var unsupportedActionInProgress by mutableStateOf(false)
    var selectedFileTrashAwaitingConfirmation by mutableStateOf(false)
    var selectedFileTrashMessage by mutableStateOf<String?>(null)
    var selectedFileTrashInProgress by mutableStateOf(false)
    var imagePreviewUiState by mutableStateOf<ImagePreviewUiState>(ImagePreviewUiState.Empty)
    var issues by mutableStateOf<List<AppIssue>>(emptyList())
        private set
    private var nextIssueId by mutableStateOf(1)

    fun addIssue(title: String, detail: String) {
        issues = listOf(AppIssue(nextIssueId, title, detail)) + issues
        nextIssueId += 1
    }

    fun clearIssues() {
        issues = emptyList()
    }

    /**
     * Applies a persisted or freshly scanned library index to the visible
     * workspace and keeps the selected file valid.
     */
    fun applyLibraryIndexSnapshot(
        snapshot: LibraryIndexSnapshot,
        selectFirstFile: Boolean,
    ) {
        libraryFiles = snapshot.libraryFiles
        duplicateFiles = snapshot.duplicateFiles
        unsupportedFiles = snapshot.unsupportedFiles
        selectedFile = if (selectFirstFile) {
            libraryFiles.firstOrNull()
        } else {
            selectedFile?.takeIf { selected ->
                allVisibleFiles().any { it.sourcePath == selected.sourcePath }
            } ?: libraryFiles.firstOrNull()
        }
    }

    /**
     * Clears transient scan/import/action state after a folder selection changes.
     *
     * Destination changes may keep or reload the library index, while source
     * changes clear the current library view because the old scan context is no
     * longer meaningful.
     */
    fun resetAfterFolderSelection(clearLibraryFiles: Boolean) {
        scanUiState = ScanUiState.Idle
        importUiState = ImportUiState.Idle
        lastImportReport = null
        emptyFolderCleanupMessage = null
        emptyFolderCleanupAwaitingConfirmation = false
        duplicateActionMessage = null
        duplicateDeleteAwaitingConfirmation = false
        duplicateActionInProgress = false
        unsupportedActionMessage = null
        unsupportedDeleteAwaitingConfirmation = false
        unsupportedActionInProgress = false
        selectedFileTrashAwaitingConfirmation = false
        selectedFileTrashMessage = null
        selectedFileTrashInProgress = false
        refreshLibraryJob?.cancel()
        refreshLibraryJob = null
        isLibraryRefreshing = false
        libraryRefreshProgress = null
        selectedFile = null
        imagePreviewUiState = ImagePreviewUiState.Empty
        if (clearLibraryFiles) {
            libraryFiles = emptyList()
            duplicateFiles = emptyList()
            unsupportedFiles = emptyList()
        }
    }

    /**
     * Removes a file from all visible collections after a successful Trash
     * operation and advances selection to the next available file.
     */
    fun removeFileFromVisibleState(
        path: String,
        preferredSelectionFiles: List<PlannedMediaFile> = allVisibleFiles(),
    ) {
        libraryFiles = libraryFiles.filterNot { it.sourcePath == path }
        duplicateFiles = duplicateFiles.filterNot { it.sourcePath == path }
        unsupportedFiles = unsupportedFiles.filterNot { it.sourcePath == path }
        selectedFile = preferredSelectionFiles
            .filterNot { it.sourcePath == path }
            .firstOrNull()
            ?: allVisibleFiles().firstOrNull()
        imagePreviewUiState = if (selectedFile == null) {
            ImagePreviewUiState.Empty
        } else {
            imagePreviewUiState
        }
    }

    private fun allVisibleFiles(): List<PlannedMediaFile> {
        return libraryFiles + duplicateFiles + unsupportedFiles
    }
}

@Composable
internal fun rememberPhotoLibraryAppState(): PhotoLibraryAppState {
    return remember { PhotoLibraryAppState() }
}
