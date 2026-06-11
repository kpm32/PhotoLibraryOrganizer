package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesProgress
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class AwaitingConfirmation(
        val readyFileCount: Int,
        val existingFileCount: Int,
    ) : ImportUiState
    data class Loading(val progress: ImportMediaFilesProgress? = null) : ImportUiState
    data class Success(val result: ImportMediaFilesResult) : ImportUiState
    data class Error(val message: String) : ImportUiState
}
