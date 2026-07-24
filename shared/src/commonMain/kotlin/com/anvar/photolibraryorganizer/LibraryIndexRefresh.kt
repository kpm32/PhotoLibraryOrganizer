package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress
import com.anvar.photolibraryorganizer.domain.repository.LibraryIndexStorage
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.LibraryRefreshProgress
import com.anvar.photolibraryorganizer.presentation.LibraryRefreshSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * Rebuilds the lightweight library index by scanning the organized library,
 * duplicate quarantine, and unsupported quarantine folders.
 */
internal suspend fun refreshLibraryIndexSnapshot(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
    libraryIndexStorage: LibraryIndexStorage,
    onProgress: (LibraryRefreshProgress) -> Unit,
): LibraryIndexSnapshot {
    val destination = destinationFolder?.trim()?.trimEnd('/').orEmpty()
    val snapshot = LibraryIndexSnapshot(
        destinationFolder = destination,
        libraryFiles = refreshLibraryFiles(destination, photoSourceScanner, onProgress),
        duplicateFiles = refreshDuplicateFiles(destination, photoSourceScanner, onProgress),
        unsupportedFiles = refreshUnsupportedFiles(destination, photoSourceScanner, onProgress),
        updatedAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
    )
    onProgress(LibraryRefreshProgress(section = LibraryRefreshSection.Saving))
    libraryIndexStorage.save(snapshot)
    return snapshot
}

private suspend fun refreshLibraryFiles(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
    onProgress: (LibraryRefreshProgress) -> Unit,
): List<PlannedMediaFile> {
    val libraryFolder = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/Library" }
        ?: return emptyList()

    return refreshPlannedFiles(libraryFolder, photoSourceScanner, LibraryRefreshSection.Library, onProgress)
}

private suspend fun refreshDuplicateFiles(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
    onProgress: (LibraryRefreshProgress) -> Unit,
): List<PlannedMediaFile> {
    val duplicatesFolder = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/Duplicates" }
        ?: return emptyList()

    return refreshPlannedFiles(duplicatesFolder, photoSourceScanner, LibraryRefreshSection.Duplicates, onProgress)
}

private suspend fun refreshUnsupportedFiles(
    destinationFolder: String?,
    photoSourceScanner: PhotoSourceScanner,
    onProgress: (LibraryRefreshProgress) -> Unit,
): List<PlannedMediaFile> {
    val unsupportedFolder = destinationFolder?.trim()?.trimEnd('/')?.let { "$it/Unsupported" }
        ?: return emptyList()

    return refreshAllFiles(unsupportedFolder, photoSourceScanner, LibraryRefreshSection.Unsupported, onProgress)
}

/**
 * Scans a folder as supported media and maps the result into UI/library items.
 */
private suspend fun refreshPlannedFiles(
    folder: String,
    photoSourceScanner: PhotoSourceScanner,
    section: LibraryRefreshSection,
    onProgress: (LibraryRefreshProgress) -> Unit,
): List<PlannedMediaFile> {
    return when (
        val result = withContext(Dispatchers.Default) {
            photoSourceScanner.scanFolder(
                path = folder,
                onProgress = { progress ->
                    onProgress(progress.toLibraryRefreshProgress(section))
                },
                readContentHash = false,
            )
        }
    ) {
        is AppResult.Success -> result.data.mediaFiles.map { mediaFile ->
            PlannedMediaFile(
                sourcePath = mediaFile.path,
                fileName = mediaFile.fileName,
                targetRelativePath = mediaFile.path,
                sizeBytes = mediaFile.sizeBytes,
                contentHash = mediaFile.contentHash,
                capturedAtEpochMillis = mediaFile.capturedAtEpochMillis,
                modifiedAtEpochMillis = mediaFile.modifiedAtEpochMillis,
            )
        }

        is AppResult.Error -> emptyList()
    }
}

private suspend fun refreshAllFiles(
    folder: String,
    photoSourceScanner: PhotoSourceScanner,
    section: LibraryRefreshSection,
    onProgress: (LibraryRefreshProgress) -> Unit,
): List<PlannedMediaFile> {
    return when (
        val result = withContext(Dispatchers.Default) {
            photoSourceScanner.scanFolder(
                path = folder,
                onProgress = { progress ->
                    onProgress(progress.toLibraryRefreshProgress(section))
                },
                readContentHash = false,
            )
        }
    ) {
        is AppResult.Success -> {
            val mediaFiles = result.data.mediaFiles.map { mediaFile ->
                PlannedMediaFile(
                    sourcePath = mediaFile.path,
                    fileName = mediaFile.fileName,
                    targetRelativePath = mediaFile.path,
                    sizeBytes = mediaFile.sizeBytes,
                    contentHash = mediaFile.contentHash,
                    capturedAtEpochMillis = mediaFile.capturedAtEpochMillis,
                    modifiedAtEpochMillis = mediaFile.modifiedAtEpochMillis,
                )
            }
            val unsupportedFiles = result.data.unsupportedFiles.map { unsupportedFile ->
                PlannedMediaFile(
                    sourcePath = unsupportedFile.path,
                    fileName = unsupportedFile.fileName,
                    targetRelativePath = unsupportedFile.path,
                    sizeBytes = unsupportedFile.sizeBytes,
                    modifiedAtEpochMillis = unsupportedFile.modifiedAtEpochMillis,
                )
            }
            mediaFiles + unsupportedFiles
        }

        is AppResult.Error -> emptyList()
    }
}

private fun ScanSourceFolderProgress.toLibraryRefreshProgress(
    section: LibraryRefreshSection,
): LibraryRefreshProgress {
    return LibraryRefreshProgress(
        section = section,
        scannedFiles = scannedFiles,
        mediaFiles = mediaFiles,
        unsupportedFiles = unsupportedFiles,
    )
}
