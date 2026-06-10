package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult

interface PhotoSourceScanner {
    suspend fun scanFolder(path: String): AppResult<ScanSourceFolderResult>
}
