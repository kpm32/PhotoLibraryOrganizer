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
import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.presentation.ImportReport
import com.anvar.photolibraryorganizer.presentation.ImportRulesPreset
import com.anvar.photolibraryorganizer.presentation.ImportUiState
import com.anvar.photolibraryorganizer.presentation.ScanUiState
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
internal fun ImportPanel(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
    importUiState: ImportUiState,
    lastImportReport: ImportReport?,
    importHistory: List<ImportReport>,
    importAvailability: ImportAvailability,
    selectedMode: ImportMode,
    onScanClick: () -> Unit,
    onImportClick: () -> Unit,
    onConfirmImportClick: () -> Unit,
    onCancelImportClick: () -> Unit,
    onCancelRunningImportClick: () -> Unit,
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
                text = "Панель импорта",
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
                    text = "Если в JPEG есть EXIF-дата съемки, используем ее. Для остальных файлов берем дату изменения.",
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
            ImportReportSummary(lastImportReport)
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
                    Text(if (scanUiState is ScanUiState.Loading) "Сканирую..." else "Сканировать")
                }
                if (scanUiState is ScanUiState.Loading) {
                    OutlinedButton(
                        onClick = onCancelScanClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Остановить сканирование")
                    }
                }
                OutlinedButton(
                    onClick = onImportClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = importAvailability is ImportAvailability.Available &&
                        importUiState !is ImportUiState.AwaitingConfirmation &&
                        importUiState !is ImportUiState.Loading,
                ) {
                    Text(if (importUiState is ImportUiState.Loading) "Импортирую..." else "Импорт")
                }
                if (importUiState is ImportUiState.Loading) {
                    OutlinedButton(
                        onClick = onCancelRunningImportClick,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Остановить импорт")
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
            text = "Просмотрено: ${progress.scannedFiles}. Медиа: ${progress.mediaFiles}. Пропущено: ${progress.unsupportedFiles}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (progress.unsupportedFileExtensions.isNotEmpty()) {
            Text(
                text = "Пропущенные типы: ${progress.unsupportedFileExtensions.toReadableUnsupportedExtensions()}",
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
                text = "История импортов",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            importHistory.take(3).forEach { report ->
                CompactRuleRow(
                    label = report.createdAtEpochMillis.toReadableDateTime(),
                    value = "${report.importMode.title}: ${report.readyFiles} файлов, ошибок ${report.failedFiles}",
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
            text = "Структура папок",
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
                            text = preset.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = preset.description,
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
private fun ImportReportSummary(lastImportReport: ImportReport?) {
    if (lastImportReport == null) return

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
                text = "Последний импорт",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            CompactRuleRow("Режим", lastImportReport.importMode.title)
            CompactRuleRow(
                label = "План",
                value = "${lastImportReport.readyFiles} к импорту, ${lastImportReport.existingFiles} уже есть",
            )
            CompactRuleRow(
                label = "Итог",
                value = "копий ${lastImportReport.copiedFiles}, переносов ${lastImportReport.movedFiles}, пропусков ${lastImportReport.skippedFiles}, ошибок ${lastImportReport.failedFiles}",
            )
            CompactRuleRow("Всего найдено", lastImportReport.plannedFiles.toString())
            CompactRuleRow("Время", lastImportReport.createdAtEpochMillis.toReadableDateTime())
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
                    text = mode.title,
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
        is ImportUiState.Loading -> importUiState.progress?.let { progress ->
            "Импорт: ${progress.processedFiles} из ${progress.totalFiles}. Копий ${progress.copiedFiles}, переносов ${progress.movedFiles}, пропусков ${progress.skippedFiles}, ошибок ${progress.failedFiles}."
        } ?: "Выполняю импорт по выбранному режиму."
        is ImportUiState.Canceled -> importUiState.progress?.let { progress ->
            "Импорт остановлен: обработано ${progress.processedFiles} из ${progress.totalFiles}. Копий ${progress.copiedFiles}, переносов ${progress.movedFiles}, пропусков ${progress.skippedFiles}, ошибок ${progress.failedFiles}."
        } ?: "Импорт остановлен. Уже обработанные файлы оставлены на месте."
        is ImportUiState.Success -> {
            "Импорт завершен: скопировано ${importUiState.result.copiedFiles}, перенесено ${importUiState.result.movedFiles}, в Unsupported ${importUiState.result.quarantinedUnsupportedFiles}, пропущено ${importUiState.result.skippedFiles}, ошибок ${importUiState.result.failedFiles + importUiState.result.failedUnsupportedFiles}."
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancelImportClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Отмена")
                }
                Button(
                    onClick = onConfirmImportClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Начать импорт")
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
                text = "Правила импорта",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            CompactRuleRow("Папки", importRules.folderTemplate.toReadableFolderRule())
            CompactRuleRow("Имена", importRules.fileNameTemplate.toReadableFileNameRule())
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
            text = importAvailability.reason,
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
        ImportMode.Copy -> "Будет скопировано"
        ImportMode.Move -> "Будет перенесено"
        ImportMode.ScanOnly -> "Будет обработано"
    }
    val sourceNote = when (selectedMode) {
        ImportMode.Copy -> "Исходники останутся на месте."
        ImportMode.Move -> {
            val unsupportedNote = if (importUiState.unsupportedFileCount > 0) {
                " Неподдерживаемые файлы уйдут в Unsupported: ${importUiState.unsupportedFileCount}."
            } else {
                ""
            }
            "Исходники исчезнут из старой папки после успешного переноса.$unsupportedNote"
        }
        ImportMode.ScanOnly -> "Файлы не изменяются."
    }
    return "$action: ${importUiState.readyFileCount}. Уже есть: ${importUiState.existingFileCount}. $sourceNote"
}

private fun scanStatusText(
    plan: PhotoLibraryPlan,
    scanUiState: ScanUiState,
): String {
    return when (scanUiState) {
        ScanUiState.Idle -> if (plan.canScan) {
            "Готово к безопасному сканированию. На этом шаге приложение еще не будет копировать, переносить или удалять файлы."
        } else {
            "Выбери исходную папку и папку библиотеки, чтобы подготовить сканирование."
        }

        is ScanUiState.Loading -> "Сканирую папку и подпапки. Файлы не изменяются."
        ScanUiState.Canceled -> "Сканирование остановлено. Файлы не изменялись."
        is ScanUiState.Success -> "Сканирование завершено. Это только статистика, импорт пока не запускался."
        is ScanUiState.Error -> scanUiState.message
    }
}

private fun Map<String, Int>.toReadableUnsupportedExtensions(): String {
    return entries
        .take(5)
        .joinToString { (extension, count) -> "$extension: $count" }
}

private fun String.toReadableFolderRule(): String {
    return when (this) {
        "YYYY/YYYY-MM" -> "Библиотека / год / месяц"
        "YYYY/YYYY-MM/YYYY-MM-DD" -> "Библиотека / год / месяц / день"
        "YYYY" -> "Библиотека / год"
        else -> "Пользовательская структура"
    }
}

private fun String.toReadableFileNameRule(): String {
    return when (this) {
        "YYYY-MM-DD_HH-mm-ss_original-name.ext" -> "Дата, время и исходное имя"
        "HH-mm-ss_original-name.ext" -> "Время и исходное имя"
        else -> "Пользовательские имена"
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
