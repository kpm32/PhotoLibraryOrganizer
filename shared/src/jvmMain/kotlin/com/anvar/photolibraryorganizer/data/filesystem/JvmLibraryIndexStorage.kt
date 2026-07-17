package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.model.LibraryIndexSnapshot
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.LibraryIndexStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.StandardCopyOption
import java.util.Base64
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

/**
 * TSV-backed local library index for the desktop app.
 *
 * The index is intentionally simple and rebuildable. Saves use unique temporary
 * files so overlapping refreshes cannot fight over a shared `.tmp` path.
 */
class JvmLibraryIndexStorage(
    private val indexPath: Path = defaultIndexPath(),
) : LibraryIndexStorage {
    override suspend fun load(destinationFolder: String?): LibraryIndexSnapshot? = withContext(Dispatchers.IO) {
        val normalizedDestination = destinationFolder.normalizedDestinationOrNull() ?: return@withContext null
        if (!indexPath.exists()) return@withContext null

        val lines = indexPath.readLines()
        val destinationLine = lines.firstOrNull { it.startsWith("$DESTINATION_PREFIX\t") } ?: return@withContext null
        val indexedDestination = destinationLine.substringAfter('\t').decodeField()
        if (indexedDestination != normalizedDestination) return@withContext null

        var updatedAtEpochMillis = 0L
        val libraryFiles = mutableListOf<PlannedMediaFile>()
        val duplicateFiles = mutableListOf<PlannedMediaFile>()
        val unsupportedFiles = mutableListOf<PlannedMediaFile>()

        lines.forEach { line ->
            val parts = line.split('\t')
            when (parts.firstOrNull()) {
                UPDATED_AT_PREFIX -> updatedAtEpochMillis = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                SECTION_LIBRARY -> parts.toPlannedMediaFileOrNull()?.let(libraryFiles::add)
                SECTION_DUPLICATES -> parts.toPlannedMediaFileOrNull()?.let(duplicateFiles::add)
                SECTION_UNSUPPORTED -> parts.toPlannedMediaFileOrNull()?.let(unsupportedFiles::add)
            }
        }

        LibraryIndexSnapshot(
            destinationFolder = normalizedDestination,
            libraryFiles = libraryFiles,
            duplicateFiles = duplicateFiles,
            unsupportedFiles = unsupportedFiles,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }

    override suspend fun save(snapshot: LibraryIndexSnapshot) = withContext(Dispatchers.IO) {
        val normalizedDestination = snapshot.destinationFolder.normalizedDestinationOrNull() ?: return@withContext
        val indexFolder = indexPath.parent ?: Path.of(".")
        indexFolder.createDirectories()
        val tempPath = Files.createTempFile(indexFolder, "${indexPath.fileName}.", ".tmp")
        val lines = buildList {
            add("$VERSION_PREFIX\t$INDEX_VERSION")
            add("$DESTINATION_PREFIX\t${normalizedDestination.encodeField()}")
            add("$UPDATED_AT_PREFIX\t${snapshot.updatedAtEpochMillis}")
            snapshot.libraryFiles.forEach { add(it.toIndexLine(SECTION_LIBRARY)) }
            snapshot.duplicateFiles.forEach { add(it.toIndexLine(SECTION_DUPLICATES)) }
            snapshot.unsupportedFiles.forEach { add(it.toIndexLine(SECTION_UNSUPPORTED)) }
        }
        try {
            tempPath.writeLines(lines)
            try {
                Files.move(tempPath, indexPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (exception: AtomicMoveNotSupportedException) {
                Files.move(tempPath, indexPath, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(tempPath)
        }
        Unit
    }

    private fun PlannedMediaFile.toIndexLine(section: String): String {
        return listOf(
            section,
            sourcePath.encodeField(),
            fileName.encodeField(),
            targetRelativePath.encodeField(),
            sizeBytes.toString(),
            contentHash.orEmpty().encodeField(),
            capturedAtEpochMillis?.toString().orEmpty(),
            modifiedAtEpochMillis?.toString().orEmpty(),
        ).joinToString(separator = "\t")
    }

    private fun List<String>.toPlannedMediaFileOrNull(): PlannedMediaFile? {
        if (size < INDEX_FIELD_COUNT) return null
        return try {
            PlannedMediaFile(
                sourcePath = this[1].decodeField(),
                fileName = this[2].decodeField(),
                targetRelativePath = this[3].decodeField(),
                sizeBytes = this[4].toLong(),
                contentHash = this[5].decodeField().ifBlank { null },
                capturedAtEpochMillis = this[6].toLongOrNull(),
                modifiedAtEpochMillis = this[7].toLongOrNull(),
            )
        } catch (exception: Throwable) {
            null
        }
    }

    private fun String?.normalizedDestinationOrNull(): String? {
        return this?.trim()?.trimEnd('/')?.takeIf { it.isNotBlank() }
    }

    private fun String.encodeField(): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray(Charsets.UTF_8))
    }

    private fun String.decodeField(): String {
        return String(Base64.getUrlDecoder().decode(this), Charsets.UTF_8)
    }

    private companion object {
        const val INDEX_VERSION = 1
        const val INDEX_FIELD_COUNT = 8
        const val VERSION_PREFIX = "version"
        const val DESTINATION_PREFIX = "destination"
        const val UPDATED_AT_PREFIX = "updatedAt"
        const val SECTION_LIBRARY = "library"
        const val SECTION_DUPLICATES = "duplicates"
        const val SECTION_UNSUPPORTED = "unsupported"

        fun defaultIndexPath(): Path {
            return Path.of(
                System.getProperty("user.home"),
                "Library",
                "Application Support",
                "PhotoLibraryOrganizer",
                "library-index.tsv",
            )
        }
    }
}
