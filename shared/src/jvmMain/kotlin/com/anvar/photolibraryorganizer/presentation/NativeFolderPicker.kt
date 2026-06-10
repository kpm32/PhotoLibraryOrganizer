package com.anvar.photolibraryorganizer.presentation

import java.awt.FileDialog
import java.awt.Frame

class NativeFolderPicker : FolderPicker {
    override fun chooseFolder(title: String): String? {
        val previousDirectoryMode = System.getProperty(DIRECTORY_MODE_PROPERTY)
        System.setProperty(DIRECTORY_MODE_PROPERTY, "true")

        return try {
            val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
                isMultipleMode = false
                isVisible = true
            }

            val directory = dialog.directory ?: return null
            val file = dialog.file ?: return null

            directory + file
        } finally {
            if (previousDirectoryMode == null) {
                System.clearProperty(DIRECTORY_MODE_PROPERTY)
            } else {
                System.setProperty(DIRECTORY_MODE_PROPERTY, previousDirectoryMode)
            }
        }
    }

    private companion object {
        const val DIRECTORY_MODE_PROPERTY = "apple.awt.fileDialogForDirectories"
    }
}
