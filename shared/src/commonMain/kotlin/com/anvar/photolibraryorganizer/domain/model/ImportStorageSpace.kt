package com.anvar.photolibraryorganizer.domain.model

sealed interface ImportStorageSpace {
    data class Available(
        val requiredBytes: Long,
        val availableBytes: Long,
    ) : ImportStorageSpace

    data class Insufficient(
        val requiredBytes: Long,
        val availableBytes: Long,
    ) : ImportStorageSpace

    data object Unavailable : ImportStorageSpace
}
