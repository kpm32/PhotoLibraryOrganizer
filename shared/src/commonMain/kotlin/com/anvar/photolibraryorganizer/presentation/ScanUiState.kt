package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Success(val summary: ScanSourceFolderSummary) : ScanUiState
    data class Error(val message: String) : ScanUiState
}
