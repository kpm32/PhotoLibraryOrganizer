package com.anvar.photolibraryorganizer.domain.model

/**
 * Result of moving one selected library item to the operating system Trash.
 *
 * The action is intentionally separate from bulk duplicate or unsupported-file
 * cleanup because it acts on the file currently visible in the inspector.
 */
sealed interface MoveSelectedFileToTrashResult {
    data class Moved(val path: String) : MoveSelectedFileToTrashResult
    data object FileNotSelected : MoveSelectedFileToTrashResult
    data object SourceFileActionNotAllowed : MoveSelectedFileToTrashResult
    data object TimedOut : MoveSelectedFileToTrashResult
    data object Failed : MoveSelectedFileToTrashResult
    data class Error(val message: String?) : MoveSelectedFileToTrashResult
}
