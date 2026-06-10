package com.anvar.photolibraryorganizer.presentation

import androidx.compose.ui.graphics.ImageBitmap

sealed interface ImagePreviewUiState {
    data object Empty : ImagePreviewUiState
    data object Loading : ImagePreviewUiState
    data class Success(val image: ImageBitmap) : ImagePreviewUiState
    data object Unsupported : ImagePreviewUiState
}
