package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.data.filesystem.JvmMediaFileImporter
import com.anvar.photolibraryorganizer.data.filesystem.JvmPhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.SwingFolderPicker
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "PhotoLibraryOrganizer",
    ) {
        App(
            photoSourceScanner = JvmPhotoSourceScanner(),
            mediaFileImporter = JvmMediaFileImporter(),
            folderPicker = SwingFolderPicker(),
        )
    }
}
