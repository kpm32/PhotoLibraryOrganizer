package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.UnsupportedFileQuarantineResult
import com.anvar.photolibraryorganizer.domain.model.UnsupportedSourceFile
import com.anvar.photolibraryorganizer.domain.repository.UnsupportedFileQuarantineRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

class JvmUnsupportedFileQuarantineRepository : UnsupportedFileQuarantineRepository {
    override suspend fun moveToQuarantine(
        destinationFolder: String?,
        unsupportedFiles: List<UnsupportedSourceFile>,
    ): AppResult<UnsupportedFileQuarantineResult> {
        val destination = destinationFolder?.trim()?.trimEnd('/')
            ?: return AppResult.Error(PhotoLibraryError.FileSystem("Папка библиотеки не выбрана."))

        return try {
            val quarantineRoot = Path.of(destination, "Unsupported")
            var movedFiles = 0
            var failedFiles = 0

            unsupportedFiles.forEach { unsupportedFile ->
                currentCoroutineContext().ensureActive()
                val sourcePath = Path.of(unsupportedFile.path)
                if (!sourcePath.exists() || !Files.isRegularFile(sourcePath)) {
                    failedFiles += 1
                    return@forEach
                }

                val typeFolder = quarantineRoot.resolve(unsupportedFile.extensionLabel.toFolderName())
                val relativeParent = Path.of(unsupportedFile.relativePath).parent
                val targetFolder = if (relativeParent == null) typeFolder else typeFolder.resolve(relativeParent)
                targetFolder.createDirectories()
                Files.move(sourcePath, targetFolder.uniqueTargetFor(unsupportedFile.fileName))
                movedFiles += 1
            }

            AppResult.Success(
                UnsupportedFileQuarantineResult(
                    movedFiles = movedFiles,
                    failedFiles = failedFiles,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "Unsupported quarantine failed"))
        }
    }

    private fun String.toFolderName(): String {
        return if (this == "без расширения") "no-extension" else this
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
