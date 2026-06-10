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
}
