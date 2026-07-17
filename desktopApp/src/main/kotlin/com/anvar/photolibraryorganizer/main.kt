package com.anvar.photolibraryorganizer

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.data.filesystem.JvmEmptyFolderCleanupRepository
import com.anvar.photolibraryorganizer.data.filesystem.JvmDuplicateQuarantineRepository
import com.anvar.photolibraryorganizer.data.filesystem.JvmImportPlanTargetResolver
import com.anvar.photolibraryorganizer.data.filesystem.JvmMediaFileImporter
import com.anvar.photolibraryorganizer.data.filesystem.JvmPhotoSourceScanner
import com.anvar.photolibraryorganizer.data.filesystem.JvmStorageSpaceProvider
import com.anvar.photolibraryorganizer.data.filesystem.JvmUnsupportedFileQuarantineRepository
import com.anvar.photolibraryorganizer.presentation.JvmAppSettingsStorage
import com.anvar.photolibraryorganizer.presentation.JvmFileRevealHandler
import com.anvar.photolibraryorganizer.presentation.JvmImagePreviewLoader
import com.anvar.photolibraryorganizer.presentation.JvmImportHistoryStorage
import com.anvar.photolibraryorganizer.presentation.NativeFolderPicker
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import java.awt.Dimension

fun main() {
    System.setProperty("apple.awt.application.appearance", "system")

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Photo Library Organizer",
            state = WindowState(width = 1500.dp, height = 940.dp),
        ) {
            LaunchedEffect(Unit) {
                window.minimumSize = Dimension(1180, 760)
            }
            App(
                photoSourceScanner = JvmPhotoSourceScanner(),
                mediaFileImporter = JvmMediaFileImporter(),
                importPlanTargetResolver = JvmImportPlanTargetResolver(),
                storageSpaceProvider = JvmStorageSpaceProvider(),
                duplicateQuarantineRepository = JvmDuplicateQuarantineRepository(),
                unsupportedFileQuarantineRepository = JvmUnsupportedFileQuarantineRepository(),
                emptyFolderCleanupRepository = JvmEmptyFolderCleanupRepository(),
                imagePreviewLoader = JvmImagePreviewLoader(),
                appSettingsStorage = JvmAppSettingsStorage(),
                importHistoryStorage = JvmImportHistoryStorage(),
                folderPicker = NativeFolderPicker(),
                fileRevealHandler = JvmFileRevealHandler(),
            )
        }
    }
}
