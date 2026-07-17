package com.anvar.photolibraryorganizer.domain.repository

fun interface FileTrashRepository {
    suspend fun moveToTrash(path: String): Boolean
}
