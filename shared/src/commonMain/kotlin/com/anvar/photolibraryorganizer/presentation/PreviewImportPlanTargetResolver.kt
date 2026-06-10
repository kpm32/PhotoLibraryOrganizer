package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import com.anvar.photolibraryorganizer.domain.repository.ImportPlanTargetResolver

object PreviewImportPlanTargetResolver : ImportPlanTargetResolver {
    override suspend fun resolve(plannedFiles: List<PlannedMediaFile>): List<PlannedMediaFile> {
        return plannedFiles.map { it.copy(targetStatus = ImportTargetStatus.Ready) }
    }
}
