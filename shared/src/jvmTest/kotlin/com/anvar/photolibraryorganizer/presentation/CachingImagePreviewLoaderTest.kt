package com.anvar.photolibraryorganizer.presentation

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class CachingImagePreviewLoaderTest {
    @Test
    fun loadImageCachesNullResults() = runBlocking {
        val delegate = CountingImagePreviewLoader()
        val loader = CachingImagePreviewLoader(delegate)

        loader.loadImage("/photo/a.jpg")
        loader.loadImage("/photo/a.jpg")

        assertEquals(1, delegate.loadCount)
    }

    @Test
    fun loadImageEvictsOldestEntryWhenCacheIsFull() = runBlocking {
        val delegate = CountingImagePreviewLoader()
        val loader = CachingImagePreviewLoader(
            delegate = delegate,
            maxEntries = 2,
        )

        loader.loadImage("/photo/a.jpg")
        loader.loadImage("/photo/b.jpg")
        loader.loadImage("/photo/c.jpg")
        loader.loadImage("/photo/a.jpg")

        assertEquals(4, delegate.loadCount)
    }

    private class CountingImagePreviewLoader : ImagePreviewLoader {
        var loadCount = 0
            private set

        override suspend fun loadImage(path: String): ImageBitmap? {
            loadCount += 1
            return null
        }
    }
}
