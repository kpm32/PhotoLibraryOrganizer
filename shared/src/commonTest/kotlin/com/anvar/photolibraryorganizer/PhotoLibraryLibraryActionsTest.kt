package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.repository.LibraryIndexStorage
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.AppSettings
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoLibraryLibraryActionsTest {

    @Test
    fun loadIndexAppliesSnapshot() = runTest {
        val indexedFile = fakePlannedMediaFile().copy(sourcePath = "/library/indexed.jpg")
        val state = PhotoLibraryAppState()
        val actions = fakeLibraryActions(
            state = state,
            coroutineScope = this,
            libraryIndexStorage = FakeLibraryIndexStorage(
                snapshot = fakeLibraryIndexSnapshot(libraryFiles = listOf(indexedFile)),
            ),
        )

        val loaded = actions.loadLibraryIndexIfAvailable("/library")

        assertTrue(loaded)
        assertEquals(listOf(indexedFile), state.libraryFiles)
        assertEquals(indexedFile, state.selectedFile)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun chooseDestinationSavesSettingsAndLoadsIndex() = runTest {
        val indexedFile = fakePlannedMediaFile().copy(sourcePath = "/library/from-index.jpg")
        val state = PhotoLibraryAppState().apply {
            sourceFolder = "/source"
        }
        val settingsStorage = FakeAppSettingsStorage()
        val actions = fakeLibraryActions(
            state = state,
            coroutineScope = this,
            appSettingsStorage = settingsStorage,
            folderPicker = FakeFolderPicker("/library"),
            libraryIndexStorage = FakeLibraryIndexStorage(
                snapshot = fakeLibraryIndexSnapshot(libraryFiles = listOf(indexedFile)),
            ),
        )

        actions.chooseDestinationFolder()
        advanceUntilIdle()

        assertEquals("/library", state.destinationFolder)
        assertEquals("/library", settingsStorage.lastSavedSettings?.destinationFolder)
        assertEquals(listOf(indexedFile), state.libraryFiles)
    }

    private fun fakeLibraryActions(
        state: PhotoLibraryAppState = PhotoLibraryAppState(),
        coroutineScope: CoroutineScope,
        photoSourceScanner: PhotoSourceScanner = FakePhotoSourceScanner(),
        libraryIndexStorage: LibraryIndexStorage = FakeLibraryIndexStorage(),
        appSettingsStorage: AppSettingsStorage = FakeAppSettingsStorage(),
        folderPicker: FolderPicker = FakeFolderPicker(null),
    ): PhotoLibraryLibraryActions {
        return PhotoLibraryLibraryActions(
            appState = state,
            photoSourceScanner = photoSourceScanner,
            libraryIndexStorage = libraryIndexStorage,
            appSettingsStorage = appSettingsStorage,
            folderPicker = folderPicker,
            coroutineScope = coroutineScope,
        )
    }

    private fun fakePlannedMediaFile(): PlannedMediaFile {
        return PlannedMediaFile(
            sourcePath = "/source/IMG_0001.JPG",
            fileName = "IMG_0001.JPG",
            targetRelativePath = "/library/Library/2025/2025-01/IMG_0001.JPG",
            sizeBytes = 1024,
        )
    }

    private fun fakeLibraryIndexSnapshot(
        libraryFiles: List<PlannedMediaFile> = emptyList(),
        duplicateFiles: List<PlannedMediaFile> = emptyList(),
        unsupportedFiles: List<PlannedMediaFile> = emptyList(),
    ): LibraryIndexSnapshot {
        return LibraryIndexSnapshot(
            destinationFolder = "/library",
            libraryFiles = libraryFiles,
            duplicateFiles = duplicateFiles,
            unsupportedFiles = unsupportedFiles,
            updatedAtEpochMillis = 0,
        )
    }

    private class FakeFolderPicker(
        private val folder: String?,
    ) : FolderPicker {
        override fun chooseFolder(title: String): String? = folder
    }

    private class FakeAppSettingsStorage : AppSettingsStorage {
        var lastSavedSettings: AppSettings? = null

        override suspend fun loadSettings(): AppSettings = AppSettings()

        override suspend fun saveSettings(settings: AppSettings) {
            lastSavedSettings = settings
        }
    }

    private class FakeLibraryIndexStorage(
        private val snapshot: LibraryIndexSnapshot? = null,
    ) : LibraryIndexStorage {
        var lastLoadedDestination: String? = null
        var lastSavedSnapshot: LibraryIndexSnapshot? = null

        override suspend fun load(destinationFolder: String?): LibraryIndexSnapshot? {
            lastLoadedDestination = destinationFolder
            return snapshot
        }

        override suspend fun save(snapshot: LibraryIndexSnapshot) {
            lastSavedSnapshot = snapshot
        }
    }

    private class FakePhotoSourceScanner : PhotoSourceScanner {
        var lastScannedPath: String? = null

        override suspend fun scanFolder(
            path: String,
            onProgress: (ScanSourceFolderProgress) -> Unit,
            readContentHash: Boolean,
        ): AppResult<ScanSourceFolderResult> {
            lastScannedPath = path
            return AppResult.Success(
                ScanSourceFolderResult(
                    sourceFolder = path,
                    mediaFiles = emptyList(),
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
}
