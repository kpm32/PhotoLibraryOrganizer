package com.anvar.photolibraryorganizer.presentation

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path

class JvmImagePreviewLoader : ImagePreviewLoader {
    override suspend fun loadImage(path: String): ImageBitmap? {
        return try {
            val bytes = Files.readAllBytes(Path.of(path))
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (exception: Throwable) {
            null
        }
    }
}
