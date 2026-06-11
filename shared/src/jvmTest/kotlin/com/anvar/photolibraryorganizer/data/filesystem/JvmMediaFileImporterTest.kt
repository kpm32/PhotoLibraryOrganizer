package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JvmMediaFileImporterTest {
    private val importer = JvmMediaFileImporter()

    @Test
    fun copiesFilesAndCreatesTargetDirectories() = runTest {
        val tempFolder = Files.createTempDirectory("photo-import-test")
        val sourceFile = tempFolder.resolve("source.jpg")
        val targetFile = tempFolder.resolve("library/Library/2025/2025-01/source.jpg")
        sourceFile.writeText("photo")

        val result = importer.copyFiles(
            listOf(
                PlannedMediaFile(
                    sourcePath = sourceFile.toString(),
                    fileName = "source.jpg",
                    targetRelativePath = targetFile.toString(),
                    sizeBytes = 5,
                ),
            ),
        )

        val success = assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(1, success.data.copiedFiles)
        assertEquals(0, success.data.skippedFiles)
        assertEquals(0, success.data.failedFiles)
        assertTrue(targetFile.exists())
        assertEquals("photo", targetFile.readText())
    }

    @Test
    fun reportsProgressWhileCopyingFiles() = runTest {
        val tempFolder = Files.createTempDirectory("photo-import-progress-test")
        val firstSourceFile = tempFolder.resolve("first.jpg")
        val secondSourceFile = tempFolder.resolve("second.jpg")
        val firstTargetFile = tempFolder.resolve("library/first.jpg")
        val secondTargetFile = tempFolder.resolve("library/second.jpg")
        firstSourceFile.writeText("first")
        secondSourceFile.writeText("second")
        val processedCounts = mutableListOf<Int>()

        val result = importer.copyFiles(
            listOf(
                PlannedMediaFile(
                    sourcePath = firstSourceFile.toString(),
                    fileName = "first.jpg",
                    targetRelativePath = firstTargetFile.toString(),
                    sizeBytes = 5,
                ),
                PlannedMediaFile(
                    sourcePath = secondSourceFile.toString(),
                    fileName = "second.jpg",
                    targetRelativePath = secondTargetFile.toString(),
                    sizeBytes = 6,
                ),
            ),
        ) { progress ->
            processedCounts += progress.processedFiles
        }

        val success = assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(2, success.data.copiedFiles)
        assertEquals(listOf(1, 2), processedCounts)
    }

    @Test
    fun skipsExistingTargetFilesWithoutOverwritingThem() = runTest {
        val tempFolder = Files.createTempDirectory("photo-import-existing-test")
        val sourceFile = tempFolder.resolve("source.jpg")
        val targetFile = tempFolder.resolve("target.jpg")
        sourceFile.writeText("new")
        targetFile.writeText("existing")

        val result = importer.copyFiles(
            listOf(
                PlannedMediaFile(
                    sourcePath = sourceFile.toString(),
                    fileName = "source.jpg",
                    targetRelativePath = targetFile.toString(),
                    sizeBytes = 3,
                ),
            ),
        )

        val success = assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(0, success.data.copiedFiles)
        assertEquals(1, success.data.skippedFiles)
        assertEquals("existing", targetFile.readText())
    }

    @Test
    fun movesFilesAndRemovesSource() = runTest {
        val tempFolder = Files.createTempDirectory("photo-move-test")
        val sourceFile = tempFolder.resolve("source.jpg")
        val targetFile = tempFolder.resolve("library/Library/2025/2025-01/source.jpg")
        sourceFile.writeText("photo")

        val result = importer.moveFiles(
            listOf(
                PlannedMediaFile(
                    sourcePath = sourceFile.toString(),
                    fileName = "source.jpg",
                    targetRelativePath = targetFile.toString(),
                    sizeBytes = 5,
                ),
            ),
        )

        val success = assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(0, success.data.copiedFiles)
        assertEquals(1, success.data.movedFiles)
        assertEquals(0, success.data.skippedFiles)
        assertEquals(0, success.data.failedFiles)
        assertFalse(sourceFile.exists())
        assertTrue(targetFile.exists())
        assertEquals("photo", targetFile.readText())
    }

    @Test
    fun reportsFailedFileWhenCopiedTargetFailsVerification() = runTest {
        val importer = JvmMediaFileImporter(
            importedFileVerifier = ImportedFileVerifier { _, _, _, _ -> false },
        )
        val tempFolder = Files.createTempDirectory("photo-import-verification-test")
        val sourceFile = tempFolder.resolve("source.jpg")
        val targetFile = tempFolder.resolve("library/source.jpg")
        sourceFile.writeText("photo")

        val result = importer.copyFiles(
            listOf(
                PlannedMediaFile(
                    sourcePath = sourceFile.toString(),
                    fileName = "source.jpg",
                    targetRelativePath = targetFile.toString(),
                    sizeBytes = 5,
                ),
            ),
        )

        val success = assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(0, success.data.copiedFiles)
        assertEquals(1, success.data.failedFiles)
        assertTrue(sourceFile.exists())
        assertFalse(targetFile.exists())
    }

    @Test
    fun continuesImportAfterSingleFileFailure() = runTest {
        val tempFolder = Files.createTempDirectory("photo-import-partial-failure-test")
        val firstSourceFile = tempFolder.resolve("missing.jpg")
        val secondSourceFile = tempFolder.resolve("second.jpg")
        val firstTargetFile = tempFolder.resolve("library/missing.jpg")
        val secondTargetFile = tempFolder.resolve("library/second.jpg")
        secondSourceFile.writeText("second")

        val result = importer.copyFiles(
            listOf(
                PlannedMediaFile(
                    sourcePath = firstSourceFile.toString(),
                    fileName = "missing.jpg",
                    targetRelativePath = firstTargetFile.toString(),
                    sizeBytes = 7,
                ),
                PlannedMediaFile(
                    sourcePath = secondSourceFile.toString(),
                    fileName = "second.jpg",
                    targetRelativePath = secondTargetFile.toString(),
                    sizeBytes = 6,
                ),
            ),
        )

        val success = assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(1, success.data.copiedFiles)
        assertEquals(1, success.data.failedFiles)
        assertFalse(firstTargetFile.exists())
        assertTrue(secondTargetFile.exists())
        assertEquals("second", secondTargetFile.readText())
    }
}
