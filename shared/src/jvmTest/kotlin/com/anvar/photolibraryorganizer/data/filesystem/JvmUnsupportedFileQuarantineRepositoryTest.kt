package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedSourceFile
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JvmUnsupportedFileQuarantineRepositoryTest {
    private val repository = JvmUnsupportedFileQuarantineRepository()

    @Test
    fun movesUnsupportedFilesToQuarantinePreservingRelativeFolders() = runTest {
        val tempFolder = Files.createTempDirectory("unsupported-quarantine-test")
        val sourceRoot = tempFolder.resolve("incoming")
        val nestedFolder = sourceRoot.resolve("phone/cache").apply { createDirectories() }
        val sourceFile = nestedFolder.resolve("thumb.db")
        val destinationFolder = tempFolder.resolve("library")
        sourceFile.writeText("cache")

        val result = repository.moveToQuarantine(
            destinationFolder = destinationFolder.toString(),
            unsupportedFiles = listOf(
                UnsupportedSourceFile(
                    path = sourceFile.toString(),
                    relativePath = "phone/cache/thumb.db",
                    fileName = "thumb.db",
                    extensionLabel = "db",
                    sizeBytes = 5,
                    modifiedAtEpochMillis = 0,
                ),
            ),
        )

        val success = assertIs<AppResult.Success<UnsupportedFileQuarantineResult>>(result)
        val targetFile = destinationFolder.resolve("Unsupported/db/phone/cache/thumb.db")
        assertEquals(1, success.data.movedFiles)
        assertEquals(0, success.data.failedFiles)
        assertFalse(sourceFile.exists())
        assertTrue(targetFile.exists())
        assertEquals("cache", targetFile.readText())
    }
}
