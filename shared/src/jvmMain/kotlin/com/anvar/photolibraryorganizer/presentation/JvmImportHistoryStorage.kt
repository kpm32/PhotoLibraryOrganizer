package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.ImportMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

class JvmImportHistoryStorage(
    private val historyPath: Path = defaultHistoryPath(),
) : ImportHistoryStorage {
    override suspend fun loadHistory(): List<ImportReport> = withContext(Dispatchers.IO) {
        if (!historyPath.exists()) {
            return@withContext emptyList()
        }

        historyPath.readLines()
            .mapNotNull { line -> line.toImportReportOrNull() }
            .sortedByDescending { it.createdAtEpochMillis }
    }

    override suspend fun appendReport(report: ImportReport) = withContext(Dispatchers.IO) {
        historyPath.parent?.createDirectories()
        val history = (loadHistory() + report)
            .sortedByDescending { it.createdAtEpochMillis }
            .take(MAX_HISTORY_ENTRIES)
        historyPath.writeLines(history.map { it.toHistoryLine() })
        Unit
    }

    private fun ImportReport.toHistoryLine(): String {
        return listOf(
            createdAtEpochMillis,
            importMode.name,
            plannedFiles,
            readyFiles,
            existingFiles,
            copiedFiles,
            movedFiles,
            skippedFiles,
            failedFiles,
            failedUnsupportedFiles,
        ).joinToString(separator = "\t")
    }

    private fun String.toImportReportOrNull(): ImportReport? {
        val parts = split('\t')
        if (parts.size !in LEGACY_HISTORY_FIELD_COUNT..HISTORY_FIELD_COUNT) return null
        return try {
            ImportReport(
                createdAtEpochMillis = parts[0].toLong(),
                importMode = ImportMode.valueOf(parts[1]),
                plannedFiles = parts[2].toInt(),
                readyFiles = parts[3].toInt(),
                existingFiles = parts[4].toInt(),
                copiedFiles = parts[5].toInt(),
                movedFiles = parts[6].toInt(),
                skippedFiles = parts[7].toInt(),
                failedFiles = parts[8].toInt(),
                failedUnsupportedFiles = parts.getOrNull(9)?.toInt() ?: 0,
            )
        } catch (exception: Throwable) {
            null
        }
    }

    private companion object {
        const val LEGACY_HISTORY_FIELD_COUNT = 9
        const val HISTORY_FIELD_COUNT = 10
        const val MAX_HISTORY_ENTRIES = 200

        fun defaultHistoryPath(): Path {
            return Path.of(
                System.getProperty("user.home"),
                "Library",
                "Application Support",
                "PhotoLibraryOrganizer",
                "import-history.tsv",
            )
        }
    }
}
