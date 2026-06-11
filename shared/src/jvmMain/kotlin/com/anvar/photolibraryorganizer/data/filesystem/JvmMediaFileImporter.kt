package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class JvmMediaFileImporter : MediaFileImporter {
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

    private fun importFiles(
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
                val sourcePath = Path.of(plannedFile.sourcePath)
                val targetPath = Path.of(plannedFile.targetRelativePath)

                when {
                    targetPath.exists() -> skippedFiles += 1
                    !sourcePath.exists() -> failedFiles += 1
                    else -> {
                        val targetParent = targetPath.parent
                        if (targetParent != null) {
                            targetParent.createDirectories()
                        }
                        if (moveSource) {
                            Files.move(sourcePath, targetPath)
                            movedFiles += 1
                        } else {
                            Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES)
                            copiedFiles += 1
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
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "File import failed"))
        }
    }
}
