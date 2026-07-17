package com.anvar.photolibraryorganizer.data.filesystem

import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name

interface TrashFileMover {
    fun moveToTrash(path: Path): Boolean
}

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

class FallbackTrashFileMover(
    private val primary: TrashFileMover,
    private val fallback: TrashFileMover,
) : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        return primary.moveToTrash(path) || fallback.moveToTrash(path)
    }
}

class ChainTrashFileMover(
    private vararg val movers: TrashFileMover,
) : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        return movers.any { mover -> mover.moveToTrash(path) }
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

object FileSystemMacTrashFileMover : TrashFileMover {
    override fun moveToTrash(path: Path): Boolean {
        if (!isMacOs()) return false

        return try {
            val sourcePath = path.toAbsolutePath().normalize()
            val trashFolder = sourcePath.trashFolderForPath() ?: return false
            trashFolder.createDirectories()
            Files.move(sourcePath, trashFolder.uniqueTargetFor(sourcePath.name))
            true
        } catch (exception: Throwable) {
            false
        }
    }

    private fun Path.trashFolderForPath(): Path? {
        val normalized = toAbsolutePath().normalize()
        val home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize()
        if (normalized.startsWith(home)) return home.resolve(".Trash")

        val parts = normalized.iterator().asSequence().map { it.toString() }.toList()
        if (parts.size >= 2 && parts[0] == "Volumes") {
            val volumeRoot = normalized.root.resolve(parts[0]).resolve(parts[1])
            return volumeRoot.resolve(".Trashes").resolve(currentUserId())
        }

        return home.resolve(".Trash")
    }

    private fun currentUserId(): String {
        return try {
            Files.getAttribute(Path.of(System.getProperty("user.home")), "unix:uid").toString()
        } catch (exception: Throwable) {
            "501"
        }
    }

    private fun Path.uniqueTargetFor(fileName: String): Path {
        val initialTarget = resolve(fileName)
        if (!initialTarget.exists()) return initialTarget

        val dotIndex = fileName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val extension = if (dotIndex > 0) fileName.substring(dotIndex) else ""

        var index = 1
        while (true) {
            val candidate = resolve("${baseName}_$index$extension")
            if (!candidate.exists()) return candidate
            index += 1
        }
    }

    private fun isMacOs(): Boolean {
        return System.getProperty("os.name")
            .lowercase(Locale.ENGLISH)
            .contains("mac")
    }
}

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

    private const val FINDER_TIMEOUT_SECONDS = 5L
}
