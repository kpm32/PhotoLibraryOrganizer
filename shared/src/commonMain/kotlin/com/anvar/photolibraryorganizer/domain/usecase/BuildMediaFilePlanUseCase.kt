package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScannedMediaFile
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class BuildMediaFilePlanUseCase {
    operator fun invoke(
        destinationFolder: String?,
        mediaFiles: List<ScannedMediaFile>,
        importRules: ImportOrganizationRules = ImportOrganizationRules.Default,
    ): List<PlannedMediaFile> {
        val destination = destinationFolder?.trim()?.trimEnd('/')
            ?: return emptyList()

        return mediaFiles.map { mediaFile ->
            val dateTime = Instant.fromEpochMilliseconds(
                mediaFile.capturedAtEpochMillis ?: mediaFile.modifiedAtEpochMillis,
            )
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val year = dateTime.year.toString()
            val month = (dateTime.month.ordinal + 1).toString().padStart(2, '0')
            val day = dateTime.day.toString().padStart(2, '0')
            val hour = dateTime.hour.toString().padStart(2, '0')
            val minute = dateTime.minute.toString().padStart(2, '0')
            val second = dateTime.second.toString().padStart(2, '0')
            val safeFileName = mediaFile.fileName.replace('/', '_')
            val tokens = mapOf(
                "YYYY" to year,
                "MM" to month,
                "DD" to day,
                "HH" to hour,
                "mm" to minute,
                "ss" to second,
                "original-name.ext" to safeFileName,
            )
            val targetFolder = "${importRules.libraryFolderName}/${importRules.folderTemplate.applyTokens(tokens)}"
            val targetFileName = importRules.fileNameTemplate.applyTokens(tokens)

            PlannedMediaFile(
                sourcePath = mediaFile.path,
                fileName = mediaFile.fileName,
                targetRelativePath = "$destination/$targetFolder/$targetFileName",
                sizeBytes = mediaFile.sizeBytes,
                contentHash = mediaFile.contentHash,
                capturedAtEpochMillis = mediaFile.capturedAtEpochMillis,
                modifiedAtEpochMillis = mediaFile.modifiedAtEpochMillis,
            )
        }
    }

    private fun String.applyTokens(tokens: Map<String, String>): String {
        return tokens.entries.fold(this) { result, token ->
            result.replace(token.key, token.value)
        }
    }
}
