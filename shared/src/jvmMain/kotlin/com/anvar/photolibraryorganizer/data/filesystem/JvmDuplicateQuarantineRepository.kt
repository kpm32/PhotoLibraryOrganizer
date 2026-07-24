package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineDeleteResult
import com.anvar.photolibraryorganizer.domain.model.DuplicateQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.DuplicateQuarantineRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * JVM implementation of duplicate quarantine management.
 *
 * Exact duplicate candidates are grouped by hash prefix under `Duplicates`;
 * later cleanup only moves files from that quarantine area to Trash.
 */
class JvmDuplicateQuarantineRepository(
    private val trashFileMover: TrashFileMover = SystemTrashFileMover,
) : DuplicateQuarantineRepository {
    override suspend fun moveToQuarantine(
        destinationFolder: String?,
        duplicateFiles: List<PlannedMediaFile>,
    ): AppResult<DuplicateQuarantineResult> {
        val destination = destinationFolder?.trim()?.trimEnd('/')
            ?: return AppResult.Error(PhotoLibraryError.FileSystem("Папка библиотеки не выбрана."))

        return try {
            val quarantineRoot = Path.of(destination, "Duplicates")
            var movedFiles = 0
            var failedFiles = 0

            duplicateFiles.forEach { duplicateFile ->
                currentCoroutineContext().ensureActive()
                val sourcePath = Path.of(duplicateFile.sourcePath)
                if (!sourcePath.exists()) {
                    failedFiles += 1
                    return@forEach
                }

                val groupFolder = quarantineRoot.resolve(duplicateFile.contentHash?.take(12) ?: "unknown")
                groupFolder.createDirectories()
                val targetPath = groupFolder.uniqueTargetFor(sourcePath.fileName.toString())

                Files.move(sourcePath, targetPath)
                movedFiles += 1
            }

            AppResult.Success(
                DuplicateQuarantineResult(
                    movedFiles = movedFiles,
                    failedFiles = failedFiles,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "Duplicate quarantine failed"))
        }
    }

    override suspend fun deleteFromQuarantine(
        destinationFolder: String?,
        quarantineFiles: List<PlannedMediaFile>,
    ): AppResult<DuplicateQuarantineDeleteResult> {
        val destination = destinationFolder?.trim()?.trimEnd('/')
            ?: return AppResult.Error(PhotoLibraryError.FileSystem("Папка библиотеки не выбрана."))

        return try {
            val quarantineRoot = Path.of(destination, "Duplicates").toAbsolutePath().normalize()
            var deletedFiles = 0
            var failedFiles = 0

            quarantineFiles.forEach { quarantineFile ->
                currentCoroutineContext().ensureActive()
                val sourcePath = Path.of(quarantineFile.sourcePath).toAbsolutePath().normalize()
                if (!sourcePath.startsWith(quarantineRoot) || !sourcePath.exists() || !Files.isRegularFile(sourcePath)) {
                    failedFiles += 1
                    return@forEach
                }

                if (!trashFileMover.moveToTrash(sourcePath)) {
                    failedFiles += 1
                    return@forEach
                }
                deletedFiles += 1
            }

            AppResult.Success(
                DuplicateQuarantineDeleteResult(
                    deletedFiles = deletedFiles,
                    failedFiles = failedFiles,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "Duplicate quarantine delete failed"))
        }
    }

    private fun Path.uniqueTargetFor(fileName: String): Path {
        val initialTarget = resolve(fileName)
        if (!initialTarget.exists()) return initialTarget

        val dotIndex = fileName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
        val extension = if (dotIndex > 0) fileName.substring(dotIndex) else ""

        var index = 1
        while (true) {
            val candidate = resolve("${baseName}_$index$extension")
            if (!candidate.exists()) return candidate
            index += 1
        }
    }
}
