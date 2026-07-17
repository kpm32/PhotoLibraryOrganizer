package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot

/**
 * Stores a lightweight snapshot of the organized library for fast application
 * startup and manual refresh.
 *
 * This is an index only. The filesystem remains the source of truth, so losing
 * or rebuilding the index must not damage the archive.
 */
interface LibraryIndexStorage {
    suspend fun load(destinationFolder: String?): LibraryIndexSnapshot?
    suspend fun save(snapshot: LibraryIndexSnapshot)
}
