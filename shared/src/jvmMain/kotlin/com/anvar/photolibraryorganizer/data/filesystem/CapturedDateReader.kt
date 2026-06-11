package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import java.nio.file.Path

fun interface CapturedDateReader {
    fun readCapturedAtEpochMillis(
        path: Path,
        extension: String,
        category: MediaFileCategory,
    ): Long?
}
