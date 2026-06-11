package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.EmptyFolderCleanupResult
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

class JvmEmptyFolderCleanupRepositoryTest {
    private val repository = JvmEmptyFolderCleanupRepository()

    @Test
    fun deletesOnlyEmptyNestedFoldersAndKeepsSourceRoot() = runTest {
        val sourceFolder = Files.createTempDirectory("empty-cleanup-test")
        val emptyNestedFolder = sourceFolder.resolve("phone/empty/deeper").apply { createDirectories() }
        val folderWithFile = sourceFolder.resolve("phone/keep").apply { createDirectories() }
        val fileToKeep = folderWithFile.resolve("note.txt").apply { writeText("keep") }

        val result = repository.deleteEmptyFolders(sourceFolder.toString())

        val success = assertIs<AppResult.Success<EmptyFolderCleanupResult>>(result)
        assertEquals(2, success.data.deletedFolders)
        assertEquals(0, success.data.failedFolders)
        assertTrue(sourceFolder.exists())
        assertFalse(emptyNestedFolder.exists())
        assertTrue(folderWithFile.exists())
        assertTrue(fileToKeep.exists())
    }
}
