package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JvmLibraryIndexStorageTest {
    @Test
    fun savesAndLoadsSnapshotForDestination() = runTest {
        val tempFolder = Files.createTempDirectory("library-index-test")
        val indexPath = tempFolder.resolve("library-index.tsv")
        val storage = JvmLibraryIndexStorage(indexPath)
        val destination = tempFolder.resolve("archive").toString()
        val libraryFile = PlannedMediaFile(
            sourcePath = "$destination/Library/2026/2026-07/photo one.jpg",
            fileName = "photo one.jpg",
            targetRelativePath = "$destination/Library/2026/2026-07/photo one.jpg",
            sizeBytes = 42,
            contentHash = "abc123",
            capturedAtEpochMillis = 1_700_000_000_000,
            modifiedAtEpochMillis = 1_700_000_100_000,
        )

        storage.save(
            LibraryIndexSnapshot(
                destinationFolder = destination,
                libraryFiles = listOf(libraryFile),
                duplicateFiles = emptyList(),
                unsupportedFiles = emptyList(),
                updatedAtEpochMillis = 1_800_000_000_000,
            ),
        )

        val result = storage.load(destination)

        assertEquals(destination, result?.destinationFolder)
        assertEquals(1_800_000_000_000, result?.updatedAtEpochMillis)
        assertEquals(listOf(libraryFile), result?.libraryFiles)
        assertEquals(emptyList(), result?.duplicateFiles)
        assertEquals(emptyList(), result?.unsupportedFiles)
    }

    @Test
    fun returnsNullForAnotherDestination() = runTest {
        val tempFolder = Files.createTempDirectory("library-index-destination-test")
        val storage = JvmLibraryIndexStorage(tempFolder.resolve("library-index.tsv"))

        storage.save(
            LibraryIndexSnapshot(
                destinationFolder = tempFolder.resolve("archive-a").toString(),
                libraryFiles = emptyList(),
                duplicateFiles = emptyList(),
                unsupportedFiles = emptyList(),
                updatedAtEpochMillis = 1,
            ),
        )

        assertNull(storage.load(tempFolder.resolve("archive-b").toString()))
    }
}
