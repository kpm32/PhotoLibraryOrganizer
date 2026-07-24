package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoLibraryAppStateTest {

    @Test
    fun keepsSelectionWhenApplyingUpdatedLibraryIndex() {
        val selected = fakePlannedMediaFile().copy(sourcePath = "/library/selected.jpg")
        val other = fakePlannedMediaFile().copy(sourcePath = "/library/other.jpg")
        val state = PhotoLibraryAppState().apply {
            selectedFile = selected
        }

        state.applyLibraryIndexSnapshot(
            snapshot = fakeLibraryIndexSnapshot(libraryFiles = listOf(other, selected)),
            selectFirstFile = false,
        )

        assertEquals(selected, state.selectedFile)
    }

    @Test
    fun removesFileAndSelectsNextPreferredFile() {
        val removed = fakePlannedMediaFile().copy(sourcePath = "/library/removed.jpg")
        val next = fakePlannedMediaFile().copy(sourcePath = "/library/next.jpg")
        val state = PhotoLibraryAppState().apply {
            libraryFiles = listOf(removed, next)
            selectedFile = removed
        }

        state.removeFileFromVisibleState(
            path = removed.sourcePath,
            preferredSelectionFiles = listOf(removed, next),
        )

        assertEquals(listOf(next), state.libraryFiles)
        assertEquals(next, state.selectedFile)
    }

    private fun fakePlannedMediaFile(): PlannedMediaFile {
        return PlannedMediaFile(
            sourcePath = "/source/IMG_0001.JPG",
            fileName = "IMG_0001.JPG",
            targetRelativePath = "/library/Library/2025/2025-01/IMG_0001.JPG",
            sizeBytes = 1024,
        )
    }

    private fun fakeLibraryIndexSnapshot(
        libraryFiles: List<PlannedMediaFile> = emptyList(),
        duplicateFiles: List<PlannedMediaFile> = emptyList(),
        unsupportedFiles: List<PlannedMediaFile> = emptyList(),
    ): LibraryIndexSnapshot {
        return LibraryIndexSnapshot(
            destinationFolder = "/library",
            libraryFiles = libraryFiles,
            duplicateFiles = duplicateFiles,
            unsupportedFiles = unsupportedFiles,
            updatedAtEpochMillis = 0,
        )
    }
}
