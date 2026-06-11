package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.ImportMode
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmImportHistoryStorageTest {
    @Test
    fun savesAndLoadsImportReportsNewestFirst() = runTest {
        val historyPath = Files.createTempDirectory("photo-import-history-test")
            .resolve("history.tsv")
        val storage = JvmImportHistoryStorage(historyPath)

        storage.appendReport(report(createdAtEpochMillis = 1000, copiedFiles = 3))
        storage.appendReport(report(createdAtEpochMillis = 2000, copiedFiles = 7))

        val history = storage.loadHistory()

        assertEquals(listOf(2000L, 1000L), history.map { it.createdAtEpochMillis })
        assertEquals(7, history.first().copiedFiles)
    }

    @Test
    fun ignoresBrokenHistoryLines() = runTest {
        val historyPath = Files.createTempDirectory("photo-import-broken-history-test")
            .resolve("history.tsv")
        historyPath.writeText("broken line\n1000\tCopy\t1\t1\t0\t1\t0\t0\t0\n")
        val storage = JvmImportHistoryStorage(historyPath)

        val history = storage.loadHistory()

        assertEquals(1, history.size)
        assertEquals(ImportMode.Copy, history.single().importMode)
    }

    private fun report(
        createdAtEpochMillis: Long,
        copiedFiles: Int,
    ): ImportReport {
        return ImportReport(
            importMode = ImportMode.Copy,
            plannedFiles = copiedFiles,
            readyFiles = copiedFiles,
            existingFiles = 0,
            copiedFiles = copiedFiles,
            movedFiles = 0,
            skippedFiles = 0,
            failedFiles = 0,
            createdAtEpochMillis = createdAtEpochMillis,
        )
    }
}
