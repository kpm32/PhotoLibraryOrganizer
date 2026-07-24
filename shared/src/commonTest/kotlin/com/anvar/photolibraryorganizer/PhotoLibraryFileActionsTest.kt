package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.presentation.FileRevealHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoLibraryFileActionsTest {

    @Test
    fun openLibrarySubfolderThroughRevealHandler() {
        val revealHandler = FakeFileRevealHandler()
        val issues = mutableListOf<Pair<String, String>>()
        val actions = PhotoLibraryFileActions(
            fileRevealHandler = revealHandler,
            addIssue = { title, detail -> issues += title to detail },
        )

        actions.openDuplicatesFolder("/library-root/")

        assertEquals("/library-root/Duplicates", revealHandler.lastOpenedPath)
        assertEquals(emptyList(), issues)
    }

    @Test
    fun reportOpenFailureAsIssue() {
        val revealHandler = FakeFileRevealHandler(openResult = false)
        val issues = mutableListOf<Pair<String, String>>()
        val actions = PhotoLibraryFileActions(
            fileRevealHandler = revealHandler,
            addIssue = { title, detail -> issues += title to detail },
        )

        actions.openSelectedFile(fakePlannedMediaFile().copy(sourcePath = "/library/missing.jpg"))

        assertEquals("/library/missing.jpg", revealHandler.lastOpenedPath)
        assertEquals(1, issues.size)
        assertTrue(issues.first().second.contains("/library/missing.jpg"))
    }

    private fun fakePlannedMediaFile(): PlannedMediaFile {
        return PlannedMediaFile(
            sourcePath = "/source/IMG_0001.JPG",
            fileName = "IMG_0001.JPG",
            targetRelativePath = "/library/Library/2025/2025-01/IMG_0001.JPG",
            sizeBytes = 1024,
        )
    }

    private class FakeFileRevealHandler(
        private val openResult: Boolean = true,
        private val revealResult: Boolean = true,
    ) : FileRevealHandler {
        var lastOpenedPath: String? = null
        var lastRevealedPath: String? = null

        override fun open(path: String): Boolean {
            lastOpenedPath = path
            return openResult
        }

        override fun reveal(path: String): Boolean {
            lastRevealedPath = path
            return revealResult
        }
    }
}
