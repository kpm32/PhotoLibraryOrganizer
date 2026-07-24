package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.ScanUiState

/**
 * Returns the active ordered file list used by inspector navigation and the
 * large preview overlay.
 */
internal fun navigationFilesForSection(
    selectedSection: AppSection,
    scanUiState: ScanUiState,
    libraryFiles: List<PlannedMediaFile>,
    duplicateFiles: List<PlannedMediaFile>,
    unsupportedFiles: List<PlannedMediaFile>,
): List<PlannedMediaFile> {
    return when (selectedSection) {
        AppSection.Import -> (scanUiState as? ScanUiState.Success)?.plannedFiles.orEmpty()
        AppSection.AllPhotos,
        AppSection.Years,
        AppSection.Months -> libraryFiles

        AppSection.WithoutDate -> libraryFiles.filter { it.libraryDateGroup() == null }
        AppSection.Duplicates -> buildDuplicateReviewGroups(
            libraryFiles = libraryFiles,
            duplicateFiles = duplicateFiles,
        ).flatMap { group -> group.libraryFiles + group.duplicateFiles }
        AppSection.Unsupported -> unsupportedFiles
        AppSection.Errors,
        AppSection.About -> emptyList()
    }
}

internal fun List<PlannedMediaFile>.nextFrom(
    selectedIndex: Int,
    step: Int,
): PlannedMediaFile {
    val nextIndex = (selectedIndex + step).floorMod(size)
    return this[nextIndex]
}

private fun Int.floorMod(size: Int): Int {
    return ((this % size) + size) % size
}
