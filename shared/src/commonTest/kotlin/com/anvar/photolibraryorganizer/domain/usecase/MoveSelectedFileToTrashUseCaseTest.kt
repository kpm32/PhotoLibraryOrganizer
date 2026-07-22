package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.model.MoveSelectedFileToTrashResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.FileTrashRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MoveSelectedFileToTrashUseCaseTest {
    @Test
    fun returnsFileNotSelectedWhenSelectionIsEmpty() = runTest {
        val useCase = MoveSelectedFileToTrashUseCase(FakeFileTrashRepository())

        val result = useCase(
            selectedFile = null,
            isSourceFileActionAllowed = true,
        )

        assertEquals(MoveSelectedFileToTrashResult.FileNotSelected, result)
    }

    @Test
    fun blocksSourceFileActionsWhenCurrentSectionIsUnsafe() = runTest {
        val repository = FakeFileTrashRepository()
        val useCase = MoveSelectedFileToTrashUseCase(repository)

        val result = useCase(
            selectedFile = plannedFile(),
            isSourceFileActionAllowed = false,
        )

        assertEquals(MoveSelectedFileToTrashResult.SourceFileActionNotAllowed, result)
        assertEquals(emptyList(), repository.paths)
    }

    @Test
    fun movesSelectedFileToTrash() = runTest {
        val repository = FakeFileTrashRepository()
        val useCase = MoveSelectedFileToTrashUseCase(repository)

        val result = useCase(
            selectedFile = plannedFile(),
            isSourceFileActionAllowed = true,
        )

        val moved = assertIs<MoveSelectedFileToTrashResult.Moved>(result)
        assertEquals("/library/2024/photo.jpg", moved.path)
        assertEquals(listOf("/library/2024/photo.jpg"), repository.paths)
    }

    @Test
    fun reportsTimeoutWhenRepositoryDoesNotReturnSoonEnough() = runTest {
        val useCase = MoveSelectedFileToTrashUseCase(
            fileTrashRepository = FakeFileTrashRepository(delayMillis = 1_000),
            timeoutMillis = 10,
        )

        val result = useCase(
            selectedFile = plannedFile(),
            isSourceFileActionAllowed = true,
        )

        assertEquals(MoveSelectedFileToTrashResult.TimedOut, result)
    }

    @Test
    fun mapsRepositoryFailure() = runTest {
        val useCase = MoveSelectedFileToTrashUseCase(
            fileTrashRepository = FakeFileTrashRepository(result = false),
        )

        val result = useCase(
            selectedFile = plannedFile(),
            isSourceFileActionAllowed = true,
        )

        assertEquals(MoveSelectedFileToTrashResult.Failed, result)
    }

    @Test
    fun mapsRepositoryException() = runTest {
        val useCase = MoveSelectedFileToTrashUseCase(
            fileTrashRepository = FakeFileTrashRepository(exceptionMessage = "disk denied"),
        )

        val result = useCase(
            selectedFile = plannedFile(),
            isSourceFileActionAllowed = true,
        )

        val error = assertIs<MoveSelectedFileToTrashResult.Error>(result)
        assertEquals("disk denied", error.message)
    }

    private fun plannedFile(): PlannedMediaFile {
        return PlannedMediaFile(
            sourcePath = "/library/2024/photo.jpg",
            fileName = "photo.jpg",
            targetRelativePath = "Library/2024/2024-04/photo.jpg",
            sizeBytes = 42,
        )
    }

    private class FakeFileTrashRepository(
        private val result: Boolean = true,
        private val delayMillis: Long = 0,
        private val exceptionMessage: String? = null,
    ) : FileTrashRepository {
        val paths = mutableListOf<String>()

        override suspend fun moveToTrash(path: String): Boolean {
            paths.add(path)
            if (delayMillis > 0) delay(delayMillis)
            exceptionMessage?.let { throw IllegalStateException(it) }
            return result
        }
    }
}
