package com.anvar.photolibraryorganizer.domain.repository

import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile

/**
 * Rechecks planned target paths against the current filesystem state.
 *
 * It is used before import starts so already-existing files can be skipped
 * instead of overwritten.
 */
interface ImportPlanTargetResolver {
    suspend fun resolve(plannedFiles: List<PlannedMediaFile>): List<PlannedMediaFile>
}
