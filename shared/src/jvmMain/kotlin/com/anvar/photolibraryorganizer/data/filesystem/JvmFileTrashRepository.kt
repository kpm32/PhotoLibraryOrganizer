package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.repository.FileTrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

class JvmFileTrashRepository(
    private val trashFileMover: TrashFileMover = SystemTrashFileMover,
) : FileTrashRepository {
    override suspend fun moveToTrash(path: String): Boolean = withContext(Dispatchers.IO) {
        val sourcePath = Path.of(path).toAbsolutePath().normalize()
        Files.isRegularFile(sourcePath) && trashFileMover.moveToTrash(sourcePath)
    }
}
