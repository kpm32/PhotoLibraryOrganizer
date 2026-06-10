package com.anvar.photolibraryorganizer

import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.data.filesystem.JvmDuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.data.filesystem.JvmImportPlanTargetResolver
import com.anvar.photolibraryorganizer.data.filesystem.JvmMediaFileImporter
import com.anvar.photolibraryorganizer.data.filesystem.JvmPhotoSourceScanner
import com.anvar.photolibraryorganizer.presentation.JvmAppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.JvmImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.NativeFolderPicker
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "PhotoLibraryOrganizer",
            state = WindowState(width = 1320.dp, height = 860.dp),
        ) {
            App(
                photoSourceScanner = JvmPhotoSourceScanner(),
                mediaFileImporter = JvmMediaFileImporter(),
                importPlanTargetResolver = JvmImportPlanTargetResolver(),
                duplicateQuarantineRepository = JvmDuplicateQuarantineRepository(),
                imagePreviewLoader = JvmImagePreviewLoader(),
                appSettingsStorage = JvmAppSettingsStorage(),
                folderPicker = NativeFolderPicker(),
            )
        }
    }
}
