package com.anvar.photolibraryorganizer.presentation

import androidx.compose.ui.graphics.ImageBitmap

object PreviewImagePreviewLoader : ImagePreviewLoader {
    override suspend fun loadImage(path: String): ImageBitmap? = null
}
