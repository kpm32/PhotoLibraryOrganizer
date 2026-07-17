package com.anvar.photolibraryorganizer.data.filesystem

import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Platform-specific primitive for moving a single file to the OS Trash.
 *
 * Higher-level repositories validate whether a file belongs to `Duplicates`,
 * `Unsupported`, or the selected library view. This mover only performs the
 * final platform operation.
 */
interface TrashFileMover {
    fun moveToTrash(path: Path): Boolean
}

/**
 * Chooses the safest available Trash implementation for the current desktop OS.
 */
object SystemTrashFileMover : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        return if (isMacOs()) {
            MacOsFinderTrashFileMover.moveToTrash(path)
        } else {
            DesktopTrashFileMover.moveToTrash(path)
        }
    }

    private fun isMacOs(): Boolean {
        return System.getProperty("os.name")
            .lowercase(Locale.ENGLISH)
            .contains("mac")
    }
}

/**
 * Java Desktop fallback for non-macOS platforms supported by the current JRE.
 */
object DesktopTrashFileMover : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        if (!Desktop.isDesktopSupported()) return false

        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) return false

        return desktop.moveToTrash(path.toFile())
    }
}

/**
 * macOS Trash implementation backed by Finder.
 *
 * Direct writes into `.Trash` or `.Trashes` are intentionally avoided because
 * external drives can block or behave differently depending on volume settings.
 * The operation returns success as soon as the source file disappears, even if
 * AppleScript itself is still winding down.
 */
object MacOsFinderTrashFileMover : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        if (!isMacOs()) return false

        return try {
            val process = ProcessBuilder(
                "/usr/bin/osascript",
                "-e",
                "tell application \"Finder\" to delete POSIX file \"${path.toString().toAppleScriptString()}\"",
            )
                .redirectErrorStream(true)
                .start()

            process.waitForExitOrMoved(path)
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

    private fun Process.waitForExitOrMoved(path: Path): Boolean {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(FINDER_TIMEOUT_SECONDS)
        while (isAlive && System.nanoTime() < deadline) {
            if (!Files.exists(path)) {
                destroyForcibly()
                return true
            }
            Thread.sleep(FINDER_POLL_INTERVAL_MILLIS)
        }

        if (isAlive) {
            destroyForcibly()
            return !Files.exists(path)
        }

        return exitValue() == 0 || !Files.exists(path)
    }

    private const val FINDER_TIMEOUT_SECONDS = 5L
    private const val FINDER_POLL_INTERVAL_MILLIS = 100L
}
