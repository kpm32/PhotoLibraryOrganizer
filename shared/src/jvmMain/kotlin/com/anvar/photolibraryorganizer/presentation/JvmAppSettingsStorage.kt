package com.anvar.photolibraryorganizer.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

class JvmAppSettingsStorage(
    private val settingsPath: Path = defaultSettingsPath(),
) : AppSettingsStorage {
    override suspend fun loadSettings(): AppSettings = withContext(Dispatchers.IO) {
        if (!settingsPath.exists()) {
            return@withContext AppSettings()
        }

        val properties = Properties()
        settingsPath.inputStream().use(properties::load)

        AppSettings(
            sourceFolder = properties.getProperty(SOURCE_FOLDER_KEY)?.takeIf { it.isNotBlank() },
            destinationFolder = properties.getProperty(DESTINATION_FOLDER_KEY)?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        settingsPath.parent?.createDirectories()

        val properties = Properties().apply {
            settings.sourceFolder?.let { setProperty(SOURCE_FOLDER_KEY, it) }
            settings.destinationFolder?.let { setProperty(DESTINATION_FOLDER_KEY, it) }
        }

        settingsPath.outputStream().use { output ->
            properties.store(output, "PhotoLibraryOrganizer settings")
        }
    }

    private companion object {
        const val SOURCE_FOLDER_KEY = "sourceFolder"
        const val DESTINATION_FOLDER_KEY = "destinationFolder"

        fun defaultSettingsPath(): Path {
            return Path.of(
                System.getProperty("user.home"),
                "Library",
                "Application Support",
                "PhotoLibraryOrganizer",
                "settings.properties",
            )
        }
    }
}
