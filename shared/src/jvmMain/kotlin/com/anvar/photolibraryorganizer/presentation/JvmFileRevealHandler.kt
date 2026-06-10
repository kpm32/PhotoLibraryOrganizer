package com.anvar.photolibraryorganizer.presentation

import java.awt.Desktop
import java.io.File
import java.util.Locale

class JvmFileRevealHandler : FileRevealHandler {
    override fun reveal(path: String): Boolean {
        return try {
            val file = File(path)
            if (!file.exists()) return false

            if (isMacOs()) {
                ProcessBuilder("open", "-R", file.absolutePath).start()
                return true
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.parentFile ?: file)
                true
            } else {
                false
            }
        } catch (exception: Throwable) {
            false
        }
    }

    private fun isMacOs(): Boolean {
        return System.getProperty("os.name")
            .lowercase(Locale.ENGLISH)
            .contains("mac")
    }
}
