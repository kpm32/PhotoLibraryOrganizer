package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.model.ScannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.FileTrashRepository
import com.anvar.photolibraryorganizer.domain.repository.LibraryIndexStorage
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.CheckImportStorageSpaceUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ImportMediaFilesUseCase
import com.anvar.photolibraryorganizer.domain.usecase.MoveSelectedFileToTrashUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase
import com.anvar.photolibraryorganizer.presentation.AppSection
import com.anvar.photolibraryorganizer.presentation.AppSettings
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SelectedFileTrashActionsTest {

    @Test
    fun firstRequestOnlyAsksForConfirmation() = runTest {
        val file = plannedFile()
        val trashRepository = FakeFileTrashRepository()
        val state = PhotoLibraryAppState().apply {
            selectedFile = file
            libraryFiles = listOf(file)
        }
        val actions = selectedFileTrashActions(state, trashRepository, this, StandardTestDispatcher(testScheduler))

        actions.requestMoveSelectedFileToTrash(listOf(file))

        assertTrue(state.selectedFileTrashAwaitingConfirmation)
        assertFalse(state.selectedFileTrashInProgress)
        assertEquals(emptyList(), trashRepository.paths)
        assertEquals(file, state.selectedFile)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun confirmedRequestMovesFileAndRemovesItFromVisibleState() = runTest {
        val removed = plannedFile()
        val next = plannedFile().copy(sourcePath = "/library/next.jpg", fileName = "next.jpg")
        val trashRepository = FakeFileTrashRepository()
        val state = PhotoLibraryAppState().apply {
            destinationFolder = "/library"
            selectedSection = AppSection.AllPhotos
            selectedFile = removed
            selectedFileTrashAwaitingConfirmation = true
            libraryFiles = listOf(removed, next)
        }
        val actions = selectedFileTrashActions(state, trashRepository, this, StandardTestDispatcher(testScheduler))

        actions.requestMoveSelectedFileToTrash(listOf(removed, next))
        advanceUntilIdle()

        assertEquals(listOf(removed.sourcePath), trashRepository.paths)
        assertFalse(state.selectedFileTrashAwaitingConfirmation)
        assertFalse(state.selectedFileTrashInProgress)
        assertEquals(listOf(next), state.libraryFiles)
        assertEquals(next, state.selectedFile)
    }

    @Test
    fun importSectionDoesNotMoveSourceFileToTrash() = runTest {
        val file = plannedFile()
        val trashRepository = FakeFileTrashRepository()
        val state = PhotoLibraryAppState().apply {
            selectedSection = AppSection.Import
            selectedFile = file
            libraryFiles = listOf(file)
        }
        val actions = selectedFileTrashActions(state, trashRepository, this, StandardTestDispatcher(testScheduler))

        actions.requestMoveSelectedFileToTrash(listOf(file))

        assertFalse(state.selectedFileTrashAwaitingConfirmation)
        assertEquals(emptyList(), trashRepository.paths)
        assertEquals(file, state.selectedFile)
    }

    private fun selectedFileTrashActions(
        state: PhotoLibraryAppState,
        trashRepository: FakeFileTrashRepository,
        coroutineScope: CoroutineScope,
        backgroundDispatcher: CoroutineDispatcher,
    ): SelectedFileTrashActions {
        val libraryActions = PhotoLibraryLibraryActions(
            appState = state,
            photoSourceScanner = FakePhotoSourceScanner(),
            libraryIndexStorage = FakeLibraryIndexStorage(),
            appSettingsStorage = FakeAppSettingsStorage(),
            folderPicker = FakeFolderPicker(),
            coroutineScope = coroutineScope,
        )
        return SelectedFileTrashActions(
            appState = state,
            useCases = PhotoLibraryUseCases(
                scanSourceFolder = ScanSourceFolderUseCase(FakePhotoSourceScanner()),
                importMediaFiles = ImportMediaFilesUseCase(FakeMediaFileImporter()),
                checkImportStorageSpace = CheckImportStorageSpaceUseCase { null },
                buildMediaFilePlan = BuildMediaFilePlanUseCase(),
                resolveImportAvailability = ResolveImportAvailabilityUseCase(),
                moveSelectedFileToTrash = MoveSelectedFileToTrashUseCase(trashRepository),
            ),
            libraryActions = libraryActions,
            coroutineScope = coroutineScope,
            backgroundDispatcher = backgroundDispatcher,
        )
    }

    private fun plannedFile(): PlannedMediaFile {
        return PlannedMediaFile(
            sourcePath = "/library/photo.jpg",
            fileName = "photo.jpg",
            targetRelativePath = "/library/Library/2024/2024-04/photo.jpg",
            sizeBytes = 42,
        )
    }

    private class FakeFileTrashRepository : FileTrashRepository {
        val paths = mutableListOf<String>()

        override suspend fun moveToTrash(path: String): Boolean {
            paths.add(path)
            return true
        }
    }

    private class FakePhotoSourceScanner : PhotoSourceScanner {
        override suspend fun scanFolder(
            path: String,
            onProgress: (ScanSourceFolderProgress) -> Unit,
            readContentHash: Boolean,
        ): AppResult<ScanSourceFolderResult> {
            return AppResult.Success(
                ScanSourceFolderResult(
                    sourceFolder = path,
                    mediaFiles = emptyList<ScannedMediaFile>(),
                    summary = ScanSourceFolderSummary(
                        scannedFiles = 0,
                        mediaFiles = 0,
                        imageFiles = 0,
                        videoFiles = 0,
                        capturedDateFiles = 0,
                        unsupportedFiles = 0,
                        totalMediaBytes = 0,
                    ),
                ),
            )
        }
    }

    private class FakeMediaFileImporter : MediaFileImporter {
        override suspend fun copyFiles(
            plannedFiles: List<PlannedMediaFile>,
            onProgress: (ImportMediaFilesProgress) -> Unit,
        ): AppResult<ImportMediaFilesResult> {
            return AppResult.Success(
                ImportMediaFilesResult(
                    copiedFiles = plannedFiles.size,
                    skippedFiles = 0,
                    failedFiles = 0,
                ),
            )
        }

        override suspend fun moveFiles(
            plannedFiles: List<PlannedMediaFile>,
            onProgress: (ImportMediaFilesProgress) -> Unit,
        ): AppResult<ImportMediaFilesResult> {
            return AppResult.Success(
                ImportMediaFilesResult(
                    copiedFiles = 0,
                    movedFiles = plannedFiles.size,
                    skippedFiles = 0,
                    failedFiles = 0,
                ),
            )
        }
    }

    private class FakeLibraryIndexStorage : LibraryIndexStorage {
        override suspend fun load(destinationFolder: String?): LibraryIndexSnapshot? = null

        override suspend fun save(snapshot: LibraryIndexSnapshot) = Unit
    }

    private class FakeAppSettingsStorage : AppSettingsStorage {
        override suspend fun loadSettings(): AppSettings = AppSettings()

        override suspend fun saveSettings(settings: AppSettings) = Unit
    }

    private class FakeFolderPicker : FolderPicker {
        override fun chooseFolder(title: String): String? = null
    }
}
