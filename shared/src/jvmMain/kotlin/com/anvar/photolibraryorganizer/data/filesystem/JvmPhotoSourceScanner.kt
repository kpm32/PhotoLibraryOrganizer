package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.model.ScannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.detectMediaFileType
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

class JvmPhotoSourceScanner : PhotoSourceScanner {
    override suspend fun scanFolder(path: String): AppResult<ScanSourceFolderResult> {
        return try {
            val sourcePath = Path.of(path)

            when {
                !sourcePath.exists() -> AppResult.Error(PhotoLibraryError.SourceFolderNotFound(path))
                !sourcePath.isDirectory() -> AppResult.Error(PhotoLibraryError.SourceFolderIsNotDirectory(path))
                else -> scanExistingDirectory(sourcePath)
            }
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.Unknown(exception.message))
        }
    }

    private fun scanExistingDirectory(sourcePath: Path): AppResult<ScanSourceFolderResult> {
        return try {
            val mediaFiles = mutableListOf<ScannedMediaFile>()
            var scannedFiles = 0
            var unsupportedFiles = 0

            Files.walk(sourcePath).use { paths ->
                paths
                    .filter { it.isRegularFile() }
                    .forEach { file ->
                        scannedFiles += 1

                        val mediaType = detectMediaFileType(file.name)
                        if (mediaType == null) {
                            unsupportedFiles += 1
                        } else {
                            mediaFiles += ScannedMediaFile(
                                path = file.toAbsolutePath().toString(),
                                fileName = file.name,
                                extension = mediaType.extension,
                                category = mediaType.category,
                                sizeBytes = file.fileSize(),
                            )
                        }
                    }
            }

            AppResult.Success(
                ScanSourceFolderResult(
                    sourceFolder = sourcePath.toAbsolutePath().toString(),
                    mediaFiles = mediaFiles,
                    summary = ScanSourceFolderSummary(
                        scannedFiles = scannedFiles,
                        mediaFiles = mediaFiles.size,
                        imageFiles = mediaFiles.count { it.category == MediaFileCategory.Image },
                        videoFiles = mediaFiles.count { it.category == MediaFileCategory.Video },
                        unsupportedFiles = unsupportedFiles,
                        totalMediaBytes = mediaFiles.sumOf { it.sizeBytes },
                    ),
                ),
            )
        } catch (exception: IOException) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "File system scan failed"))
        } catch (exception: SecurityException) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "No permission to scan folder"))
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.Unknown(exception.message))
        }
    }
}
