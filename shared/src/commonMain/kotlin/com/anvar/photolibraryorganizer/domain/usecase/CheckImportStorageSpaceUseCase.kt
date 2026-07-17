package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.model.ImportStorageSpace
import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.StorageSpaceProvider

/**
 * Estimates whether the destination has enough free space for a copy import.
 *
 * Files that already exist at the target are excluded because the importer will
 * skip them instead of writing another copy.
 */
class CheckImportStorageSpaceUseCase(
    private val storageSpaceProvider: StorageSpaceProvider,
) {
    suspend operator fun invoke(
        destinationFolder: String,
        plannedFiles: List<PlannedMediaFile>,
    ): ImportStorageSpace {
        val requiredBytes = plannedFiles
            .asSequence()
            .filter { it.targetStatus != ImportTargetStatus.AlreadyExists }
            .sumOf { it.sizeBytes }
        val availableBytes = storageSpaceProvider.availableBytes(destinationFolder)
            ?: return ImportStorageSpace.Unavailable

        return if (availableBytes >= requiredBytes) {
            ImportStorageSpace.Available(requiredBytes, availableBytes)
        } else {
            ImportStorageSpace.Insufficient(requiredBytes, availableBytes)
        }
    }
}
