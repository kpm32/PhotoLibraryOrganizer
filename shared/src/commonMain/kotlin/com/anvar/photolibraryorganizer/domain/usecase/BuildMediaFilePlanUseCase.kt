package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.model.ScannedMediaFile
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class BuildMediaFilePlanUseCase {
    operator fun invoke(
        destinationFolder: String?,
        mediaFiles: List<ScannedMediaFile>,
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
            val targetFileName = "${year}-${month}-${day}_${hour}-${minute}-${second}_$safeFileName"

            PlannedMediaFile(
                sourcePath = mediaFile.path,
                fileName = mediaFile.fileName,
                targetRelativePath = "$destination/Library/$year/$year-$month/$targetFileName",
                sizeBytes = mediaFile.sizeBytes,
                contentHash = mediaFile.contentHash,
            )
        }
    }
}
