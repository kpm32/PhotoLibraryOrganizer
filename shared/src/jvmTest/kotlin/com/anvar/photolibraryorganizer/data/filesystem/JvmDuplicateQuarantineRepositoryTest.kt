package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JvmDuplicateQuarantineRepositoryTest {
    private val repository = JvmDuplicateQuarantineRepository()

    @Test
    fun movesDuplicatesToQuarantineFolder() = runTest {
        val destination = Files.createTempDirectory("duplicate-quarantine-test")
        val duplicate = destination.resolve("Library/2026/2026-06/photo-copy.jpg")
        duplicate.parent.createDirectories()
        duplicate.writeText("same-content")

        val result = repository.moveToQuarantine(
            destinationFolder = destination.toString(),
            duplicateFiles = listOf(
                PlannedMediaFile(
                    sourcePath = duplicate.toString(),
                    fileName = duplicate.fileName.toString(),
                    targetRelativePath = duplicate.toString(),
                    sizeBytes = 12,
                    contentHash = "abcdef1234567890",
                ),
            ),
        )

        val success = assertIs<AppResult.Success<DuplicateQuarantineResult>>(result)
        val quarantineFile = destination.resolve("Duplicates/abcdef123456/photo-copy.jpg")
        assertEquals(1, success.data.movedFiles)
        assertEquals(0, success.data.failedFiles)
        assertFalse(duplicate.exists())
        assertTrue(quarantineFile.exists())
    }
}
