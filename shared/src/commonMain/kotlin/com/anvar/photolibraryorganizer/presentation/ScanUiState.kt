package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.model.UnsupportedSourceFile

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Loading(val progress: ScanSourceFolderProgress? = null) : ScanUiState
    data object Canceled : ScanUiState
    data class Success(
        val summary: ScanSourceFolderSummary,
        val plannedFiles: List<PlannedMediaFile>,
        val unsupportedFiles: List<UnsupportedSourceFile> = emptyList(),
    ) : ScanUiState
    data class Error(val message: String) : ScanUiState
}
