package com.anvar.photolibraryorganizer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.ImportFailureDetail
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.presentation.ImportReport
import com.anvar.photolibraryorganizer.presentation.ImportRulesPreset
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.ScanUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Import workflow panel: source scan, plan review, copy/move execution, and
 * last-import diagnostics.
 */
@Composable
internal fun ImportPanel(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importUiState: ImportUiState,
    lastImportReport: ImportReport?,
    emptyFolderCleanupMessage: String?,
    emptyFolderCleanupAwaitingConfirmation: Boolean,
    importHistory: List<ImportReport>,
    importAvailability: ImportAvailability,
    selectedMode: ImportMode,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImportClick: () -> Unit,
    onCancelImportClick: () -> Unit,
    onCancelRunningImportClick: () -> Unit,
    onRequestEmptyFolderCleanupClick: () -> Unit,
    onConfirmEmptyFolderCleanupClick: () -> Unit,
    onCancelEmptyFolderCleanupClick: () -> Unit,
    onOpenLibraryFolderClick: () -> Unit,
    onOpenUnsupportedFolderClick: () -> Unit,
    onOpenDuplicatesFolderClick: () -> Unit,
    onImportModeSelected: (ImportMode) -> Unit,
    onImportRulesSelected: (ImportOrganizationRules) -> Unit,
    onCancelScanClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = uiText("Панель импорта", "Import Panel"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            Text(
                text = scanStatusText(plan, scanUiState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ScanProgressIndicator(scanUiState)
            if (scanUiState is ScanUiState.Success) {
                Text(
                    text = uiText(
                        ru = "Если в JPEG есть EXIF-дата съемки, используем ее. Для остальных файлов берем дату изменения.",
                        en = "JPEG capture dates use EXIF when available. Other files use the modified date.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ImportAvailabilityHint(importAvailability)
            }
            ImportStatus(importUiState)
            ImportConfirmation(
                importUiState = importUiState,
                selectedMode = selectedMode,
                onConfirmImportClick = onConfirmImportClick,
                onCancelImportClick = onCancelImportClick,
            )
            ImportReportSummary(
                lastImportReport = lastImportReport,
                importUiState = importUiState,
                onOpenLibraryFolderClick = onOpenLibraryFolderClick,
                onOpenUnsupportedFolderClick = onOpenUnsupportedFolderClick,
                onOpenDuplicatesFolderClick = onOpenDuplicatesFolderClick,
            )
            EmptyFolderCleanupAction(
                lastImportReport = lastImportReport,
                message = emptyFolderCleanupMessage,
                awaitingConfirmation = emptyFolderCleanupAwaitingConfirmation,
                onRequestClick = onRequestEmptyFolderCleanupClick,
                onConfirmClick = onConfirmEmptyFolderCleanupClick,
                onCancelClick = onCancelEmptyFolderCleanupClick,
            )
            ImportHistorySummary(importHistory)
            ImportRulesSummary(plan.importRules)
            ImportRulesPresetPicker(
                selectedRules = plan.importRules,
                onImportRulesSelected = onImportRulesSelected,
            )
            ImportModeChips(
                selectedMode = selectedMode,
                onImportModeSelected = onImportModeSelected,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = plan.canScan && scanUiState !is ScanUiState.Loading,
                ) {
                    Text(
                        if (scanUiState is ScanUiState.Loading) {
                            uiText("Сканирую...", "Scanning...")
                        } else {
                            uiText("Сканировать", "Scan")
                        },
                    )
                }
                if (scanUiState is ScanUiState.Loading) {
                    OutlinedButton(
                        onClick = onCancelScanClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(uiText("Остановить сканирование", "Stop Scan"))
                    }
                }
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importAvailability is ImportAvailability.Available &&
                        importUiState !is ImportUiState.CheckingStorageSpace &&
                        importUiState !is ImportUiState.AwaitingConfirmation &&
                        importUiState !is ImportUiState.Loading,
                ) {
                    Text(
                        if (importUiState is ImportUiState.Loading) {
                            uiText("Импортирую...", "Importing...")
                        } else {
                            uiText("Импорт", "Import")
                        },
                    )
                }
                if (importUiState is ImportUiState.Loading) {
                    OutlinedButton(
                        onClick = onCancelRunningImportClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(uiText("Остановить импорт", "Stop Import"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanProgressIndicator(scanUiState: ScanUiState) {
    if (scanUiState !is ScanUiState.Loading) return

    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    scanUiState.progress?.let { progress ->
        Text(
            text = uiText(
                ru = "Просмотрено: ${progress.scannedFiles}. Медиа: ${progress.mediaFiles}. Пропущено: ${progress.unsupportedFiles}.",
                en = "Scanned: ${progress.scannedFiles}. Media: ${progress.mediaFiles}. Skipped: ${progress.unsupportedFiles}.",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (progress.unsupportedFileExtensions.isNotEmpty()) {
            Text(
                text = uiText(
                    ru = "Пропущенные типы: ${progress.unsupportedFileExtensions.toReadableUnsupportedExtensions()}",
                    en = "Skipped types: ${progress.unsupportedFileExtensions.toReadableUnsupportedExtensions()}",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ImportHistorySummary(importHistory: List<ImportReport>) {
    if (importHistory.isEmpty()) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = uiText("История импортов", "Import History"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            importHistory.take(3).forEach { report ->
                CompactRuleRow(
                    label = report.createdAtEpochMillis.toReadableDateTime(),
                    value = uiText(
                        ru = "${report.importMode.titleText()}: ${report.readyFiles} файлов, ошибок ${report.totalFailedFiles}",
                        en = "${report.importMode.titleText()}: ${report.readyFiles} files, ${report.totalFailedFiles} errors",
                    ),
                )
            }
        }
    }
}

@Composable
private fun ImportRulesPresetPicker(
    selectedRules: ImportOrganizationRules,
    onImportRulesSelected: (ImportOrganizationRules) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = uiText("Структура папок", "Folder Structure"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ImportRulesPreset.entries.forEach { preset ->
                val selected = preset.rules == selectedRules
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onImportRulesSelected(preset.rules) },
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = preset.titleText(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = preset.descriptionText(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportReportSummary(
    lastImportReport: ImportReport?,
    importUiState: ImportUiState,
    onOpenLibraryFolderClick: () -> Unit,
    onOpenUnsupportedFolderClick: () -> Unit,
    onOpenDuplicatesFolderClick: () -> Unit,
) {
    if (lastImportReport == null) return
    val importResult = (importUiState as? ImportUiState.Success)?.result

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = uiText("Последний импорт", "Last Import"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            CompactRuleRow(uiText("Режим", "Mode"), lastImportReport.importMode.titleText())
            CompactRuleRow(
                label = uiText("План", "Plan"),
                value = uiText(
                    ru = "${lastImportReport.readyFiles} к импорту, ${lastImportReport.existingFiles} уже есть",
                    en = "${lastImportReport.readyFiles} to import, ${lastImportReport.existingFiles} already exist",
                ),
            )
            CompactRuleRow(
                label = uiText("Итог", "Result"),
                value = uiText(
                    ru = "копий ${lastImportReport.copiedFiles}, переносов ${lastImportReport.movedFiles}, пропусков ${lastImportReport.skippedFiles}, ошибок ${lastImportReport.totalFailedFiles}",
                    en = "copied ${lastImportReport.copiedFiles}, moved ${lastImportReport.movedFiles}, skipped ${lastImportReport.skippedFiles}, errors ${lastImportReport.totalFailedFiles}",
                ),
            )
            CompactRuleRow(uiText("Ошибки медиа", "Media errors"), lastImportReport.failedFiles.toString())
            CompactRuleRow(uiText("Ошибки пропущенных", "Skipped errors"), lastImportReport.failedUnsupportedFiles.toString())
            importResult?.let { result ->
                CompactRuleRow(uiText("В пропущенные", "Moved to Skipped"), result.quarantinedUnsupportedFiles.toString())
                CompactRuleRow(uiText("Ошибок пропущенных", "Skipped-file errors"), result.failedUnsupportedFiles.toString())
                ImportFailureDetails(result.failureDetails)
            }
            CompactRuleRow(uiText("Всего найдено", "Total found"), lastImportReport.plannedFiles.toString())
            CompactRuleRow(uiText("Время", "Time"), lastImportReport.createdAtEpochMillis.toReadableDateTime())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onOpenLibraryFolderClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(uiText("Библиотека", "Library"))
                }
                OutlinedButton(
                    onClick = onOpenUnsupportedFolderClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(uiText("Пропущенные", "Skipped"))
                }
                OutlinedButton(
                    onClick = onOpenDuplicatesFolderClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(uiText("Дубли", "Duplicates"))
                }
            }
        }
    }
}

@Composable
private fun ImportFailureDetails(failureDetails: List<ImportFailureDetail>) {
    if (failureDetails.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = uiText("Ошибки импорта", "Import Errors"),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        failureDetails.take(5).forEach { detail ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = detail.reason,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail.sourcePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (failureDetails.size > 5) {
            Text(
                text = uiText(
                    ru = "Еще ошибок: ${failureDetails.size - 5}",
                    en = "More errors: ${failureDetails.size - 5}",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyFolderCleanupAction(
    lastImportReport: ImportReport?,
    message: String?,
    awaitingConfirmation: Boolean,
    onRequestClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    if (lastImportReport?.importMode != ImportMode.Move) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = uiText("Пустые папки источника", "Empty Source Folders"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message ?: uiText(
                    ru = "После переноса можно удалить пустые подпапки во входящей папке. Файлы не удаляются.",
                    en = "After moving files, empty subfolders in the source folder can be removed. Files are not deleted.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (awaitingConfirmation) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(uiText("Отмена", "Cancel"))
                    }
                    Button(
                        onClick = onConfirmClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(uiText("Удалить пустые", "Remove Empty"))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onRequestClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(uiText("Очистить пустые папки", "Clean Empty Folders"))
                }
            }
        }
    }
}

@Composable
private fun ImportModeChips(
    selectedMode: ImportMode,
    onImportModeSelected: (ImportMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ImportMode.entries.forEach { mode ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onImportModeSelected(mode) },
                shape = MaterialTheme.shapes.small,
                color = if (mode == selectedMode) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            ) {
                Text(
                    text = mode.titleText(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ImportStatus(importUiState: ImportUiState) {
    val text = when (importUiState) {
        ImportUiState.Idle -> return
        is ImportUiState.AwaitingConfirmation -> return
        ImportUiState.CheckingStorageSpace -> uiText(
            ru = "Проверяю свободное место в папке библиотеки.",
            en = "Checking free space in the library folder.",
        )
        is ImportUiState.Loading -> importUiState.progress?.let { progress ->
            uiText(
                ru = "Импорт: ${progress.processedFiles} из ${progress.totalFiles}. Копий ${progress.copiedFiles}, переносов ${progress.movedFiles}, пропусков ${progress.skippedFiles}, ошибок ${progress.failedFiles}.",
                en = "Import: ${progress.processedFiles} of ${progress.totalFiles}. Copied ${progress.copiedFiles}, moved ${progress.movedFiles}, skipped ${progress.skippedFiles}, errors ${progress.failedFiles}.",
            )
        } ?: uiText("Выполняю импорт по выбранному режиму.", "Importing with the selected mode.")
        is ImportUiState.Canceled -> importUiState.progress?.let { progress ->
            uiText(
                ru = "Импорт остановлен: обработано ${progress.processedFiles} из ${progress.totalFiles}. Копий ${progress.copiedFiles}, переносов ${progress.movedFiles}, пропусков ${progress.skippedFiles}, ошибок ${progress.failedFiles}.",
                en = "Import stopped: processed ${progress.processedFiles} of ${progress.totalFiles}. Copied ${progress.copiedFiles}, moved ${progress.movedFiles}, skipped ${progress.skippedFiles}, errors ${progress.failedFiles}.",
            )
        } ?: uiText(
            ru = "Импорт остановлен. Уже обработанные файлы оставлены на месте.",
            en = "Import stopped. Already processed files were left as they are.",
        )
        is ImportUiState.Success -> {
            uiText(
                ru = "Импорт завершен: скопировано ${importUiState.result.copiedFiles}, перенесено ${importUiState.result.movedFiles}, в пропущенные ${importUiState.result.quarantinedUnsupportedFiles}, пропущено ${importUiState.result.skippedFiles}, ошибок медиа ${importUiState.result.failedFiles}, ошибок пропущенных ${importUiState.result.failedUnsupportedFiles}.",
                en = "Import complete: copied ${importUiState.result.copiedFiles}, moved ${importUiState.result.movedFiles}, moved to Skipped ${importUiState.result.quarantinedUnsupportedFiles}, skipped ${importUiState.result.skippedFiles}, media errors ${importUiState.result.failedFiles}, skipped-file errors ${importUiState.result.failedUnsupportedFiles}.",
            )
        }
        is ImportUiState.Error -> importUiState.message
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (importUiState is ImportUiState.Loading) {
            LinearProgressIndicator(
                progress = {
                    importUiState.progress?.let { progress ->
                        if (progress.totalFiles > 0) progress.processedFiles.toFloat() / progress.totalFiles else 0f
                    } ?: 0f
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImportConfirmation(
    importUiState: ImportUiState,
    selectedMode: ImportMode,
    onConfirmImportClick: () -> Unit,
    onCancelImportClick: () -> Unit,
) {
    if (importUiState !is ImportUiState.AwaitingConfirmation) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = importConfirmationText(
                    importUiState = importUiState,
                    selectedMode = selectedMode,
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (selectedMode == ImportMode.Copy && importUiState.requiredBytes != null && importUiState.availableBytes != null) {
                CompactRuleRow(uiText("Нужно места", "Required Space"), importUiState.requiredBytes.toReadableSize())
                CompactRuleRow(uiText("Свободно", "Available"), importUiState.availableBytes.toReadableSize())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancelImportClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(uiText("Отмена", "Cancel"))
                }
                Button(
                    onClick = onConfirmImportClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(uiText("Начать импорт", "Start Import"))
                }
            }
        }
    }
}

@Composable
private fun ImportRulesSummary(importRules: ImportOrganizationRules) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = uiText("Правила импорта", "Import Rules"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            CompactRuleRow(uiText("Папки", "Folders"), importRules.folderTemplate.toReadableFolderRule())
            CompactRuleRow(uiText("Имена", "Names"), importRules.fileNameTemplate.toReadableFileNameRule())
        }
    }
}

@Composable
private fun CompactRuleRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ImportAvailabilityHint(importAvailability: ImportAvailability) {
    if (importAvailability is ImportAvailability.Unavailable) {
        Text(
            text = importAvailability.reasonText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun importConfirmationText(
    importUiState: ImportUiState.AwaitingConfirmation,
    selectedMode: ImportMode,
): String {
    val action = when (selectedMode) {
        ImportMode.Copy -> uiText("Будет скопировано", "Will copy")
        ImportMode.Move -> uiText("Будет перенесено", "Will move")
        ImportMode.ScanOnly -> uiText("Будет обработано", "Will process")
    }
    val sourceNote = when (selectedMode) {
        ImportMode.Copy -> uiText("Исходники останутся на месте.", "Source files will stay in place.")
        ImportMode.Move -> {
            val unsupportedNote = if (importUiState.unsupportedFileCount > 0) {
                uiText(
                    ru = " Неподдерживаемые файлы уйдут в отдельную папку: ${importUiState.unsupportedFileCount}.",
                    en = " Unsupported files will be moved to a separate folder: ${importUiState.unsupportedFileCount}.",
                )
            } else {
                ""
            }
            uiText(
                ru = "Исходники исчезнут из старой папки после успешного переноса.$unsupportedNote",
                en = "Source files will disappear from the old folder after a successful move.$unsupportedNote",
            )
        }
        ImportMode.ScanOnly -> uiText("Файлы не изменяются.", "Files are not changed.")
    }
    return uiText(
        ru = "$action: ${importUiState.readyFileCount}. Уже есть: ${importUiState.existingFileCount}. $sourceNote",
        en = "$action: ${importUiState.readyFileCount}. Already exist: ${importUiState.existingFileCount}. $sourceNote",
    )
}

private fun scanStatusText(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
): String {
    return when (scanUiState) {
        ScanUiState.Idle -> if (plan.canScan) {
            uiText(
                ru = "Готово к безопасному сканированию. На этом шаге приложение еще не будет копировать, переносить или удалять файлы.",
                en = "Ready for a safe scan. At this step the app will not copy, move, or delete files.",
            )
        } else {
            uiText(
                ru = "Выбери исходную папку и папку библиотеки, чтобы подготовить сканирование.",
                en = "Choose the source folder and library folder to prepare scanning.",
            )
        }

        is ScanUiState.Loading -> uiText(
            ru = "Сканирую папку и подпапки. Файлы не изменяются.",
            en = "Scanning the folder and subfolders. Files are not changed.",
        )
        ScanUiState.Canceled -> uiText(
            ru = "Сканирование остановлено. Файлы не изменялись.",
            en = "Scan stopped. Files were not changed.",
        )
        is ScanUiState.Success -> uiText(
            ru = "Сканирование завершено. Это только статистика, импорт пока не запускался.",
            en = "Scan complete. This is only statistics; import has not started yet.",
        )
        is ScanUiState.Error -> scanUiState.message
    }
}

private fun Map<String, Int>.toReadableUnsupportedExtensions(): String {
    return entries
        .take(5)
        .joinToString { (extension, count) -> "$extension: $count" }
}

private fun ImportRulesPreset.titleText(): String {
    return when (this) {
        ImportRulesPreset.YearMonth -> uiText("Год / месяц", "Year / Month")
        ImportRulesPreset.YearMonthDay -> uiText("Год / месяц / день", "Year / Month / Day")
        ImportRulesPreset.YearOnly -> uiText("Только год", "Year Only")
    }
}

private fun ImportRulesPreset.descriptionText(): String {
    return when (this) {
        ImportRulesPreset.YearMonth -> uiText(
            ru = "Удобно для большого архива без слишком глубоких папок.",
            en = "Good for large archives without deep folder nesting.",
        )
        ImportRulesPreset.YearMonthDay -> uiText(
            ru = "Лучше для дней с большим количеством фото.",
            en = "Better for days with many photos.",
        )
        ImportRulesPreset.YearOnly -> uiText(
            ru = "Минимум папок, сортировка в основном по имени файла.",
            en = "Fewer folders, mostly sorted by file name.",
        )
    }
}

private fun String.toReadableFolderRule(): String {
    return when (this) {
        "YYYY/YYYY-MM" -> uiText("Библиотека / год / месяц", "Library / year / month")
        "YYYY/YYYY-MM/YYYY-MM-DD" -> uiText("Библиотека / год / месяц / день", "Library / year / month / day")
        "YYYY" -> uiText("Библиотека / год", "Library / year")
        else -> uiText("Пользовательская структура", "Custom structure")
    }
}

private fun String.toReadableFileNameRule(): String {
    return when (this) {
        "YYYY-MM-DD_HH-mm-ss_original-name.ext" -> uiText("Дата, время и исходное имя", "Date, time, and original name")
        "HH-mm-ss_original-name.ext" -> uiText("Время и исходное имя", "Time and original name")
        else -> uiText("Пользовательские имена", "Custom names")
    }
}

private fun Long.toReadableDateTime(): String {
    val dateTime = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val month = (dateTime.month.ordinal + 1).toString().padStart(2, '0')
    val day = dateTime.day.toString().padStart(2, '0')
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    val second = dateTime.second.toString().padStart(2, '0')
    return "${dateTime.year}-$month-$day $hour:$minute:$second"
}
