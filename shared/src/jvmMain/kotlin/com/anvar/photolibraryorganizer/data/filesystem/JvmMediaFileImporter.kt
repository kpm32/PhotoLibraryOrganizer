package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class JvmMediaFileImporter : MediaFileImporter {
    override suspend fun copyFiles(plannedFiles: List<PlannedMediaFile>): AppResult<ImportMediaFilesResult> {
        return try {
            var copiedFiles = 0
            var skippedFiles = 0
            var failedFiles = 0

            plannedFiles.forEach { plannedFile ->
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
                        Files.copy(sourcePath, targetPath, StandardCopyOption.COPY_ATTRIBUTES)
                        copiedFiles += 1
                    }
                }
            }

            AppResult.Success(
                ImportMediaFilesResult(
                    copiedFiles = copiedFiles,
                    skippedFiles = skippedFiles,
                    failedFiles = failedFiles,
                ),
            )
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "Copy import failed"))
        }
    }
}
