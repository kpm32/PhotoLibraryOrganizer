package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.repository.LibraryIndexStorage
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.AppSettings
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.FolderPicker
import com.anvar.photolibraryorganizer.presentation.LibraryRefreshProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Coordinates library folder selection, persisted folder settings, and library
 * index refresh operations for the app shell.
 */
internal class PhotoLibraryLibraryActions(
    private val appState: PhotoLibraryAppState,
    private val photoSourceScanner: PhotoSourceScanner,
    private val libraryIndexStorage: LibraryIndexStorage,
    private val appSettingsStorage: AppSettingsStorage,
    private val folderPicker: FolderPicker,
    private val coroutineScope: CoroutineScope,
) {
    suspend fun loadLibraryIndexIfAvailable(
        destination: String?,
        selectFirstFile: Boolean = true,
    ): Boolean {
        val snapshot = libraryIndexStorage.load(destination) ?: return false
        appState.applyLibraryIndexSnapshot(snapshot, selectFirstFile)
        return true
    }

    suspend fun refreshLibraryIndexFromDisk(
        selectFirstFile: Boolean = true,
        onProgress: (LibraryRefreshProgress) -> Unit = {},
    ) {
        val snapshot = refreshLibraryIndexSnapshot(
            destinationFolder = appState.destinationFolder,
            photoSourceScanner = photoSourceScanner,
            libraryIndexStorage = libraryIndexStorage,
            onProgress = onProgress,
        )
        appState.applyLibraryIndexSnapshot(snapshot, selectFirstFile)
    }

    fun chooseSourceFolder() {
        folderPicker.chooseFolder(uiText("Выбери исходную папку", "Choose Source Folder"))?.let { folder ->
            appState.sourceFolder = folder
            saveCurrentSettings()
        }
        appState.resetAfterFolderSelection(clearLibraryFiles = true)
    }

    fun chooseDestinationFolder() {
        folderPicker.chooseFolder(uiText("Выбери папку библиотеки", "Choose Library Folder"))?.let { folder ->
            appState.destinationFolder = folder
            saveCurrentSettings()
        }
        appState.resetAfterFolderSelection(clearLibraryFiles = false)
        coroutineScope.launch {
            if (!loadLibraryIndexIfAvailable(appState.destinationFolder)) {
                appState.clearLibraryFileCollections()
            }
        }
    }

    fun startLibraryRefresh() {
        if (appState.isLibraryRefreshing) return

        appState.refreshLibraryJob = coroutineScope.launch {
            appState.isLibraryRefreshing = true
            appState.libraryRefreshProgress = null
            try {
                refreshLibraryIndexFromDisk { progress ->
                    coroutineScope.launch {
                        if (appState.isLibraryRefreshing) {
                            appState.libraryRefreshProgress = progress
                        }
                    }
                }
            } catch (exception: CancellationException) {
                appState.libraryRefreshProgress = null
            } finally {
                appState.isLibraryRefreshing = false
                appState.refreshLibraryJob = null
            }
        }
    }

    fun cancelLibraryRefresh() {
        appState.refreshLibraryJob?.cancel()
        appState.refreshLibraryJob = null
        appState.isLibraryRefreshing = false
        appState.libraryRefreshProgress = null
    }

    fun saveCurrentSettings() {
        coroutineScope.launch {
            appSettingsStorage.saveSettings(
                AppSettings(
                    sourceFolder = appState.sourceFolder,
                    destinationFolder = appState.destinationFolder,
                    importRules = appState.importRules,
                ),
            )
        }
    }
}
