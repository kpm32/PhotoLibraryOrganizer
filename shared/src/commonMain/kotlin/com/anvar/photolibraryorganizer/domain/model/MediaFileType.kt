package com.anvar.photolibraryorganizer.domain.model

data class MediaFileType(
    val extension: String,
    val category: MediaFileCategory,
)

fun detectMediaFileType(fileName: String): MediaFileType? {
    val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .takeIf { it.isNotBlank() }
        ?: return null

    return when (extension) {
        "jpg", "jpeg", "png", "heic", "heif", "webp", "gif", "tif", "tiff", "bmp" ->
            MediaFileType(extension = extension, category = MediaFileCategory.Image)

        "mp4", "mov", "m4v", "avi", "mkv", "webm", "3gp" ->
            MediaFileType(extension = extension, category = MediaFileCategory.Video)

        else -> null
    }
}
