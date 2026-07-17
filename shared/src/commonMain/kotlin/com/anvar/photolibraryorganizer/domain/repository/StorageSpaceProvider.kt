package com.anvar.photolibraryorganizer.domain.repository

/**
 * Reads available storage for a destination path when the platform can provide it.
 */
fun interface StorageSpaceProvider {
    suspend fun availableBytes(path: String): Long?
}
