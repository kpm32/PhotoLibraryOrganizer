package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.model.ScannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.UnsupportedSourceFile
import com.anvar.photolibraryorganizer.domain.model.detectMediaFileType
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.name

class JvmPhotoSourceScanner(
    private val metadataDateReader: CapturedDateReader = MacMetadataDateReader(),
) : PhotoSourceScanner {
    private val jpegExifDateReader = JpegExifDateReader()

    override suspend fun scanFolder(
        path: String,
        onProgress: (ScanSourceFolderProgress) -> Unit,
    ): AppResult<ScanSourceFolderResult> {
        return try {
            val sourcePath = Path.of(path)

            when {
                !sourcePath.exists() -> AppResult.Error(PhotoLibraryError.SourceFolderNotFound(path))
                !sourcePath.isDirectory() -> AppResult.Error(PhotoLibraryError.SourceFolderIsNotDirectory(path))
                else -> scanExistingDirectory(sourcePath, onProgress)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.Unknown(exception.message))
        }
    }

    private suspend fun scanExistingDirectory(
        sourcePath: Path,
        onProgress: (ScanSourceFolderProgress) -> Unit,
    ): AppResult<ScanSourceFolderResult> {
        return try {
            val mediaFiles = mutableListOf<ScannedMediaFile>()
            val unsupportedSourceFiles = mutableListOf<UnsupportedSourceFile>()
            val unsupportedFileExtensions = mutableMapOf<String, Int>()
            var scannedFiles = 0
            var unsupportedFiles = 0

            Files.walk(sourcePath).use { paths ->
                val iterator = paths
                    .filter { it.isRegularFile() }
                    .iterator()
                while (iterator.hasNext()) {
                    val file = iterator.next()
                    currentCoroutineContext().ensureActive()
                    scannedFiles += 1

                    val mediaType = detectMediaFileType(file.name)
                    if (mediaType == null) {
                        unsupportedFiles += 1
                        val extension = file.name.unsupportedExtensionLabel()
                        unsupportedFileExtensions[extension] = unsupportedFileExtensions.getOrDefault(extension, 0) + 1
                        unsupportedSourceFiles += UnsupportedSourceFile(
                            path = file.toAbsolutePath().toString(),
                            relativePath = sourcePath.relativize(file).toString(),
                            fileName = file.name,
                            extensionLabel = extension,
                            sizeBytes = file.fileSize(),
                            modifiedAtEpochMillis = file.getLastModifiedTime().toMillis(),
                        )
                    } else {
                        mediaFiles += ScannedMediaFile(
                            path = file.toAbsolutePath().toString(),
                            fileName = file.name,
                            extension = mediaType.extension,
                            category = mediaType.category,
                            sizeBytes = file.fileSize(),
                            modifiedAtEpochMillis = file.getLastModifiedTime().toMillis(),
                            capturedAtEpochMillis = file.readCapturedAtEpochMillis(
                                extension = mediaType.extension,
                                category = mediaType.category,
                            ),
                            contentHash = file.sha256(),
                        )
                    }
                    if (scannedFiles % PROGRESS_EMIT_STEP == 0) {
                        onProgress(
                            ScanSourceFolderProgress(
                                scannedFiles = scannedFiles,
                                mediaFiles = mediaFiles.size,
                                unsupportedFiles = unsupportedFiles,
                                unsupportedFileExtensions = unsupportedFileExtensions.toSortedUnsupportedExtensions(),
                            ),
                        )
                    }
                }
            }
            onProgress(
                ScanSourceFolderProgress(
                    scannedFiles = scannedFiles,
                    mediaFiles = mediaFiles.size,
                    unsupportedFiles = unsupportedFiles,
                    unsupportedFileExtensions = unsupportedFileExtensions.toSortedUnsupportedExtensions(),
                ),
            )

            AppResult.Success(
                ScanSourceFolderResult(
                    sourceFolder = sourcePath.toAbsolutePath().toString(),
                    mediaFiles = mediaFiles,
                    unsupportedFiles = unsupportedSourceFiles,
                    summary = ScanSourceFolderSummary(
                        scannedFiles = scannedFiles,
                        mediaFiles = mediaFiles.size,
                        imageFiles = mediaFiles.count { it.category == MediaFileCategory.Image },
                        videoFiles = mediaFiles.count { it.category == MediaFileCategory.Video },
                        capturedDateFiles = mediaFiles.count { it.capturedAtEpochMillis != null },
                        unsupportedFiles = unsupportedFiles,
                        totalMediaBytes = mediaFiles.sumOf { it.sizeBytes },
                        unsupportedFileExtensions = unsupportedFileExtensions.toSortedUnsupportedExtensions(),
                    ),
                ),
            )
        } catch (exception: IOException) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "File system scan failed"))
        } catch (exception: SecurityException) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "No permission to scan folder"))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.Unknown(exception.message))
        }
    }

    private fun String.unsupportedExtensionLabel(): String {
        return substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
            .ifBlank { "без расширения" }
    }

    private fun Map<String, Int>.toSortedUnsupportedExtensions(): Map<String, Int> {
        return toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .toMap()
    }

    private fun Path.readCapturedAtEpochMillis(
        extension: String,
        category: MediaFileCategory,
    ): Long? {
        val jpegCapturedAt = if (extension == "jpg" || extension == "jpeg") {
            jpegExifDateReader.readCapturedAtEpochMillis(this)
        } else {
            null
        }
        return jpegCapturedAt ?: metadataDateReader.readCapturedAtEpochMillis(
            path = this,
            extension = extension,
            category = category,
        )
    }

    private suspend fun Path.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(this).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    private companion object {
        const val PROGRESS_EMIT_STEP = 250
    }
}
