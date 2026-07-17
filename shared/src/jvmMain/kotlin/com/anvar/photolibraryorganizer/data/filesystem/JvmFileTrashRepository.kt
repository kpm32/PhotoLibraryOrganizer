package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.repository.FileTrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

/**
 * JVM adapter for moving one selected library file to the OS Trash.
 *
 * The repository only accepts an existing regular file and delegates the
 * platform-specific operation to [TrashFileMover].
 */
class JvmFileTrashRepository(
    private val trashFileMover: TrashFileMover = SystemTrashFileMover,
) : FileTrashRepository {
    override suspend fun moveToTrash(path: String): Boolean = withContext(Dispatchers.IO) {
        val sourcePath = Path.of(path).toAbsolutePath().normalize()
        Files.isRegularFile(sourcePath) && trashFileMover.moveToTrash(sourcePath)
    }
}
