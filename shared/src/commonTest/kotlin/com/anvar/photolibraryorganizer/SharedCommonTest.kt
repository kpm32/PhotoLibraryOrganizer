package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderSummary
import com.anvar.photolibraryorganizer.domain.model.detectMediaFileType
import com.anvar.photolibraryorganizer.domain.repository.PhotoSourceScanner
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
        assertEquals("Scan only", ImportMode.ScanOnly.title)
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
                        unsupportedFiles = 0,
                        totalMediaBytes = 0,
                    ),
                ),
            )
        }
    }
}
