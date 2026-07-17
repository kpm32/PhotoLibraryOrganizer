package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.repository.StorageSpaceProvider
import java.nio.file.Files
import java.nio.file.Path

class JvmStorageSpaceProvider : StorageSpaceProvider {
    override suspend fun availableBytes(path: String): Long? {
        return runCatching {
            Files.getFileStore(Path.of(path)).usableSpace
        }.getOrNull()
    }
}
