package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.ImportPlanTargetResolver
import java.nio.file.Path
import kotlin.io.path.exists

class JvmImportPlanTargetResolver : ImportPlanTargetResolver {
    override suspend fun resolve(plannedFiles: List<PlannedMediaFile>): List<PlannedMediaFile> {
        return plannedFiles.map { plannedFile ->
            val targetStatus = if (Path.of(plannedFile.targetRelativePath).exists()) {
                ImportTargetStatus.AlreadyExists
            } else {
                ImportTargetStatus.Ready
            }
            plannedFile.copy(targetStatus = targetStatus)
        }
    }
}
