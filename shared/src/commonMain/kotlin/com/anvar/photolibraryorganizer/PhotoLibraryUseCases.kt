package com.anvar.photolibraryorganizer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.anvar.photolibraryorganizer.domain.repository.FileTrashRepository
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.repository.StorageSpaceProvider
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.CheckImportStorageSpaceUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ImportMediaFilesUseCase
import com.anvar.photolibraryorganizer.domain.usecase.MoveSelectedFileToTrashUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase

/**
 * Application-level use case bundle for the Compose desktop root.
 *
 * Keeping construction here makes `App.kt` a thinner composition root and gives
 * future controller/reducer code one stable dependency object to receive.
 */
internal data class PhotoLibraryUseCases(
    val scanSourceFolder: ScanSourceFolderUseCase,
    val importMediaFiles: ImportMediaFilesUseCase,
    val checkImportStorageSpace: CheckImportStorageSpaceUseCase,
    val buildMediaFilePlan: BuildMediaFilePlanUseCase,
    val resolveImportAvailability: ResolveImportAvailabilityUseCase,
    val moveSelectedFileToTrash: MoveSelectedFileToTrashUseCase,
)

@Composable
internal fun rememberPhotoLibraryUseCases(
    photoSourceScanner: PhotoSourceScanner,
    mediaFileImporter: MediaFileImporter,
    storageSpaceProvider: StorageSpaceProvider,
    fileTrashRepository: FileTrashRepository,
): PhotoLibraryUseCases {
    val scanSourceFolderUseCase = remember(photoSourceScanner) {
        ScanSourceFolderUseCase(photoSourceScanner)
    }
    val importMediaFilesUseCase = remember(mediaFileImporter) {
        ImportMediaFilesUseCase(mediaFileImporter)
    }
    val checkImportStorageSpaceUseCase = remember(storageSpaceProvider) {
        CheckImportStorageSpaceUseCase(storageSpaceProvider)
    }
    val moveSelectedFileToTrashUseCase = remember(fileTrashRepository) {
        MoveSelectedFileToTrashUseCase(fileTrashRepository)
    }
    val buildMediaFilePlanUseCase = remember { BuildMediaFilePlanUseCase() }
    val resolveImportAvailabilityUseCase = remember { ResolveImportAvailabilityUseCase() }

    return remember(
        scanSourceFolderUseCase,
        importMediaFilesUseCase,
        checkImportStorageSpaceUseCase,
        buildMediaFilePlanUseCase,
        resolveImportAvailabilityUseCase,
        moveSelectedFileToTrashUseCase,
    ) {
        PhotoLibraryUseCases(
            scanSourceFolder = scanSourceFolderUseCase,
            importMediaFiles = importMediaFilesUseCase,
            checkImportStorageSpace = checkImportStorageSpaceUseCase,
            buildMediaFilePlan = buildMediaFilePlanUseCase,
            resolveImportAvailability = resolveImportAvailabilityUseCase,
            moveSelectedFileToTrash = moveSelectedFileToTrashUseCase,
        )
    }
}
