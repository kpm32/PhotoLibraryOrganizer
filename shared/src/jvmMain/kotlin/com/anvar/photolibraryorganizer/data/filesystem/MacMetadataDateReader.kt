package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.model.MediaFileCategory
import java.nio.file.Path
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal class MacMetadataDateReader(
    private val metadataOutputProvider: (Path) -> String? = ::readMdlsOutput,
) : CapturedDateReader {
    override fun readCapturedAtEpochMillis(
        path: Path,
        extension: String,
        category: MediaFileCategory,
    ): Long? {
        if (!supportsMetadataDate(extension = extension, category = category)) return null

        return runCatching {
            val output = metadataOutputProvider(path) ?: return null

            output.lineSequence()
                .mapNotNull { line -> line.trim().takeIf { it.isNotBlank() && it != NullValue } }
                .mapNotNull(::parseMetadataDate)
                .firstOrNull()
        }.getOrNull()
    }

    private fun supportsMetadataDate(
        extension: String,
        category: MediaFileCategory,
    ): Boolean {
        return category == MediaFileCategory.Video || extension in MetadataImageExtensions
    }

    private fun parseMetadataDate(value: String): Long? {
        return runCatching {
            OffsetDateTime.parse(value, MetadataDateFormatter)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }

    private companion object {
        const val ContentCreationDateAttribute = "kMDItemContentCreationDate"
        const val FileSystemCreationDateAttribute = "kMDItemFSCreationDate"
        const val NullValue = "(null)"
        val MetadataDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z")
        val MetadataImageExtensions = setOf("heic", "heif")

        private fun readMdlsOutput(path: Path): String {
            return ProcessBuilder(
                "mdls",
                "-raw",
                "-name",
                ContentCreationDateAttribute,
                "-name",
                FileSystemCreationDateAttribute,
                path.toString(),
            )
                .redirectErrorStream(true)
                .start()
                .let { process ->
                    val text = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    text
                }
        }
    }
}
