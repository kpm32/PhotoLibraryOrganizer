package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.EmptyFolderCleanupResult
import com.anvar.photolibraryorganizer.domain.repository.EmptyFolderCleanupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class JvmEmptyFolderCleanupRepository : EmptyFolderCleanupRepository {
    override suspend fun deleteEmptyFolders(sourceFolder: String?): AppResult<EmptyFolderCleanupResult> {
        val sourceRoot = sourceFolder?.trim()?.takeIf { it.isNotBlank() }?.let { Path.of(it) }
            ?: return AppResult.Error(PhotoLibraryError.InvalidSourceFolder)

        if (!sourceRoot.exists()) {
            return AppResult.Error(PhotoLibraryError.SourceFolderNotFound(sourceRoot.toString()))
        }
        if (!sourceRoot.isDirectory()) {
            return AppResult.Error(PhotoLibraryError.SourceFolderIsNotDirectory(sourceRoot.toString()))
        }

        return try {
            val normalizedRoot = sourceRoot.toAbsolutePath().normalize()
            var deletedFolders = 0
            var failedFolders = 0

            val folders = Files.walk(normalizedRoot).use { paths ->
                paths
                    .filter { Files.isDirectory(it) }
                    .sorted(Comparator.reverseOrder())
                    .toList()
            }

            folders.forEach { folder ->
                currentCoroutineContext().ensureActive()
                val normalizedFolder = folder.toAbsolutePath().normalize()
                if (normalizedFolder == normalizedRoot) return@forEach

                if (!normalizedFolder.startsWith(normalizedRoot)) {
                    failedFolders += 1
                    return@forEach
                }

                if (normalizedFolder.isEmptyDirectory()) {
                    try {
                        Files.delete(normalizedFolder)
                        deletedFolders += 1
                    } catch (exception: Throwable) {
                        failedFolders += 1
                    }
                }
            }

            AppResult.Success(
                EmptyFolderCleanupResult(
                    deletedFolders = deletedFolders,
                    failedFolders = failedFolders,
                ),
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            AppResult.Error(PhotoLibraryError.FileSystem(exception.message ?: "Empty folder cleanup failed"))
        }
    }

    private fun Path.isEmptyDirectory(): Boolean {
        return Files.newDirectoryStream(this).use { stream ->
            !stream.iterator().hasNext()
        }
    }
}
