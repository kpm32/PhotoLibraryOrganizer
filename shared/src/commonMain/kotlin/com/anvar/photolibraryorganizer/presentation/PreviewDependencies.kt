package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.model.ScannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner

object PreviewFolderPicker : FolderPicker {
    override fun chooseFolder(title: String): String = when {
        title.contains("исход", ignoreCase = true) -> "/Users/anvardzan/Pictures/Unsorted archive"
        else -> "/Users/anvardzan/Pictures/PhotoLibrary"
    }
}

object PreviewPhotoSourceScanner : PhotoSourceScanner {
    override suspend fun scanFolder(path: String): AppResult<ScanSourceFolderResult> {
        val files = listOf(
            ScannedMediaFile(
                path = "$path/IMG_0001.jpg",
                fileName = "IMG_0001.jpg",
                extension = "jpg",
                category = MediaFileCategory.Image,
                sizeBytes = 2_400_000,
                modifiedAtEpochMillis = 1_735_689_600_000,
            ),
            ScannedMediaFile(
                path = "$path/VID_0001.mov",
                fileName = "VID_0001.mov",
                extension = "mov",
                category = MediaFileCategory.Video,
                sizeBytes = 18_000_000,
                modifiedAtEpochMillis = 1_735_693_200_000,
            ),
        )
        return AppResult.Success(
            ScanSourceFolderResult(
                sourceFolder = path,
                mediaFiles = files,
                summary = ScanSourceFolderSummary(
                    scannedFiles = 3,
                    mediaFiles = files.size,
                    imageFiles = 1,
                    videoFiles = 1,
                    capturedDateFiles = 0,
                    unsupportedFiles = 1,
                    totalMediaBytes = files.sumOf { it.sizeBytes },
                ),
            ),
        )
    }
}
