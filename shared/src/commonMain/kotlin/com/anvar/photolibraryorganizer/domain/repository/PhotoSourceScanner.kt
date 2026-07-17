package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderProgress
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult

/**
 * Reads a source folder recursively and classifies files as supported media or
 * unsupported side files.
 *
 * Hashing is optional because full-library refreshes need fast metadata reads,
 * while source scans for duplicate detection need content hashes.
 */
interface PhotoSourceScanner {
    suspend fun scanFolder(
        path: String,
        onProgress: (ScanSourceFolderProgress) -> Unit = {},
        readContentHash: Boolean = true,
    ): AppResult<ScanSourceFolderResult>
}
