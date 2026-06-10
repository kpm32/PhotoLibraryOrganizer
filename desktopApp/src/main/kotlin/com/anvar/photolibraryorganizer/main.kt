package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.data.filesystem.JvmMediaFileImporter
import com.anvar.photolibraryorganizer.data.filesystem.JvmPhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.JvmImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.NativeFolderPicker
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")

    application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PhotoLibraryOrganizer",
    ) {
        App(
            photoSourceScanner = JvmPhotoSourceScanner(),
            mediaFileImporter = JvmMediaFileImporter(),
            imagePreviewLoader = JvmImagePreviewLoader(),
            folderPicker = NativeFolderPicker(),
        )
    }
    }
}
