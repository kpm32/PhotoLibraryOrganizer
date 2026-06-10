package com.anvar.photolibraryorganizer.domain.usecase

import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.model.ImportAvailability
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile

class ResolveImportAvailabilityUseCase {
    operator fun invoke(
        importMode: ImportMode,
        plannedFiles: List<PlannedMediaFile>,
    ): ImportAvailability {
        return when {
            plannedFiles.isEmpty() -> ImportAvailability.Unavailable("Сначала нужно просканировать папку и получить план файлов.")
            importMode == ImportMode.ScanOnly -> ImportAvailability.Unavailable("Выбран режим только сканирования. Для импорта выбери копирование или перенос.")
            else -> ImportAvailability.Available
        }
    }
}
