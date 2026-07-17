package com.anvar.photolibraryorganizer.data.filesystem

import java.awt.Desktop
import java.nio.file.Path

interface TrashFileMover {
    fun moveToTrash(path: Path): Boolean
}

object DesktopTrashFileMover : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        if (!Desktop.isDesktopSupported()) return false

        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) return false

        return desktop.moveToTrash(path.toFile())
    }
}
