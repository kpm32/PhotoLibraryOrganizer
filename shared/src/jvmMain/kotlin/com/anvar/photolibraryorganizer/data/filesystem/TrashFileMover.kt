package com.anvar.photolibraryorganizer.data.filesystem

import java.awt.Desktop
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit

interface TrashFileMover {
    fun moveToTrash(path: Path): Boolean
}

object SystemTrashFileMover : TrashFileMover {
    private val mover = FallbackTrashFileMover(
        primary = DesktopTrashFileMover,
        fallback = MacOsFinderTrashFileMover,
    )

    override fun moveToTrash(path: Path): Boolean = mover.moveToTrash(path)
}

class FallbackTrashFileMover(
    private val primary: TrashFileMover,
    private val fallback: TrashFileMover,
) : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        return primary.moveToTrash(path) || fallback.moveToTrash(path)
    }
}

object DesktopTrashFileMover : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        if (!Desktop.isDesktopSupported()) return false

        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) return false

        return desktop.moveToTrash(path.toFile())
    }
}

object MacOsFinderTrashFileMover : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        if (!isMacOs()) return false

        return try {
            val process = ProcessBuilder(
                "osascript",
                "-e",
                "tell application \"Finder\" to delete POSIX file \"${path.toString().toAppleScriptString()}\"",
            )
                .redirectErrorStream(true)
                .start()

            if (!process.waitFor(FINDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return false
            }

            process.exitValue() == 0
        } catch (exception: Throwable) {
            false
        }
    }

    private fun isMacOs(): Boolean {
        return System.getProperty("os.name")
            .lowercase(Locale.ENGLISH)
            .contains("mac")
    }

    private fun String.toAppleScriptString(): String {
        return replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private const val FINDER_TIMEOUT_SECONDS = 10L
}
