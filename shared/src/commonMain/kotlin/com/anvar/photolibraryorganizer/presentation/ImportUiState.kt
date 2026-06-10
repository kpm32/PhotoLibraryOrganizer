package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Loading : ImportUiState
    data class Success(val result: ImportMediaFilesResult) : ImportUiState
    data class Error(val message: String) : ImportUiState
}
