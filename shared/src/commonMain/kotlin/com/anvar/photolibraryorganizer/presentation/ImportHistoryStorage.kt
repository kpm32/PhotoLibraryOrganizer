package com.anvar.photolibraryorganizer.presentation

interface ImportHistoryStorage {
    suspend fun loadHistory(): List<ImportReport>
    suspend fun appendReport(report: ImportReport)
}

object PreviewImportHistoryStorage : ImportHistoryStorage {
    override suspend fun loadHistory(): List<ImportReport> = emptyList()
    override suspend fun appendReport(report: ImportReport) = Unit
}
