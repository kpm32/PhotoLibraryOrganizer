package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MacMetadataDateReaderTest {
    @Test
    fun readsFirstAvailableMetadataDate() {
        val reader = MacMetadataDateReader(
            metadataOutputProvider = {
                """
                (null)
                2024-01-02 03:04:05 +0000
                """.trimIndent()
            },
        )

        val result = reader.readCapturedAtEpochMillis(
            path = Path.of("/photo.heic"),
            extension = "heic",
            category = MediaFileCategory.Image,
        )

        assertEquals(1_704_164_645_000, result)
    }

    @Test
    fun ignoresUnsupportedImageExtension() {
        val reader = MacMetadataDateReader(
            metadataOutputProvider = { "2024-01-02 03:04:05 +0000" },
        )

        val result = reader.readCapturedAtEpochMillis(
            path = Path.of("/photo.png"),
            extension = "png",
            category = MediaFileCategory.Image,
        )

        assertNull(result)
    }
}
