package com.anvar.photolibraryorganizer.data.filesystem

import com.anvar.photolibraryorganizer.domain.model.ImportTargetStatus
import com.anvar.photolibraryorganizer.domain.model.PlannedMediaFile
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmImportPlanTargetResolverTest {
    private val resolver = JvmImportPlanTargetResolver()

    @Test
    fun marksExistingTargetsBeforeImport() = runTest {
        val folder = Files.createTempDirectory("target-resolver-test")
        val existingTarget = folder.resolve("existing.jpg")
        val missingTarget = folder.resolve("missing.jpg")
        existingTarget.writeText("already imported")

        val result = resolver.resolve(
            listOf(
                plannedFile(existingTarget.toString()),
                plannedFile(missingTarget.toString()),
            ),
        )

        assertEquals(ImportTargetStatus.AlreadyExists, result[0].targetStatus)
        assertEquals(ImportTargetStatus.Ready, result[1].targetStatus)
    }

    private fun plannedFile(targetPath: String): PlannedMediaFile {
        return PlannedMediaFile(
            sourcePath = "/source/photo.jpg",
            fileName = "photo.jpg",
            targetRelativePath = targetPath,
            sizeBytes = 1024,
        )
    }
}
