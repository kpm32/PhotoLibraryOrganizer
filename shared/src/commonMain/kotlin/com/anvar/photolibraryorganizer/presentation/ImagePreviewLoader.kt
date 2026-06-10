package com.anvar.photolibraryorganizer.presentation

import androidx.compose.ui.graphics.ImageBitmap

interface ImagePreviewLoader {
    suspend fun loadImage(path: String): ImageBitmap?
}
