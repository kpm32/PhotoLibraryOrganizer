package com.anvar.photolibraryorganizer.domain.repository

fun interface StorageSpaceProvider {
    suspend fun availableBytes(path: String): Long?
}
