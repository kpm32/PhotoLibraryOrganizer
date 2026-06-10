package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile

interface ImportPlanTargetResolver {
    suspend fun resolve(plannedFiles: List<PlannedMediaFile>): List<PlannedMediaFile>
}
