package com.anvar.photolibraryorganizer.presentation

import com.anvar.photolibraryorganizer.domain.model.ImportOrganizationRules
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.util.Properties
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmAppSettingsStorageTest {
    @Test
    fun savesAndLoadsSelectedFoldersAndImportRules() = runTest {
        val settingsPath = Files.createTempDirectory("photo-settings-test")
            .resolve("settings.properties")
        val storage = JvmAppSettingsStorage(settingsPath)

        storage.saveSettings(
            AppSettings(
                sourceFolder = "/source",
                destinationFolder = "/library",
                importRules = ImportOrganizationRules(
                    libraryFolderName = "Library",
                    folderTemplate = "YYYY/YYYY-MM/YYYY-MM-DD",
                    fileNameTemplate = "HH-mm-ss_original-name.ext",
                ),
            ),
        )

        val result = storage.loadSettings()

        assertEquals("/source", result.sourceFolder)
        assertEquals("/library", result.destinationFolder)
        assertEquals("Library", result.importRules.libraryFolderName)
        assertEquals("YYYY/YYYY-MM/YYYY-MM-DD", result.importRules.folderTemplate)
        assertEquals("HH-mm-ss_original-name.ext", result.importRules.fileNameTemplate)
    }

    @Test
    fun usesDefaultImportRulesForOldSettingsFile() = runTest {
        val settingsPath = Files.createTempDirectory("photo-legacy-settings-test")
            .resolve("settings.properties")
        Properties().apply {
            setProperty("sourceFolder", "/source")
            setProperty("destinationFolder", "/library")
        }.let { properties ->
            settingsPath.outputStream().use { output ->
                properties.store(output, "legacy settings")
            }
        }
        val storage = JvmAppSettingsStorage(settingsPath)

        val result = storage.loadSettings()

        assertEquals("/source", result.sourceFolder)
        assertEquals("/library", result.destinationFolder)
        assertEquals(ImportOrganizationRules.Default, result.importRules)
    }

    @Test
    fun savesAndLoadsSelectedFolders() = runTest {
        val settingsPath = Files.createTempDirectory("photo-settings-test")
            .resolve("settings.properties")
        val storage = JvmAppSettingsStorage(settingsPath)

        storage.saveSettings(
            AppSettings(
                sourceFolder = "/source",
                destinationFolder = "/library",
            ),
        )

        val result = storage.loadSettings()

        assertEquals("/source", result.sourceFolder)
        assertEquals("/library", result.destinationFolder)
    }

    @Test
    fun returnsEmptySettingsWhenFileDoesNotExist() = runTest {
        val settingsPath = Files.createTempDirectory("photo-empty-settings-test")
            .resolve("missing.properties")
        val storage = JvmAppSettingsStorage(settingsPath)

        val result = storage.loadSettings()

        assertEquals(AppSettings(), result)
    }
}
