package com.anvar.photolibraryorganizer.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Loads previews for the inspector and grid.
 *
 * Images are decoded directly through Skia. Video thumbnails fall back to macOS
 * QuickLook because Compose Desktop does not provide native video frame decoding.
 */
class JvmImagePreviewLoader : ImagePreviewLoader {
    override suspend fun loadImage(path: String): ImageBitmap? {
        return withContext(Dispatchers.IO) {
            decodeImage(path) ?: loadQuickLookThumbnail(path)
        }
    }

    private fun decodeImage(path: String): ImageBitmap? {
        return try {
            val bytes = Files.readAllBytes(Path.of(path))
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (exception: Throwable) {
            null
        }
    }

    private fun loadQuickLookThumbnail(path: String): ImageBitmap? {
        if (!isMacOs() || !path.isVideoPath()) return null

        val sourcePath = Path.of(path)
        if (!sourcePath.exists() || !sourcePath.isRegularFile()) return null

        val tempFolder = Files.createTempDirectory("photo-library-video-preview")
        return try {
            val process = ProcessBuilder(
                "qlmanage",
                "-t",
                "-s",
                QUICK_LOOK_THUMBNAIL_SIZE.toString(),
                "-o",
                tempFolder.toString(),
                sourcePath.toString(),
            )
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(QUICK_LOOK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() != 0) return null

            val thumbnail = Files.list(tempFolder).use { files ->
                files
                    .filter { it.name.endsWith(".png", ignoreCase = true) }
                    .findFirst()
                    .orElse(null)
            } ?: return null

            decodeImage(thumbnail.toString())
        } catch (exception: Throwable) {
            null
        } finally {
            tempFolder.deleteRecursively()
        }
    }

    private fun isMacOs(): Boolean {
        return System.getProperty("os.name").contains("mac", ignoreCase = true)
    }

    private fun String.isVideoPath(): Boolean {
        val extension = substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        return extension in videoExtensions
    }

    private fun Path.deleteRecursively() {
        if (!exists()) return

        Files.walk(this).use { paths ->
            paths
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

    private companion object {
        const val QUICK_LOOK_THUMBNAIL_SIZE = 512
        const val QUICK_LOOK_TIMEOUT_SECONDS = 8L

        val videoExtensions = setOf(
            "mp4",
            "mov",
            "m4v",
            "avi",
            "mkv",
            "webm",
            "3gp",
            "mpg",
            "mpeg",
            "mts",
            "m2ts",
            "ts",
        )
    }
}
