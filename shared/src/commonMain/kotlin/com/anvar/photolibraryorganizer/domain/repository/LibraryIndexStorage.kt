package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot

interface LibraryIndexStorage {
    suspend fun load(destinationFolder: String?): LibraryIndexSnapshot?
    suspend fun save(snapshot: LibraryIndexSnapshot)
}
