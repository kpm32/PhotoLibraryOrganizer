package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

class JvmMediaFileImporter(
    private val importedFileVerifier: ImportedFileVerifier = ImportedFileVerifier.Default,
) : MediaFileImporter {
    override suspend fun copyFiles(
        plannedFiles: List<PlannedMediaFile>,
        onProgress: (ImportMediaFilesProgress) -> Unit,
    ): AppResult<ImportMediaFilesResult> {
        return importFiles(plannedFiles, moveSource = false, onProgress = onProgress)
    }

    override suspend fun moveFiles(
        plannedFiles: List<PlannedMediaFile>,
        onProgress: (ImportMediaFilesProgress) -> Unit,
    ): AppResult<ImportMediaFilesResult> {
        return importFiles(plannedFiles, moveSource = true, onProgress = onProgress)
    }

    private suspend fun importFiles(
        plannedFiles: List<PlannedMediaFile>,
        moveSource: Boolean,
        onProgress: (ImportMediaFilesProgress) -> Unit,
    ): AppResult<ImportMediaFilesResult> {
        return try {
            var copiedFiles = 0
            var movedFiles = 0
            var skippedFiles = 0
            var failedFiles = 0

            plannedFiles.forEachIndexed { index, plannedFile ->
                currentCoroutineContext().ensureActive()
                val sourcePath = Path.of(plannedFile.sourcePath)
                val targetPath = Path.of(plannedFile.targetRelativePath)

                when {
                    targetPath.exists() -> skippedFiles += 1
                    !sourcePath.exists() -> failedFiles += 1
                    else -> {
                        val imported = try {
                            val sourceSizeBytes = Files.size(sourcePath)
                            val targetParent = targetPath.parent
                            if (targetParent != null) {
                                targetParent.createDirectories()
                            }
                            if (moveSource) {
                                Files.move(sourcePath, targetPath)
                            } else {
                                Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES)
                            }
                            importedFileVerifier.isImported(
                                sourcePath = sourcePath,
                                targetPath = targetPath,
                                expectedSizeBytes = sourceSizeBytes,
                                moveSource = moveSource,
                            )
                        } catch (_: Throwable) {
                            false
                        }

                        if (imported) {
                            if (moveSource) {
                                movedFiles += 1
                            } else {
                                copiedFiles += 1
                            }
                        } else {
                            if (!moveSource) {
                                targetPath.deleteIfExists()
                            }
                            failedFiles += 1
                        }
                    }
                }
                onProgress(
                    ImportMediaFilesProgress(
                        totalFiles = plannedFiles.size,
                        processedFiles = index + 1,
                        copiedFiles = copiedFiles,
                        movedFiles = movedFiles,
                        skippedFiles = skippedFiles,
                        failedFiles = failedFiles,
                    ),
                )
            }

            AppResult.Success(
                ImportMediaFilesResult(
                    copiedFiles = copiedFiles,
                    movedFiles = movedFiles,
                    skippedFiles = skippedFiles,
                    failedFiles = failedFiles,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "File import failed"))
        }
    }
}

fun interface ImportedFileVerifier {
    fun isImported(
        sourcePath: Path,
        targetPath: Path,
        expectedSizeBytes: Long,
        moveSource: Boolean,
    ): Boolean

    object Default : ImportedFileVerifier {
        override fun isImported(
            sourcePath: Path,
            targetPath: Path,
            expectedSizeBytes: Long,
            moveSource: Boolean,
        ): Boolean {
            return targetPath.exists() &&
                Files.size(targetPath) == expectedSizeBytes &&
                (!moveSource || !sourcePath.exists())
        }
    }
}
