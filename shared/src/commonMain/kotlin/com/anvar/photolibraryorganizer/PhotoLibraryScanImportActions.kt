package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.AppResult
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportStorageSpace
import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.ImportPlanTargetResolver
import com.anvar.photolibraryorganizer.domain.repository.UnsupportedFileQuarantineRepository
import com.anvar.photolibraryorganizer.presentation.ImportHistoryStorage
import com.anvar.photolibraryorganizer.presentation.ImportReport
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.ScanUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * Coordinates source scanning, import preparation, and import execution.
 */
internal class PhotoLibraryScanImportActions(
    private val appState: PhotoLibraryAppState,
    private val useCases: PhotoLibraryUseCases,
    private val importPlanTargetResolver: ImportPlanTargetResolver,
    private val unsupportedFileQuarantineRepository: UnsupportedFileQuarantineRepository,
    private val importHistoryStorage: ImportHistoryStorage,
    private val libraryActions: PhotoLibraryLibraryActions,
    private val coroutineScope: CoroutineScope,
) {
    fun selectImportMode(importMode: ImportMode) {
        appState.importMode = importMode
        appState.importUiState = ImportUiState.Idle
    }

    fun startScan(plan: PhotoLibraryPlan) {
        val currentScanToken = appState.scanRequestToken + 1
        appState.scanRequestToken = currentScanToken
        appState.scanJob?.cancel()
        appState.scanJob = coroutineScope.launch {
            appState.scanUiState = ScanUiState.Loading()
            appState.importUiState = ImportUiState.Idle
            appState.lastImportReport = null
            appState.emptyFolderCleanupMessage = null
            appState.emptyFolderCleanupAwaitingConfirmation = false
            appState.duplicateActionMessage = null
            appState.duplicateDeleteAwaitingConfirmation = false
            appState.unsupportedActionMessage = null
            appState.unsupportedDeleteAwaitingConfirmation = false
            appState.libraryFiles = emptyList()
            appState.duplicateFiles = emptyList()
            try {
                when (
                    val result = withContext(Dispatchers.Default) {
                        useCases.scanSourceFolder(appState.sourceFolder) { progress ->
                            coroutineScope.launch {
                                if (appState.scanRequestToken == currentScanToken && appState.scanUiState is ScanUiState.Loading) {
                                    appState.scanUiState = ScanUiState.Loading(progress)
                                }
                            }
                        }
                    }
                ) {
                    is AppResult.Success -> {
                        if (appState.scanRequestToken != currentScanToken) return@launch
                        val plannedFiles = useCases.buildMediaFilePlan(
                            destinationFolder = appState.destinationFolder,
                            mediaFiles = result.data.mediaFiles,
                            importRules = plan.importRules,
                        )
                        val resolvedFiles = importPlanTargetResolver.resolve(plannedFiles)
                        ScanUiState.Success(
                            summary = result.data.summary,
                            plannedFiles = resolvedFiles,
                            unsupportedFiles = result.data.unsupportedFiles,
                        ).also {
                            appState.scanUiState = it
                            appState.selectedFile = it.plannedFiles.firstOrNull()
                        }
                    }

                    is AppResult.Error -> {
                        if (appState.scanRequestToken != currentScanToken) return@launch
                        val message = result.error.toUserMessage()
                        appState.addIssue(uiText("Сканирование", "Scan"), message)
                        appState.scanUiState = ScanUiState.Error(message)
                    }
                }
            } catch (exception: CancellationException) {
                if (appState.scanRequestToken == currentScanToken) {
                    appState.scanUiState = ScanUiState.Canceled
                }
            } catch (exception: Throwable) {
                if (appState.scanRequestToken != currentScanToken) return@launch
                val message = uiText(
                    ru = "Сканирование прервалось: ${exception.message ?: "без деталей"}",
                    en = "Scan failed: ${exception.message ?: "no details"}",
                )
                appState.addIssue(uiText("Сканирование", "Scan"), message)
                appState.scanUiState = ScanUiState.Error(message)
            } finally {
                if (appState.scanRequestToken == currentScanToken) {
                    appState.scanJob = null
                }
            }
        }
    }

    fun cancelScan() {
        val activeScanJob = appState.scanJob
        if (activeScanJob != null) {
            appState.scanRequestToken += 1
            activeScanJob.cancel()
            appState.scanJob = null
            appState.scanUiState = ScanUiState.Canceled
        }
    }

    fun prepareImport() {
        val targetFolder = appState.destinationFolder
        if (targetFolder.isNullOrBlank()) {
            appState.importUiState = ImportUiState.Error(uiText("Не выбрана папка библиотеки.", "Library folder is not selected."))
            return
        }

        appState.importUiState = ImportUiState.CheckingStorageSpace
        coroutineScope.launch {
            try {
                val currentScanSuccess = appState.scanUiState as? ScanUiState.Success
                val plannedFiles = withContext(Dispatchers.Default) {
                    importPlanTargetResolver.resolve(currentScanSuccess?.plannedFiles.orEmpty())
                }
                if (currentScanSuccess != null) {
                    appState.scanUiState = currentScanSuccess.copy(plannedFiles = plannedFiles)
                }
                val space = if (appState.importMode == ImportMode.Copy) {
                    withContext(Dispatchers.IO) {
                        useCases.checkImportStorageSpace(targetFolder, plannedFiles)
                    }
                } else {
                    null
                }
                appState.importUiState = importConfirmationState(space, plannedFiles, currentScanSuccess?.unsupportedFiles.orEmpty().size)
            } catch (exception: Throwable) {
                val message = uiText(
                    ru = "Не удалось подготовить импорт: ${exception.message ?: "без деталей"}",
                    en = "Could not prepare import: ${exception.message ?: "no details"}",
                )
                appState.addIssue(uiText("Импорт", "Import"), message)
                appState.importUiState = ImportUiState.Error(message)
            }
        }
    }

    fun cancelImportConfirmation() {
        appState.importUiState = ImportUiState.Idle
    }

    fun confirmImport() {
        appState.importJob?.cancel()
        appState.importJob = coroutineScope.launch {
            val currentScanSuccess = appState.scanUiState as? ScanUiState.Success
            val scannedPlannedFiles = currentScanSuccess?.plannedFiles.orEmpty()
            val plannedFiles = withContext(Dispatchers.Default) {
                importPlanTargetResolver.resolve(scannedPlannedFiles)
            }
            if (currentScanSuccess != null) {
                appState.scanUiState = currentScanSuccess.copy(plannedFiles = plannedFiles)
            }
            if (appState.importMode == ImportMode.Copy && !ensureCopySpace(plannedFiles)) return@launch

            val unsupportedSourceFiles = (appState.scanUiState as? ScanUiState.Success)?.unsupportedFiles.orEmpty()
            val readyFileCount = plannedFiles.count { it.targetStatus != ImportTargetStatus.AlreadyExists }
            val existingFileCount = plannedFiles.count { it.targetStatus == ImportTargetStatus.AlreadyExists }
            appState.lastImportProgress = null
            appState.importUiState = ImportUiState.Loading()
            try {
                when (
                    val result = withContext(Dispatchers.Default) {
                        useCases.importMediaFiles(appState.importMode, plannedFiles) { progress ->
                            coroutineScope.launch {
                                if (appState.importUiState is ImportUiState.Loading) {
                                    appState.lastImportProgress = progress
                                    appState.importUiState = ImportUiState.Loading(progress)
                                }
                            }
                        }
                    }
                ) {
                    is AppResult.Success -> {
                        val unsupportedQuarantineResult = if (appState.importMode == ImportMode.Move && unsupportedSourceFiles.isNotEmpty()) {
                            withContext(Dispatchers.Default) {
                                unsupportedFileQuarantineRepository.moveToQuarantine(
                                    destinationFolder = appState.destinationFolder,
                                    unsupportedFiles = unsupportedSourceFiles,
                                )
                            }
                        } else {
                            null
                        }
                        val finalImportResult = result.data.withUnsupportedQuarantine(
                            result = unsupportedQuarantineResult,
                            fallbackFailedFiles = unsupportedSourceFiles.size,
                        )
                        val report = ImportReport(
                            importMode = appState.importMode,
                            plannedFiles = plannedFiles.size,
                            readyFiles = readyFileCount,
                            existingFiles = existingFileCount,
                            copiedFiles = finalImportResult.copiedFiles,
                            movedFiles = finalImportResult.movedFiles,
                            skippedFiles = finalImportResult.skippedFiles,
                            failedFiles = finalImportResult.failedFiles,
                            failedUnsupportedFiles = finalImportResult.failedUnsupportedFiles,
                            createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
                        )
                        appState.lastImportReport = report
                        appState.emptyFolderCleanupMessage = null
                        appState.emptyFolderCleanupAwaitingConfirmation = false
                        importHistoryStorage.appendReport(report)
                        appState.importHistory = importHistoryStorage.loadHistory()
                        libraryActions.refreshLibraryIndexFromDisk(selectFirstFile = false)
                        if (finalImportResult.failedFiles > 0 || finalImportResult.failedUnsupportedFiles > 0) {
                            appState.addIssue(
                                title = uiText("Импорт", "Import"),
                                detail = uiText(
                                    ru = "Ошибки медиа: ${finalImportResult.failedFiles}. Ошибки неподдерживаемых: ${finalImportResult.failedUnsupportedFiles}.",
                                    en = "Media errors: ${finalImportResult.failedFiles}. Unsupported-file errors: ${finalImportResult.failedUnsupportedFiles}.",
                                ),
                            )
                        }
                        appState.importUiState = ImportUiState.Success(finalImportResult)
                    }

                    is AppResult.Error -> {
                        val message = result.error.toUserMessage()
                        appState.addIssue(uiText("Импорт", "Import"), message)
                        appState.importUiState = ImportUiState.Error(message)
                    }
                }
            } catch (exception: CancellationException) {
                appState.importUiState = ImportUiState.Canceled(appState.lastImportProgress)
                libraryActions.refreshLibraryIndexFromDisk(selectFirstFile = false)
            } catch (exception: Throwable) {
                val message = uiText(
                    ru = "Импорт прервался: ${exception.message ?: "без деталей"}",
                    en = "Import failed: ${exception.message ?: "no details"}",
                )
                appState.addIssue(uiText("Импорт", "Import"), message)
                appState.importUiState = ImportUiState.Error(message)
            } finally {
                appState.importJob = null
            }
        }
    }

    fun cancelRunningImport() {
        appState.importJob?.cancel()
        appState.importUiState = ImportUiState.Canceled(appState.lastImportProgress)
        coroutineScope.launch {
            libraryActions.refreshLibraryIndexFromDisk(selectFirstFile = false)
        }
    }

    private suspend fun ensureCopySpace(plannedFiles: List<PlannedMediaFile>): Boolean {
        return when (val space = withContext(Dispatchers.IO) {
            useCases.checkImportStorageSpace(appState.destinationFolder.orEmpty(), plannedFiles)
        }) {
            is ImportStorageSpace.Insufficient -> {
                appState.importUiState = ImportUiState.Error(
                    uiText(
                        ru = "Недостаточно места в папке библиотеки: нужно ${space.requiredBytes.toReadableSize()}, свободно ${space.availableBytes.toReadableSize()}.",
                        en = "Not enough space in the library folder: required ${space.requiredBytes.toReadableSize()}, available ${space.availableBytes.toReadableSize()}.",
                    ),
                )
                false
            }

            ImportStorageSpace.Unavailable -> {
                appState.importUiState = ImportUiState.Error(
                    uiText(
                        ru = "Не удалось определить свободное место в папке библиотеки. Проверь доступ к диску.",
                        en = "Could not determine free space in the library folder. Check disk access.",
                    ),
                )
                false
            }

            is ImportStorageSpace.Available -> true
        }
    }

    private fun importConfirmationState(
        space: ImportStorageSpace?,
        plannedFiles: List<PlannedMediaFile>,
        unsupportedFileCount: Int,
    ): ImportUiState {
        return when (space) {
            is ImportStorageSpace.Insufficient -> ImportUiState.Error(
                uiText(
                    ru = "Недостаточно места в папке библиотеки: нужно ${space.requiredBytes.toReadableSize()}, свободно ${space.availableBytes.toReadableSize()}.",
                    en = "Not enough space in the library folder: required ${space.requiredBytes.toReadableSize()}, available ${space.availableBytes.toReadableSize()}.",
                ),
            )

            ImportStorageSpace.Unavailable -> ImportUiState.Error(
                uiText(
                    ru = "Не удалось определить свободное место в папке библиотеки. Проверь доступ к диску.",
                    en = "Could not determine free space in the library folder. Check disk access.",
                ),
            )

            is ImportStorageSpace.Available -> ImportUiState.AwaitingConfirmation(
                readyFileCount = plannedFiles.count { it.targetStatus != ImportTargetStatus.AlreadyExists },
                existingFileCount = plannedFiles.count { it.targetStatus == ImportTargetStatus.AlreadyExists },
                unsupportedFileCount = unsupportedFileCount,
                requiredBytes = space.requiredBytes,
                availableBytes = space.availableBytes,
            )

            null -> ImportUiState.AwaitingConfirmation(
                readyFileCount = plannedFiles.count { it.targetStatus != ImportTargetStatus.AlreadyExists },
                existingFileCount = plannedFiles.count { it.targetStatus == ImportTargetStatus.AlreadyExists },
                unsupportedFileCount = unsupportedFileCount,
            )
        }
    }
}
