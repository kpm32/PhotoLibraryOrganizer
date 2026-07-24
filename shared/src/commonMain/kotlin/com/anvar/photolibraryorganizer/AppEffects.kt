package com.anvar.photolibraryorganizer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.anvar.photolibraryorganizer.presentation.AppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.CachingImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.ImagePreviewUiState
import com.anvar.photolibraryorganizer.presentation.ImportHistoryStorage

/**
 * Loads persisted settings and cached library data when the app starts.
 */
@Composable
internal fun RestorePersistedAppStateEffect(
    appState: PhotoLibraryAppState,
    appSettingsStorage: AppSettingsStorage,
    importHistoryStorage: ImportHistoryStorage,
    loadLibraryIndexIfAvailable: suspend (String?) -> Boolean,
) {
    LaunchedEffect(Unit) {
        val settings = appSettingsStorage.loadSettings()
        appState.importHistory = importHistoryStorage.loadHistory()
        appState.sourceFolder = settings.sourceFolder
        appState.destinationFolder = settings.destinationFolder
        appState.importRules = settings.importRules
        loadLibraryIndexIfAvailable(settings.destinationFolder)
    }
}

/**
 * Keeps the inspector preview synchronized with the currently selected file.
 */
@Composable
internal fun SelectedFilePreviewEffect(
    appState: PhotoLibraryAppState,
    imagePreviewLoader: CachingImagePreviewLoader,
) {
    LaunchedEffect(appState.selectedFile) {
        val file = appState.selectedFile
        appState.imagePreviewUiState = if (file == null) {
            ImagePreviewUiState.Empty
        } else {
            appState.imagePreviewUiState = ImagePreviewUiState.Loading
            imagePreviewLoader.loadImage(file.sourcePath)?.let { image ->
                ImagePreviewUiState.Success(image)
            } ?: ImagePreviewUiState.Unsupported
        }
    }
}
