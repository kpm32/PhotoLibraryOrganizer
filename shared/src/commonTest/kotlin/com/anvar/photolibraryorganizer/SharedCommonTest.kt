package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.ImportMediaFilesResult
import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.model.ScannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.detectMediaFileType
import com.anvar.photolibraryorganizer.domain.repository.MediaFileImporter
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
import com.anvar.photolibraryorganizer.domain.usecase.BuildMediaFilePlanUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ImportMediaFilesUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ResolveImportAvailabilityUseCase
import com.anvar.photolibraryorganizer.domain.usecase.ScanSourceFolderUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun scanRequiresSourceAndDestinationFolders() {
        val plan = PhotoLibraryPlan(
            sourceFolder = "/source",
            destinationFolder = null,
            importMode = ImportMode.ScanOnly,
        )

        assertFalse(plan.canScan)
    }

    @Test
    fun scanIsAvailableWhenBothFoldersAreSelected() {
        val plan = PhotoLibraryPlan(
            sourceFolder = "/source",
            destinationFolder = "/library",
            importMode = ImportMode.ScanOnly,
        )

        assertTrue(plan.canScan)
    }

    @Test
    fun scanOnlyIsDefaultSafeMode() {
        assertEquals("Только сканировать", ImportMode.ScanOnly.title)
    }

    @Test
    fun detectsImageFileTypeIgnoringCase() {
        val mediaType = detectMediaFileType("Summer.Photo.JPEG")

        assertEquals(MediaFileCategory.Image, mediaType?.category)
        assertEquals("jpeg", mediaType?.extension)
    }

    @Test
    fun detectsVideoFileTypeIgnoringCase() {
        val mediaType = detectMediaFileType("Family.MOV")

        assertEquals(MediaFileCategory.Video, mediaType?.category)
        assertEquals("mov", mediaType?.extension)
    }

    @Test
    fun ignoresUnsupportedFileType() {
        assertEquals(null, detectMediaFileType("notes.txt"))
    }

    @Test
    fun scanUseCaseRejectsBlankSourceFolder() = runTest {
        val useCase = ScanSourceFolderUseCase(FakePhotoSourceScanner())

        val result = useCase(" ")

        val error = assertIs<AppResult.Error>(result)
        assertEquals(PhotoLibraryError.InvalidSourceFolder, error.error)
    }

    @Test
    fun scanUseCaseDelegatesValidSourceFolderToScanner() = runTest {
        val scanner = FakePhotoSourceScanner()
        val useCase = ScanSourceFolderUseCase(scanner)

        val result = useCase("/source")

        assertIs<AppResult.Success<ScanSourceFolderResult>>(result)
        assertEquals("/source", scanner.lastScannedPath)
    }

    @Test
    fun buildsTargetPathFromFileModifiedDate() {
        val useCase = BuildMediaFilePlanUseCase()

        val result = useCase(
            destinationFolder = "/library-root",
            mediaFiles = listOf(
                ScannedMediaFile(
                    path = "/source/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 1024,
                    modifiedAtEpochMillis = 1_735_689_600_000,
                ),
            ),
        )

        assertEquals(1, result.size)
        assertTrue(result.first().targetRelativePath.contains("/library-root/Library/2025/2025-01/"))
        assertTrue(result.first().targetRelativePath.endsWith("_IMG_0001.JPG"))
    }

    @Test
    fun buildsTargetPathFromCapturedDateWhenAvailable() {
        val useCase = BuildMediaFilePlanUseCase()

        val result = useCase(
            destinationFolder = "/library-root",
            mediaFiles = listOf(
                ScannedMediaFile(
                    path = "/source/IMG_0001.JPG",
                    fileName = "IMG_0001.JPG",
                    extension = "jpg",
                    category = MediaFileCategory.Image,
                    sizeBytes = 1024,
                    modifiedAtEpochMillis = 1_735_689_600_000,
                    capturedAtEpochMillis = 1_577_836_800_000,
                ),
            ),
        )

        assertEquals(1, result.size)
        assertTrue(result.first().targetRelativePath.contains("/library-root/Library/2020/2020-01/"))
    }

    @Test
    fun importIsUnavailableBeforeScanPlanExists() {
        val useCase = ResolveImportAvailabilityUseCase()

        val result = useCase(
            importMode = ImportMode.Copy,
            plannedFiles = emptyList(),
        )

        assertIs<ImportAvailability.Unavailable>(result)
    }

    @Test
    fun importIsUnavailableInScanOnlyMode() {
        val useCase = ResolveImportAvailabilityUseCase()

        val result = useCase(
            importMode = ImportMode.ScanOnly,
            plannedFiles = listOf(fakePlannedMediaFile()),
        )

        assertIs<ImportAvailability.Unavailable>(result)
    }

    @Test
    fun importIsAvailableForCopyModeWhenPlanExists() {
        val useCase = ResolveImportAvailabilityUseCase()

        val result = useCase(
            importMode = ImportMode.Copy,
            plannedFiles = listOf(fakePlannedMediaFile()),
        )

        assertEquals(ImportAvailability.Available, result)
    }

    @Test
    fun importUseCaseDelegatesCopyModeToImporter() = runTest {
        val importer = FakeMediaFileImporter()
        val useCase = ImportMediaFilesUseCase(importer)

        val result = useCase(
            importMode = ImportMode.Copy,
            plannedFiles = listOf(fakePlannedMediaFile()),
        )

        assertIs<AppResult.Success<ImportMediaFilesResult>>(result)
        assertEquals(1, importer.lastPlannedFiles.size)
    }

    @Test
    fun importUseCaseRejectsMoveModeForNow() = runTest {
        val useCase = ImportMediaFilesUseCase(FakeMediaFileImporter())

        val result = useCase(
            importMode = ImportMode.Move,
            plannedFiles = listOf(fakePlannedMediaFile()),
        )

        val error = assertIs<AppResult.Error>(result)
        assertEquals(PhotoLibraryError.UnsupportedImportMode, error.error)
    }

    private fun fakePlannedMediaFile(): PlannedMediaFile {
        return PlannedMediaFile(
            sourcePath = "/source/IMG_0001.JPG",
            fileName = "IMG_0001.JPG",
            targetRelativePath = "/library/Library/2025/2025-01/IMG_0001.JPG",
            sizeBytes = 1024,
        )
    }

    private class FakePhotoSourceScanner : PhotoSourceScanner {
        var lastScannedPath: String? = null

        override suspend fun scanFolder(path: String): AppResult<ScanSourceFolderResult> {
            lastScannedPath = path
            return AppResult.Success(
                ScanSourceFolderResult(
                    sourceFolder = path,
                    mediaFiles = emptyList(),
                    summary = ScanSourceFolderSummary(
                        scannedFiles = 0,
                        mediaFiles = 0,
                        imageFiles = 0,
                        videoFiles = 0,
                        capturedDateFiles = 0,
                        unsupportedFiles = 0,
                        totalMediaBytes = 0,
                    ),
                ),
            )
        }
    }

    private class FakeMediaFileImporter : MediaFileImporter {
        var lastPlannedFiles: List<PlannedMediaFile> = emptyList()

        override suspend fun copyFiles(plannedFiles: List<PlannedMediaFile>): AppResult<ImportMediaFilesResult> {
            lastPlannedFiles = plannedFiles
            return AppResult.Success(
                ImportMediaFilesResult(
                    copiedFiles = plannedFiles.size,
                    skippedFiles = 0,
                    failedFiles = 0,
                ),
            )
        }
    }
}
