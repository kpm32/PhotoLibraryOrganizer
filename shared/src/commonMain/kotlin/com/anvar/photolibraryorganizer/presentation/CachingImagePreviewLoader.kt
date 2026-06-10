package com.anvar.photolibraryorganizer.presentation

import androidx.compose.ui.graphics.ImageBitmap

class CachingImagePreviewLoader(
    private val delegate: ImagePreviewLoader,
    private val maxEntries: Int = DefaultMaxEntries,
) : ImagePreviewLoader {
    private val cache = LinkedHashMap<String, ImageBitmap?>()

    init {
        require(maxEntries > 0) { "maxEntries must be greater than zero" }
    }

    override suspend fun loadImage(path: String): ImageBitmap? {
        if (cache.containsKey(path)) {
            return cache[path]
        }

        val image = delegate.loadImage(path)
        cache[path] = image
        trimCache()
        return image
    }

    private fun trimCache() {
        while (cache.size > maxEntries) {
            cache.remove(cache.keys.first())
        }
    }

    private companion object {
        const val DefaultMaxEntries = 256
    }
}
