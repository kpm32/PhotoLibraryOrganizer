package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ImportFailureDetail
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

private const val MaxFailureDetails = 20

/**
 * Filesystem importer for copy and move modes.
 *
 * Each file is handled independently so one failed copy/move is reported in the
 * final result without stopping the whole import batch.
 */
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

    /**
     * Executes the common import loop for copy and move.
     *
     * The target file is verified after writing because external drives can
     * report success before data is actually usable.
     */
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
            val failureDetails = mutableListOf<ImportFailureDetail>()

            plannedFiles.forEachIndexed { index, plannedFile ->
                currentCoroutineContext().ensureActive()
                val sourcePath = Path.of(plannedFile.sourcePath)
                val targetPath = Path.of(plannedFile.targetRelativePath)

                when {
                    targetPath.exists() -> skippedFiles += 1
                    !sourcePath.exists() -> {
                        failedFiles += 1
                        failureDetails.addFailureDetail(
                            plannedFile = plannedFile,
                            reason = "Исходный файл не найден",
                        )
                    }
                    else -> {
                        var failureReason = "Не удалось подтвердить целевой файл после импорта"
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
                        } catch (exception: Throwable) {
                            failureReason = exception.message ?: "Файловая операция завершилась ошибкой"
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
                            failureDetails.addFailureDetail(
                                plannedFile = plannedFile,
                                reason = failureReason,
                            )
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
                    failureDetails = failureDetails.toList(),
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "File import failed"))
        }
    }
}

private fun MutableList<ImportFailureDetail>.addFailureDetail(
    plannedFile: PlannedMediaFile,
    reason: String,
) {
    if (size >= MaxFailureDetails) return
    add(
        ImportFailureDetail(
            sourcePath = plannedFile.sourcePath,
            targetPath = plannedFile.targetRelativePath,
            reason = reason,
        ),
    )
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
