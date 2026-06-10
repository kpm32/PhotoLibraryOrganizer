package com.anvar.photolibraryorganizer.domain

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: PhotoLibraryError) : AppResult<Nothing>
}
