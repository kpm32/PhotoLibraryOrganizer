package com.anvar.photolibraryorganizer.domain.model

sealed interface ImportAvailability {
    data object Available : ImportAvailability
    data class Unavailable(val reason: ImportUnavailableReason) : ImportAvailability
}

enum class ImportUnavailableReason {
    PlanMissing,
    ScanOnlyMode,
}
