package com.anvar.photolibraryorganizer.data.filesystem

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId

internal class JpegExifDateReader {
    fun readCapturedAtEpochMillis(path: Path): Long? {
        return runCatching {
            val bytes = Files.readAllBytes(path)
            val tiff = findExifTiff(bytes) ?: return null
            val dateTime = readDateTimeOriginal(tiff) ?: readDateTime(tiff) ?: return null
            parseExifDateTime(dateTime)
        }.getOrNull()
    }

    private fun findExifTiff(bytes: ByteArray): ByteArray? {
        if (bytes.size < 4 || bytes[0] != JpegMarker || bytes[1] != StartOfImageMarker) {
            return null
        }

        var offset = 2
        while (offset + 4 < bytes.size) {
            if (bytes[offset] != JpegMarker) return null

            val marker = bytes[offset + 1].toInt() and 0xFF
            if (marker == StartOfScanMarker || marker == EndOfImageMarker) return null

            val segmentLength = readUnsignedShort(bytes, offset + 2)
            val segmentStart = offset + 4
            val segmentEnd = offset + 2 + segmentLength
            if (segmentLength < 2 || segmentEnd > bytes.size) return null

            if (marker == App1Marker && hasExifHeader(bytes, segmentStart)) {
                return bytes.copyOfRange(segmentStart + ExifHeader.size, segmentEnd)
            }
            offset = segmentEnd
        }

        return null
    }

    private fun readDateTimeOriginal(tiff: ByteArray): String? {
        return readExifTag(tiff, DateTimeOriginalTag)
    }

    private fun readDateTime(tiff: ByteArray): String? {
        return readExifTag(tiff, DateTimeTag)
    }

    private fun readExifTag(
        tiff: ByteArray,
        tag: Int,
    ): String? {
        if (tiff.size < 8) return null

        val byteOrder = when {
            tiff[0] == 'I'.code.toByte() && tiff[1] == 'I'.code.toByte() -> ByteOrder.LITTLE_ENDIAN
            tiff[0] == 'M'.code.toByte() && tiff[1] == 'M'.code.toByte() -> ByteOrder.BIG_ENDIAN
            else -> return null
        }
        val buffer = ByteBuffer.wrap(tiff).order(byteOrder)
        val firstIfdOffset = buffer.readInt(4)

        return readTagFromIfd(tiff, buffer, firstIfdOffset, tag)
            ?: readExifSubIfd(tiff, buffer, firstIfdOffset, tag)
    }

    private fun readExifSubIfd(
        tiff: ByteArray,
        buffer: ByteBuffer,
        firstIfdOffset: Int,
        tag: Int,
    ): String? {
        val exifSubIfdOffset = readTagValueOffset(tiff, buffer, firstIfdOffset, ExifSubIfdPointerTag)
            ?: return null
        return readTagFromIfd(tiff, buffer, exifSubIfdOffset, tag)
    }

    private fun readTagFromIfd(
        tiff: ByteArray,
        buffer: ByteBuffer,
        ifdOffset: Int,
        tag: Int,
    ): String? {
        if (ifdOffset < 0 || ifdOffset + 2 > tiff.size) return null

        val entryCount = buffer.readUnsignedShort(ifdOffset)
        val entriesStart = ifdOffset + 2
        repeat(entryCount) { index ->
            val entryOffset = entriesStart + index * IfdEntrySize
            if (entryOffset + IfdEntrySize > tiff.size) return null

            if (buffer.readUnsignedShort(entryOffset) == tag) {
                val type = buffer.readUnsignedShort(entryOffset + 2)
                val count = buffer.readInt(entryOffset + 4)
                val valueOffset = buffer.readInt(entryOffset + 8)
                if (type != AsciiType || count <= 0) return null

                val stringOffset = if (count <= InlineValueSize) entryOffset + 8 else valueOffset
                if (stringOffset < 0 || stringOffset + count > tiff.size) return null

                return tiff.copyOfRange(stringOffset, stringOffset + count)
                    .decodeToString()
                    .trimEnd('\u0000', ' ')
            }
        }

        return null
    }

    private fun readTagValueOffset(
        tiff: ByteArray,
        buffer: ByteBuffer,
        ifdOffset: Int,
        tag: Int,
    ): Int? {
        if (ifdOffset < 0 || ifdOffset + 2 > tiff.size) return null

        val entryCount = buffer.readUnsignedShort(ifdOffset)
        val entriesStart = ifdOffset + 2
        repeat(entryCount) { index ->
            val entryOffset = entriesStart + index * IfdEntrySize
            if (entryOffset + IfdEntrySize > tiff.size) return null

            if (buffer.readUnsignedShort(entryOffset) == tag) {
                return buffer.readInt(entryOffset + 8)
            }
        }

        return null
    }

    private fun parseExifDateTime(value: String): Long? {
        val match = ExifDateTimeRegex.matchEntire(value) ?: return null
        val dateTime = LocalDateTime.of(
            match.groupValues[1].toInt(),
            match.groupValues[2].toInt(),
            match.groupValues[3].toInt(),
            match.groupValues[4].toInt(),
            match.groupValues[5].toInt(),
            match.groupValues[6].toInt(),
        )
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun hasExifHeader(
        bytes: ByteArray,
        offset: Int,
    ): Boolean {
        return offset + ExifHeader.size <= bytes.size &&
            ExifHeader.indices.all { bytes[offset + it] == ExifHeader[it] }
    }

    private fun readUnsignedShort(
        bytes: ByteArray,
        offset: Int,
    ): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
    }

    private fun ByteBuffer.readUnsignedShort(offset: Int): Int {
        return getShort(offset).toInt() and 0xFFFF
    }

    private fun ByteBuffer.readInt(offset: Int): Int {
        return getInt(offset)
    }

    private companion object {
        const val App1Marker = 0xE1
        const val AsciiType = 2
        const val DateTimeTag = 0x0132
        const val DateTimeOriginalTag = 0x9003
        const val EndOfImageMarker = 0xD9
        const val ExifSubIfdPointerTag = 0x8769
        const val IfdEntrySize = 12
        const val InlineValueSize = 4
        const val StartOfImageMarker = 0xD8.toByte()
        const val StartOfScanMarker = 0xDA
        val JpegMarker = 0xFF.toByte()
        val ExifHeader = byteArrayOf('E'.code.toByte(), 'x'.code.toByte(), 'i'.code.toByte(), 'f'.code.toByte(), 0, 0)
        val ExifDateTimeRegex = Regex("""(\d{4}):(\d{2}):(\d{2}) (\d{2}):(\d{2}):(\d{2})""")
    }
}
