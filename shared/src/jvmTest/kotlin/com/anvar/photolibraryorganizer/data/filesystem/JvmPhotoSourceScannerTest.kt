package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.PhotoLibraryError
import com.anvar.photolibraryorganizer.domain.model.ScanSourceFolderResult
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.path.createDirectory
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class JvmPhotoSourceScannerTest {
    private val scanner = JvmPhotoSourceScanner()

    @Test
    fun scansMediaFilesRecursivelyWithoutChangingSourceFolder() = runTest {
        val sourceFolder = Files.createTempDirectory("photo-scan-test")
        val nestedFolder = sourceFolder.resolve("nested").createDirectory()
        sourceFolder.resolve("image.JPG").writeText("fake image")
        nestedFolder.resolve("video.MOV").writeText("fake video")
        nestedFolder.resolve("notes.txt").writeText("not media")
        nestedFolder.resolve("sidecar.AAE").writeText("apple sidecar")
        nestedFolder.resolve("README").writeText("no extension")

        val progressEvents = mutableListOf<Int>()
        val result = scanner.scanFolder(sourceFolder.toString()) { progress ->
            progressEvents += progress.scannedFiles
        }

        val success = assertIs<AppResult.Success<ScanSourceFolderResult>>(result)
        val summary = success.data.summary
        assertEquals(5, summary.scannedFiles)
        assertEquals(2, summary.mediaFiles)
        assertEquals(1, summary.imageFiles)
        assertEquals(1, summary.videoFiles)
        assertEquals(3, summary.unsupportedFiles)
        assertEquals(
            mapOf(
                "aae" to 1,
                "без расширения" to 1,
                "txt" to 1,
            ),
            summary.unsupportedFileExtensions,
        )
        assertEquals(listOf(5), progressEvents)
    }

    @Test
    fun readsCapturedDateFromJpegExif() = runTest {
        val sourceFolder = Files.createTempDirectory("photo-exif-test")
        sourceFolder.resolve("image.jpg").writeBytes(jpegWithDateTimeOriginal())

        val result = scanner.scanFolder(sourceFolder.toString(), onProgress = {})

        val success = assertIs<AppResult.Success<ScanSourceFolderResult>>(result)
        val mediaFile = success.data.mediaFiles.single()
        assertEquals(expectedCapturedAtEpochMillis(), mediaFile.capturedAtEpochMillis)
        assertEquals(1, success.data.summary.capturedDateFiles)
    }

    @Test
    fun calculatesStableContentHashesForMediaFiles() = runTest {
        val sourceFolder = Files.createTempDirectory("photo-hash-test")
        sourceFolder.resolve("same-a.jpg").writeText("same-content")
        sourceFolder.resolve("same-b.jpg").writeText("same-content")
        sourceFolder.resolve("different.jpg").writeText("different-content")

        val result = scanner.scanFolder(sourceFolder.toString(), onProgress = {})

        val success = assertIs<AppResult.Success<ScanSourceFolderResult>>(result)
        val filesByName = success.data.mediaFiles.associateBy { it.fileName }
        assertEquals(filesByName.getValue("same-a.jpg").contentHash, filesByName.getValue("same-b.jpg").contentHash)
        assertNotEquals(filesByName.getValue("same-a.jpg").contentHash, filesByName.getValue("different.jpg").contentHash)
    }

    @Test
    fun returnsTypedErrorWhenSourceFolderDoesNotExist() = runTest {
        val result = scanner.scanFolder("/path/that/does/not/exist", onProgress = {})

        val error = assertIs<AppResult.Error>(result)
        assertIs<PhotoLibraryError.SourceFolderNotFound>(error.error)
    }

    private fun jpegWithDateTimeOriginal(): ByteArray {
        val exifHeader = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
        val tiff = byteArrayOf(
            0x4D, 0x4D, 0x00, 0x2A,
            0x00, 0x00, 0x00, 0x08,
            0x00, 0x01,
            0x87.toByte(), 0x69, 0x00, 0x04,
            0x00, 0x00, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x1A,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x01,
            0x90.toByte(), 0x03, 0x00, 0x02,
            0x00, 0x00, 0x00, 0x14,
            0x00, 0x00, 0x00, 0x2C,
            0x00, 0x00, 0x00, 0x00,
        ) + "2021:02:03 04:05:06\u0000".encodeToByteArray()
        val payload = exifHeader + tiff
        val segmentLength = payload.size + 2

        return byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE1.toByte(),
            ((segmentLength shr 8) and 0xFF).toByte(),
            (segmentLength and 0xFF).toByte(),
        ) + payload + byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    }

    private fun expectedCapturedAtEpochMillis(): Long {
        return LocalDateTime.of(2021, 2, 3, 4, 5, 6)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
