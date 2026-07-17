package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineDeleteResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedSourceFile
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
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

    @Test
    fun movesOnlyFilesInsideUnsupportedQuarantineToTrash() = runTest {
        val tempFolder = Files.createTempDirectory("unsupported-delete-test")
        val destinationFolder = tempFolder.resolve("library")
        val quarantineFile = destinationFolder.resolve("Unsupported/db/thumb.db")
        val outsideFile = tempFolder.resolve("outside.db")
        val trashFileMover = FakeTrashFileMover()
        val repository = JvmUnsupportedFileQuarantineRepository(trashFileMover)
        quarantineFile.parent.createDirectories()
        quarantineFile.writeText("delete me")
        outsideFile.writeText("keep me")

        val result = repository.deleteFromQuarantine(
            destinationFolder = destinationFolder.toString(),
            quarantineFiles = listOf(
                PlannedMediaFile(
                    sourcePath = quarantineFile.toString(),
                    fileName = "thumb.db",
                    targetRelativePath = quarantineFile.toString(),
                    sizeBytes = 9,
                ),
                PlannedMediaFile(
                    sourcePath = outsideFile.toString(),
                    fileName = "outside.db",
                    targetRelativePath = outsideFile.toString(),
                    sizeBytes = 7,
                ),
            ),
        )

        val success = assertIs<AppResult.Success<UnsupportedFileQuarantineDeleteResult>>(result)
        assertEquals(1, success.data.deletedFiles)
        assertEquals(1, success.data.failedFiles)
        assertEquals(listOf(quarantineFile.toAbsolutePath().normalize()), trashFileMover.movedPaths)
        assertFalse(quarantineFile.exists())
        assertTrue(outsideFile.exists())
        assertEquals("keep me", outsideFile.readText())
    }

    private class FakeTrashFileMover : TrashFileMover {
        val movedPaths = mutableListOf<Path>()

        override fun moveToTrash(path: Path): Boolean {
            movedPaths.add(path)
            Files.deleteIfExists(path)
            return true
        }
    }
}
