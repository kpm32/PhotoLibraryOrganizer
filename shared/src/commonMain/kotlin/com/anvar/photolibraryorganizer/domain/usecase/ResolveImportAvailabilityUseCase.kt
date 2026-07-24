package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.ImportUnavailableReason
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile

/**
 * Centralizes rules for enabling the import command.
 *
 * The use case returns typed reasons instead of UI text so presentation can
 * localize messages without leaking language concerns into domain code.
 */
class ResolveImportAvailabilityUseCase {
    operator fun invoke(
        importMode: ImportMode,
        plannedFiles: List<PlannedMediaFile>,
    ): ImportAvailability {
        return when {
            plannedFiles.isEmpty() -> ImportAvailability.Unavailable(ImportUnavailableReason.PlanMissing)
            importMode == ImportMode.ScanOnly -> ImportAvailability.Unavailable(ImportUnavailableReason.ScanOnlyMode)
            else -> ImportAvailability.Available
        }
    }
}
