package com.anvar.photolibraryorganizer.domain

sealed interface PhotoLibraryError {
    data object InvalidSourceFolder : PhotoLibraryError
    data class SourceFolderNotFound(val path: String) : PhotoLibraryError
    data class SourceFolderIsNotDirectory(val path: String) : PhotoLibraryError
    data object ImportPlanIsEmpty : PhotoLibraryError
    data object UnsupportedImportMode : PhotoLibraryError
    data class FileSystem(val message: String) : PhotoLibraryError
    data class Unknown(val message: String? = null) : PhotoLibraryError
}
