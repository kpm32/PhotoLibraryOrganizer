package com.anvar.photolibraryorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan

@Composable
@Preview
fun App() {
    MaterialTheme {
        var sourceFolder by remember { mutableStateOf<String?>(null) }
        var destinationFolder by remember { mutableStateOf<String?>(null) }
        var importMode by remember { mutableStateOf(ImportMode.ScanOnly) }

        val plan = PhotoLibraryPlan(
            sourceFolder = sourceFolder,
            destinationFolder = destinationFolder,
            importMode = importMode,
        )

        PhotoLibraryOrganizerApp(
            plan = plan,
            onSourceFolderClick = {
                sourceFolder = "/Users/anvardzan/Pictures/Unsorted archive"
            },
            onDestinationFolderClick = {
                destinationFolder = "/Users/anvardzan/Pictures/PhotoLibrary"
            },
            onImportModeSelected = { importMode = it },
        )
    }
}

@Composable
private fun PhotoLibraryOrganizerApp(
    plan: PhotoLibraryPlan,
    onSourceFolderClick: () -> Unit,
    onDestinationFolderClick: () -> Unit,
    onImportModeSelected: (ImportMode) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .safeContentPadding()
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FolderSelector(
                    title = "Исходная папка",
                    description = "Большая папка с подпапками, которую надо разобрать.",
                    path = plan.sourceFolder,
                    actionText = "Выбрать источник",
                    onClick = onSourceFolderClick,
                    modifier = Modifier.weight(1f),
                )
                FolderSelector(
                    title = "Папка библиотеки",
                    description = "Новая или существующая папка, куда ляжет порядок.",
                    path = plan.destinationFolder,
                    actionText = "Выбрать назначение",
                    onClick = onDestinationFolderClick,
                    modifier = Modifier.weight(1f),
                )
            }

            ImportModeSelector(
                selectedMode = plan.importMode,
                onImportModeSelected = onImportModeSelected,
            )

            ScanPreview(plan = plan)
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Photo Library Organizer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Сначала сканируем архив без изменений, потом копируем или переносим только после подтверждения.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FolderSelector(
    title: String,
    description: String,
    path: String?,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.defaultMinSize(minHeight = 172.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = path ?: "Папка пока не выбрана",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (path == null) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                )
            }
            OutlinedButton(onClick = onClick) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun ImportModeSelector(
    selectedMode: ImportMode,
    onImportModeSelected: (ImportMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Режим обработки",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ImportMode.entries.forEach { mode ->
                ModeOption(
                    mode = mode,
                    selected = mode == selectedMode,
                    onClick = { onImportModeSelected(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ModeOption(
    mode: ImportMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        tonalElevation = if (selected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = mode.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScanPreview(plan: PhotoLibraryPlan) {
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
                text = "Предпросмотр сканирования",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider()
            Text(
                text = if (plan.canScan) {
                    "Готово к безопасному сканированию. На этом шаге приложение еще не будет копировать, переносить или удалять файлы."
                } else {
                    "Выбери исходную папку и папку библиотеки, чтобы подготовить сканирование."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {},
                    enabled = plan.canScan,
                ) {
                    Text("Scan")
                }
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                ) {
                    Text("Import")
                }
            }
        }
    }
}
