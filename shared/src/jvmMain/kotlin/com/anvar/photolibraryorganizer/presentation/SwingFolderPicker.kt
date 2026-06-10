package com.anvar.photolibraryorganizer.presentation

import java.io.File
import javax.swing.JFileChooser

class SwingFolderPicker : FolderPicker {
    override fun chooseFolder(title: String): String? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
            currentDirectory = File(System.getProperty("user.home"))
        }

        return when (chooser.showOpenDialog(null)) {
            JFileChooser.APPROVE_OPTION -> chooser.selectedFile.absolutePath
            else -> null
        }
    }
}
