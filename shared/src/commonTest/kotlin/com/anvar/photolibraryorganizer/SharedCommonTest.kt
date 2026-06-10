package com.anvar.photolibraryorganizer

import com.anvar.photolibraryorganizer.domain.ImportMode
import com.anvar.photolibraryorganizer.domain.PhotoLibraryPlan
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun scanRequiresSourceAndDestinationFolders() {
        val plan = PhotoLibraryPlan(
            sourceFolder = "/source",
            destinationFolder = null,
            importMode = ImportMode.ScanOnly,
        )

        assertFalse(plan.canScan)
    }

    @Test
    fun scanIsAvailableWhenBothFoldersAreSelected() {
        val plan = PhotoLibraryPlan(
            sourceFolder = "/source",
            destinationFolder = "/library",
            importMode = ImportMode.ScanOnly,
        )

        assertTrue(plan.canScan)
    }

    @Test
    fun scanOnlyIsDefaultSafeMode() {
        assertEquals("Scan only", ImportMode.ScanOnly.title)
    }
}
