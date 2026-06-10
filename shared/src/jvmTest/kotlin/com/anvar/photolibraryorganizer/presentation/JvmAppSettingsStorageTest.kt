package com.anvar.photolibraryorganizer.presentation

import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmAppSettingsStorageTest {
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
