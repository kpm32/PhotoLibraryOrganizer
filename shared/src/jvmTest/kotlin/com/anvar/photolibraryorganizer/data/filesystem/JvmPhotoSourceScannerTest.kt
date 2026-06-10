package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.createDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class JvmPhotoSourceScannerTest {
    private val scanner = JvmPhotoSourceScanner()

    @Test
    fun scansMediaFilesRecursivelyWithoutChangingSourceFolder() = runTest {
        val sourceFolder = Files.createTempDirectory("photo-scan-test")
        val nestedFolder = sourceFolder.resolve("nested").createDirectory()
        sourceFolder.resolve("image.JPG").writeText("fake image")
        nestedFolder.resolve("video.MOV").writeText("fake video")
        nestedFolder.resolve("notes.txt").writeText("not media")

        val result = scanner.scanFolder(sourceFolder.toString())

        val success = assertIs<AppResult.Success<ScanSourceFolderResult>>(result)
        val summary = success.data.summary
        assertEquals(3, summary.scannedFiles)
        assertEquals(2, summary.mediaFiles)
        assertEquals(1, summary.imageFiles)
        assertEquals(1, summary.videoFiles)
        assertEquals(1, summary.unsupportedFiles)
    }

    @Test
    fun returnsTypedErrorWhenSourceFolderDoesNotExist() = runTest {
        val result = scanner.scanFolder("/path/that/does/not/exist")

        val error = assertIs<AppResult.Error>(result)
        assertIs<PhotoLibraryError.SourceFolderNotFound>(error.error)
    }
}
